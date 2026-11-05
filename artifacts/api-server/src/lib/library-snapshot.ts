import { existsSync, readFileSync } from "node:fs";
import { mkdtemp, rm } from "node:fs/promises";
import { dirname, join, resolve } from "node:path";
import { tmpdir } from "node:os";
import { pathToFileURL } from "node:url";
import { DatabaseSync } from "node:sqlite";

export const snapshotColumns = {
  genres: ["genre_id", "genre_name"],
  users: ["user_id", "username", "email"],
  games: ["game_id", "genre_id", "title", "launch_url", "release_date"],
  user_games: ["user_id", "game_id", "rating", "play_count"],
} as const;
export type Snapshot = Record<keyof typeof snapshotColumns, Record<string, unknown>[]>;

export function findJavaProject(start = process.cwd()): string {
  let dir = start;
  while (true) {
    const project = join(dir, "java-database-starter");
    if (existsSync(join(project, "src/main/resources/schema.sql"))) return project;
    const parent = dirname(dir);
    if (parent === dir) throw new Error("The approved Java/SQLite schema is missing.");
    dir = parent;
  }
}

export async function writeSqliteSnapshot(rows: Snapshot) {
  const directory = await mkdtemp(join(tmpdir(), "browser-library-backup-"));
  const path = join(directory, "library.db");
  const cleanup = () => rm(directory, { recursive: true, force: true });
  let db: DatabaseSync | undefined;
  try {
    db = new DatabaseSync(path);
    db.exec(readFileSync(join(findJavaProject(), "src/main/resources/schema.sql"), "utf8"));
    db.exec("BEGIN");
    for (const [table, columns] of Object.entries(snapshotColumns)) {
      const statement = db.prepare(`INSERT INTO ${table} (${columns.join(",")}) VALUES (${columns.map(() => "?").join(",")})`);
      for (const row of rows[table as keyof Snapshot]) {
        statement.run(...columns.map((column) => row[column] as string | number | null));
      }
    }
    db.exec("COMMIT");
    db.close();
    return { path, cleanup };
  } catch (error) {
    db?.close();
    await cleanup();
    throw error;
  }
}

/** Call only after the offline recovery validator approves a standalone backup. */
export function readSqliteSnapshot(path: string): Snapshot {
  if (["-wal", "-shm", "-journal"].some((suffix) => existsSync(path + suffix))) {
    throw new Error("Import requires a standalone backup without SQLite sidecars.");
  }
  const uri = pathToFileURL(resolve(path));
  uri.searchParams.set("immutable", "1");
  const db = new DatabaseSync(uri.href, { readOnly: true });
  try {
    db.exec("PRAGMA trusted_schema = OFF");
    return Object.fromEntries(Object.entries(snapshotColumns).map(([table, columns]) => [
      table, db.prepare(`SELECT ${columns.join(",")} FROM ${table}`).all(),
    ])) as Snapshot;
  } finally { db.close(); }
}
