// Offline administrator tool. Never import this into a browser or HTTP route.
import {
  chmodSync, closeSync, copyFileSync, existsSync, fsyncSync, lstatSync,
  mkdirSync, mkdtempSync, openSync, readFileSync, readdirSync,
  realpathSync, renameSync, rmSync, statSync, writeFileSync,
} from "node:fs";
import { constants } from "node:fs";
import { dirname, join, resolve } from "node:path";
import { fileURLToPath, pathToFileURL } from "node:url";
import { DatabaseSync, backup } from "node:sqlite";
import { createHash } from "node:crypto";

const root = dirname(dirname(fileURLToPath(import.meta.url)));
export const defaultDatabase = join(root, "game-library.db");
const tables = ["games", "genres", "user_games", "users"];
const suffixes = ["", "-wal", "-shm", "-journal"];
const schema = readFileSync(join(root, "src/main/resources/schema.sql"), "utf8");
const normalize = (sql) => sql.replace(/IF NOT EXISTS/gi, "").replace(/\s+/g, "").toLowerCase();

function regularFile(path) {
  if (!lstatSync(path).isFile()) throw new Error("Use a regular database file, not a directory or symbolic link.");
}

/** Strictly matches the approved schema, including constraints and FK actions.
 * Formatting differences are accepted; unfamiliar schemas require manual review.
 */
function standaloneReader(path) {
  if (suffixes.slice(1).some((s) => existsSync(path + s))) {
    throw new Error("Use a standalone downloaded backup, not a database with journal/WAL sidecars.");
  }
  // SQLite can create WAL/SHM files even on a readOnly connection. Browser
  // downloads are self-contained snapshots: immutable mode avoids touching them.
  const uri = pathToFileURL(resolve(path));
  uri.searchParams.set("immutable", "1");
  return new DatabaseSync(uri.href, { readOnly: true });
}

export function validate(path, { standalone = true } = {}) {
  regularFile(path);
  const db = standalone ? standaloneReader(path) : new DatabaseSync(path, { readOnly: true });
  const expected = new DatabaseSync(":memory:");
  try {
    db.exec("PRAGMA trusted_schema = OFF; PRAGMA foreign_keys = ON;");
    expected.exec(schema);
    const integrity = db.prepare("PRAGMA integrity_check").all();
    if (integrity.length !== 1 || Object.values(integrity[0])[0] !== "ok") {
      throw new Error("SQLite integrity check failed.");
    }
    const objects = db.prepare("SELECT type, name, sql FROM sqlite_schema WHERE name NOT LIKE 'sqlite_%'").all();
    const actualTables = objects.filter((row) => row.type === "table");
    if (JSON.stringify(actualTables.map((row) => row.name).sort()) !== JSON.stringify(tables) ||
        objects.some((row) => !["table", "index"].includes(row.type))) {
      throw new Error("Expected only the approved four tables, without views or triggers.");
    }
    for (const row of actualTables) {
      const reference = expected.prepare("SELECT sql FROM sqlite_schema WHERE name = ?").get(row.name);
      if (normalize(row.sql) !== normalize(reference.sql)) {
        throw new Error(`Unrecognized ${row.name} schema or constraints. No automatic conversion is allowed.`);
      }
    }
    // Reject extra/changed indexes too (including unique/expression indexes).
    const indexShape = (connection) => connection.prepare(
      "SELECT name, sql FROM sqlite_schema WHERE type = 'index' AND sql IS NOT NULL ORDER BY name"
    ).all().map((row) => [row.name, normalize(row.sql)]);
    if (JSON.stringify(indexShape(db)) !== JSON.stringify(indexShape(expected))) {
      throw new Error("Unrecognized library indexes.");
    }
    if (db.prepare("PRAGMA foreign_key_check").all().length) {
      throw new Error("Backup contains broken foreign-key links.");
    }
    const invalid = db.prepare(`SELECT COUNT(*) AS count FROM user_games WHERE
      (rating IS NOT NULL AND (typeof(rating) != 'integer' OR rating NOT BETWEEN 1 AND 5))
      OR typeof(play_count) != 'integer' OR play_count < 0 OR play_count > 9007199254740991`).get();
    if (invalid.count !== 0) throw new Error("Ratings must be null or integers 1–5; play counts must be nonnegative safe integers.");
    return Object.fromEntries(tables.map((table) => [
      table, db.prepare(`SELECT COUNT(*) AS count FROM ${table}`).get().count,
    ]));
  } finally {
    expected.close();
    db.close();
  }
}

function syncFile(path) {
  const fd = openSync(path, "r");
  try { fsyncSync(fd); } finally { closeSync(fd); }
}

// This project runs on Linux. Refuse recovery if same-account processes cannot
// be inspected. Closing browser tabs alone never stops the API's SQLite handle.
export function assertNoOpenDatabase(database) {
  if (process.platform !== "linux" || !existsSync("/proc/self/fd")) {
    throw new Error("Offline recovery requires Linux /proc process inspection.");
  }
  const identities = new Set(suffixes.filter((s) => existsSync(database + s))
    .map((s) => { const stat = statSync(database + s); return `${stat.dev}:${stat.ino}`; }));
  for (const pid of readdirSync("/proc").filter((name) => /^\d+$/.test(name))) {
    try {
      if (statSync(`/proc/${pid}`).uid !== process.getuid()) continue;
      for (const fd of readdirSync(`/proc/${pid}/fd`)) {
        try {
          const stat = statSync(`/proc/${pid}/fd/${fd}`);
          if (identities.has(`${stat.dev}:${stat.ino}`)) {
            throw new Error(`Database is open in process ${pid}. Stop Java, the API, and SQLite tools first.`);
          }
        } catch (error) { if (error.code !== "ENOENT") throw error; }
      }
    } catch (error) {
      if (error.code !== "ENOENT" && error.code !== "ESRCH") throw error;
    }
  }
}

function ownedDatabase(database) {
  regularFile(database);
  if (statSync(database).uid !== process.getuid()) throw new Error("Run as the database's operating-system owner.");
}

export async function restore(source, database, { confirmation, writersStopped } = {}) {
  if (confirmation !== "REPLACE-LIBRARY" || !writersStopped) {
    throw new Error("Recovery requires --writers-stopped and --confirm REPLACE-LIBRARY. Read RECOVERY.md first.");
  }
  database = resolve(database);
  source = resolve(source);
  ownedDatabase(database);
  regularFile(source);
  const a = statSync(database), b = statSync(source);
  if (a.dev === b.dev && a.ino === b.ino) throw new Error("The backup must not be the active database.");
  if (suffixes.slice(1).some((s) => existsSync(source + s))) {
    throw new Error("Use a standalone downloaded backup, not a database with journal/WAL sidecars.");
  }
  // Validate before taking the maintenance gate; bad input never blocks service.
  validate(source);
  const lock = database + ".recovery-lock";
  mkdirSync(lock, { mode: 0o700 }); // Exclusive: never override a previous recovery.
  let staging;
  let preservation;
  try {
    assertNoOpenDatabase(database);
    // Normalize into a standalone SQLite snapshot in the active filesystem.
    staging = mkdtempSync(join(dirname(database), ".library-stage-"));
    chmodSync(staging, 0o700);
    const staged = join(staging, "library.db");
    const input = standaloneReader(source);
    try { await backup(input, staged); } finally { input.close(); }
    chmodSync(staged, 0o600);
    const stagedDb = new DatabaseSync(staged);
    try { stagedDb.exec("PRAGMA journal_mode = DELETE;"); } finally { stagedDb.close(); }
    const counts = validate(staged);
    syncFile(staged);
    assertNoOpenDatabase(database);

    const archiveRoot = join(dirname(database), "library-recovery");
    mkdirSync(archiveRoot, { recursive: true, mode: 0o700 });
    chmodSync(archiveRoot, 0o700);
    preservation = mkdtempSync(join(archiveRoot, "before-"));
    chmodSync(preservation, 0o700);
    const hashes = {};
    // Raw copies preserve even a damaged current DB and any committed WAL.
    for (const suffix of suffixes) {
      if (!existsSync(database + suffix)) continue;
      const saved = join(preservation, "library.db" + suffix);
      copyFileSync(database + suffix, saved, constants.COPYFILE_EXCL);
      chmodSync(saved, 0o600);
      syncFile(saved);
      hashes["library.db" + suffix] = createHash("sha256").update(readFileSync(saved)).digest("hex");
    }
    writeFileSync(join(preservation, "manifest.json"), JSON.stringify({
      createdAt: new Date().toISOString(), hashes,
    }, null, 2), { mode: 0o600, flag: "wx" });
    syncFile(join(preservation, "manifest.json"));
    syncFile(preservation);
    syncFile(archiveRoot);
    writeFileSync(join(lock, "preserved-path.txt"), preservation + "\n", { mode: 0o600 });
    syncFile(join(lock, "preserved-path.txt"));
    syncFile(lock);
    syncFile(dirname(database));
    assertNoOpenDatabase(database);
    // All service starts are still gated. Never attach the old WAL to new data.
    for (const suffix of suffixes.slice(1)) rmSync(database + suffix, { force: true });
    renameSync(staged, database); // Atomic install on the same filesystem.
    syncFile(dirname(database));
    validate(database);
    return { counts, preservation, maintenance: lock };
  } catch (error) {
    // Leave the gate in place on every failure; the administrator inspects and
    // explicitly resumes. In particular, do not reopen services after a crash.
    throw new Error(`${error.message}\nRecovery stopped; maintenance remains at ${lock}.` +
      (preservation ? `\nPreserved files: ${preservation}` : ""), { cause: error });
  } finally {
    if (staging) rmSync(staging, { recursive: true, force: true });
  }
}

export function resume(database) {
  database = resolve(database);
  ownedDatabase(database);
  const lock = database + ".recovery-lock";
  if (!existsSync(lock)) throw new Error("No recovery maintenance gate exists.");
  assertNoOpenDatabase(database);
  const counts = validate(database, { standalone: false });
  rmSync(lock, { recursive: true });
  syncFile(dirname(database));
  return counts;
}

async function main(args) {
  const [command, ...rest] = args;
  const options = {};
  const positional = [];
  for (let i = 0; i < rest.length; i++) {
    if (rest[i] === "--writers-stopped") options.writersStopped = true;
    else if (rest[i] === "--confirm" && rest[i + 1]) options.confirmation = rest[++i];
    else if (rest[i] === "--database" && rest[i + 1]) options.database = resolve(rest[++i]);
    else if (rest[i].startsWith("--")) throw new Error(`Unknown option: ${rest[i]}`);
    else positional.push(rest[i]);
  }
  const database = options.database ?? defaultDatabase;
  if (command === "validate" && positional.length === 1) {
    console.log("Valid backup. Row counts:", validate(resolve(positional[0])));
  } else if (command === "restore" && positional.length === 1) {
    console.log("Recovery installed. Services remain stopped:", await restore(positional[0], database, options));
    console.log("Inspect the result, then run resume explicitly. See RECOVERY.md for rollback.");
  } else if (command === "resume" && positional.length === 0) {
    if (options.confirmation !== "RESUME-LIBRARY") throw new Error("Use --confirm RESUME-LIBRARY after reviewing recovery.");
    console.log("Validated; maintenance gate removed. Restart services manually. Row counts:", resume(database));
  } else {
    throw new Error("Usage: node library-recovery.mjs validate BACKUP | restore BACKUP --writers-stopped --confirm REPLACE-LIBRARY | resume --confirm RESUME-LIBRARY [--database PATH]");
  }
}

if (process.argv[1] && realpathSync(process.argv[1]) === fileURLToPath(import.meta.url)) {
  main(process.argv.slice(2)).catch((error) => { console.error(error.message); process.exitCode = 1; });
}
