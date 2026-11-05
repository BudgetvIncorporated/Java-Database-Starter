import type { SQLInputValue } from "node:sqlite";
import { snapshotColumns, writeSqliteSnapshot, type Snapshot } from "./library-snapshot";

type Result = {
  rows: Record<string, unknown>[];
  rowCount: number | null;
  fields?: { name: string; dataTypeID: number }[];
};
export interface LibraryClient {
  query(sql: string, values?: unknown[]): Promise<Result>;
  release(): void;
}
export interface LibraryPool {
  query(sql: string, values?: unknown[]): Promise<Result>;
  connect(): Promise<LibraryClient>;
}

// Translate only the small, controlled SQL dialect used by the library routes.
// Parameters remain bound; no values, user text, or identifiers enter SQL text.
export function postgresSql(source: string): string {
  let parameter = 0;
  return source
    .replace(/\bAS ([a-z]\w*[A-Z]\w*)/g, 'AS "$1"')
    .replace(/\bORDER BY totalPlayCount\b/g, 'ORDER BY "totalPlayCount"')
    .replace(/([\w.]+) COLLATE NOCASE/g, "lower($1)")
    .replace("instr(lower(g.title), lower(?))", "strpos(lower(g.title), lower(?))")
    .replace(/\?/g, () => `$${++parameter}`)
    .replace(/(\$\d+) IS NULL/g, "$1::text IS NULL");
}

function normalize(result: Result) {
  return result.rows.map((row) => {
    const value = { ...row };
    for (const field of result.fields ?? []) {
      if ([20, 1700].includes(field.dataTypeID) && value[field.name] != null) {
        const number = Number(value[field.name]);
        if (!Number.isFinite(number) || (field.dataTypeID === 20 && !Number.isSafeInteger(number))) {
          throw new Error("Library aggregate exceeds safe numeric limits.");
        }
        value[field.name] = number;
      }
    }
    return value;
  });
}

export class PostgresLibraryStore {
  readonly db;
  constructor(readonly pool: LibraryPool) {
    this.db = {
      prepare: (source: string) => ({
        all: async (...values: SQLInputValue[]) => normalize(await pool.query(postgresSql(source), values)),
        get: async (...values: SQLInputValue[]) => normalize(await pool.query(postgresSql(source), values))[0],
        run: async (...values: SQLInputValue[]) => {
          let sql = postgresSql(source);
          const insert = source.match(/^INSERT INTO (games|users)\b/i);
          if (insert) sql += ` RETURNING ${insert[1] === "games" ? "game_id" : "user_id"} AS id`;
          const result = await pool.query(sql, values);
          return { changes: result.rowCount ?? 0, lastInsertRowid: Number(result.rows[0]?.id ?? 0) };
        },
      }),
    };
  }

  /** Schema is managed by development migrations and Publish, never at startup. */
  async assertReady() {
    for (const [table, columns] of Object.entries(snapshotColumns)) {
      await this.pool.query(`SELECT ${columns.join(",")} FROM ${table} LIMIT 0`);
    }
  }

  async createBackup() {
    const client = await this.pool.connect();
    const rows = {} as Snapshot;
    try {
      await client.query("BEGIN ISOLATION LEVEL REPEATABLE READ READ ONLY");
      for (const [table, columns] of Object.entries(snapshotColumns)) {
        rows[table as keyof Snapshot] = (await client.query(`SELECT ${columns.join(",")} FROM ${table}`)).rows;
      }
      await client.query("COMMIT");
    } catch (error) {
      await client.query("ROLLBACK");
      throw error;
    } finally { client.release(); }
    return writeSqliteSnapshot(rows);
  }

  /** Offline-only data transfer; this method does not create or alter schema. */
  async importSnapshot(rows: Snapshot, replace = false, preserve?: (before: Snapshot) => Promise<void>) {
    const client = await this.pool.connect();
    try {
      await client.query("BEGIN");
      await client.query("LOCK TABLE genres, users, games, user_games IN ACCESS EXCLUSIVE MODE");
      const existing = await client.query("SELECT (SELECT COUNT(*) FROM genres) + (SELECT COUNT(*) FROM users) + (SELECT COUNT(*) FROM games) + (SELECT COUNT(*) FROM user_games) AS count");
      if (Number(existing.rows[0].count) && !replace) {
        throw new Error("Browser database is not empty. Refusing to replace library records without explicit confirmation.");
      }
      if (replace) {
        // Preserve the exact state protected by the replacement lock, not an
        // earlier snapshot that could miss a write committed just before locking.
        if (preserve) {
          const before = {} as Snapshot;
          for (const [table, columns] of Object.entries(snapshotColumns)) {
            before[table as keyof Snapshot] = (await client.query(`SELECT ${columns.join(",")} FROM ${table}`)).rows;
          }
          await preserve(before);
        }
        for (const table of ["user_games", "games", "users", "genres"]) await client.query(`DELETE FROM ${table}`);
      }
      for (const [table, columns] of Object.entries(snapshotColumns)) {
        const sql = `INSERT INTO ${table} (${columns.join(",")}) VALUES (${columns.map((_, i) => `$${i + 1}`).join(",")})`;
        for (const row of rows[table as keyof Snapshot]) {
          await client.query(sql, columns.map((column) => row[column]));
        }
      }
      for (const [table, column] of [["genres", "genre_id"], ["users", "user_id"], ["games", "game_id"]]) {
        await client.query(`SELECT setval(pg_get_serial_sequence('${table}', '${column}'), COALESCE(MAX(${column}), 1), COUNT(*) > 0) FROM ${table}`);
      }
      await client.query("COMMIT");
    } catch (error) {
      await client.query("ROLLBACK");
      throw error;
    } finally { client.release(); }
  }
}
