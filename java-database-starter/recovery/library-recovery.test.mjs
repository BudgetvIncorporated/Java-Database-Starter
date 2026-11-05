import test from "node:test";
import assert from "node:assert/strict";
import {
  copyFileSync, existsSync, mkdirSync, mkdtempSync, readFileSync,
  readdirSync, rmSync, statSync, writeFileSync,
} from "node:fs";
import { tmpdir } from "node:os";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import { DatabaseSync, backup } from "node:sqlite";
import { spawn, spawnSync } from "node:child_process";
import { once } from "node:events";
import { createHash } from "node:crypto";
import { assertNoOpenDatabase, restore, resume, validate } from "./library-recovery.mjs";

const schema = readFileSync(join(dirname(fileURLToPath(import.meta.url)), "../src/main/resources/schema.sql"), "utf8");
const confirmed = { confirmation: "REPLACE-LIBRARY", writersStopped: true };
function fixture(t) {
  const directory = mkdtempSync(join(tmpdir(), "library-recovery-test-"));
  t.after(() => rmSync(directory, { recursive: true, force: true }));
  return directory;
}
function create(path, count = 1) {
  const db = new DatabaseSync(path);
  db.exec(schema);
  for (let i = 1; i <= count; i++) db.prepare("INSERT INTO genres VALUES (?, ?)").run(i, `Genre ${i}`);
  db.exec(`INSERT INTO users VALUES (1, 'Fixture', 'fixture@example.test');
    INSERT INTO games VALUES (1, 1, 'Fixture', 'https://example.test', '2024-01-01');
    INSERT INTO user_games VALUES (1, 1, NULL, 0);`);
  db.close();
}
function mutate(path, sql) {
  const db = new DatabaseSync(path);
  try { db.exec(sql); } finally { db.close(); }
}
async function openInChild(t, path, sql = "") {
  const child = spawn(process.execPath, ["--input-type=module", "-e", `
    import { DatabaseSync } from "node:sqlite";
    const db = new DatabaseSync(process.argv[1]);
    if (process.argv[2]) db.exec(process.argv[2]);
    process.send("ready");
    setInterval(() => {}, 1000);
  `, path, sql], { stdio: ["ignore", "ignore", "pipe", "ipc"] });
  t.after(() => { if (child.exitCode === null) child.kill("SIGKILL"); });
  await once(child, "message");
  return child;
}

test("accepts a standalone approved library without writing the download", (t) => {
  const path = join(fixture(t), "backup.db");
  create(path);
  const bytes = readFileSync(path);
  assert.deepEqual(validate(path), { games: 1, genres: 1, user_games: 1, users: 1 });
  assert.deepEqual(readFileSync(path), bytes);
  assert(!existsSync(path + "-wal"));
});

test("a browser-style WAL-mode download validates and restores without creating input sidecars", async (t) => {
  const directory = fixture(t), live = join(directory, "live.db"), input = join(directory, "download.db");
  const active = join(directory, "active.db");
  create(live, 2);
  create(active);
  const source = new DatabaseSync(live);
  try {
    source.exec("PRAGMA journal_mode = WAL; INSERT INTO genres VALUES (3, 'Committed fixture')");
    await backup(source, input);
  } finally { source.close(); }
  const bytes = readFileSync(input);
  assert.equal(validate(input).genres, 3);
  assert(!existsSync(input + "-wal"));
  assert(!existsSync(input + "-shm"));
  const result = await restore(input, active, confirmed);
  assert.equal(result.counts.genres, 3);
  assert(!existsSync(input + "-wal"));
  assert(!existsSync(input + "-shm"));
  assert.deepEqual(readFileSync(input), bytes);
  resume(active);
});

for (const [name, sql, pattern] of [
  ["missing table", "DROP TABLE user_games", /four tables/],
  ["extra table", "CREATE TABLE extra (id INTEGER)", /four tables/],
  ["trigger", "CREATE TRIGGER surprise AFTER INSERT ON genres BEGIN DELETE FROM users; END", /views or triggers/],
  ["changed schema", "ALTER TABLE games ADD COLUMN extra TEXT", /schema/],
  ["missing index", "DROP INDEX idx_games_genre_id", /indexes/],
  ["broken foreign key", "PRAGMA foreign_keys = OFF; UPDATE user_games SET user_id = 999", /foreign-key/],
  ["fractional rating", "UPDATE user_games SET rating = 1.5", /Ratings/],
  ["fractional plays", "UPDATE user_games SET play_count = 1.5", /play counts/],
  ["unsafe play count", "UPDATE user_games SET play_count = 9007199254740992", /play counts/],
  ["invalid rating", "PRAGMA ignore_check_constraints = ON; UPDATE user_games SET rating = 6", /integrity|Ratings/],
  ["negative plays", "PRAGMA ignore_check_constraints = ON; UPDATE user_games SET play_count = -1", /integrity|play counts/],
]) {
  test(`rejects ${name}`, (t) => {
    const path = join(fixture(t), "backup.db");
    create(path);
    mutate(path, sql);
    assert.throws(() => validate(path), pattern);
  });
}

test("rejects non-SQLite input and missing paths without creating files", (t) => {
  const directory = fixture(t);
  const path = join(directory, "invalid.db");
  writeFileSync(path, "not SQLite");
  assert.throws(() => validate(path));
  const missing = join(directory, "missing.db");
  assert.throws(() => validate(missing));
  assert(!existsSync(missing));
});

test("requires deliberate confirmation and leaves active bytes untouched on invalid input", async (t) => {
  const directory = fixture(t), active = join(directory, "active.db"), input = join(directory, "input.db");
  create(active);
  create(input, 2);
  const original = readFileSync(active);
  await assert.rejects(restore(input, active), /requires/);
  await assert.rejects(restore(input, active, { confirmation: "REPLACE-LIBRARY" }), /requires/);
  mutate(input, "DROP TABLE user_games");
  await assert.rejects(restore(input, active, confirmed), /four tables/);
  assert.deepEqual(readFileSync(active), original);
  assert(!existsSync(active + ".recovery-lock"));
});

test("refuses an open SQLite process and keeps maintenance gated", async (t) => {
  const directory = fixture(t), active = join(directory, "active.db"), input = join(directory, "input.db");
  create(active);
  create(input, 2);
  const child = await openInChild(t, active);
  const original = readFileSync(active);
  await assert.rejects(restore(input, active, confirmed), /Database is open/);
  assert(existsSync(active + ".recovery-lock"));
  assert.deepEqual(readFileSync(active), original);
  const exited = once(child, "exit");
  child.kill("SIGKILL");
  await exited;
  assert.equal(resume(active).genres, 1);
});

test("preserves the old database, installs explicitly, and supports explicit rollback and resume", async (t) => {
  const directory = fixture(t), active = join(directory, "active.db"), input = join(directory, "input.db");
  create(active);
  create(input, 2);
  const original = readFileSync(active);
  const result = await restore(input, active, confirmed);
  assert.equal(result.counts.genres, 2);
  assert.deepEqual(readFileSync(join(result.preservation, "library.db")), original);
  assert.equal(statSync(result.preservation).mode & 0o777, 0o700);
  assert.equal(statSync(join(result.preservation, "library.db")).mode & 0o777, 0o600);
  const manifest = JSON.parse(readFileSync(join(result.preservation, "manifest.json")));
  assert.equal(manifest.hashes["library.db"], createHash("sha256").update(original).digest("hex"));
  assert(existsSync(result.maintenance));
  await assert.rejects(restore(input, active, confirmed), /EEXIST/);
  assert.equal(resume(active).genres, 2);
  assert(!existsSync(result.maintenance));
  const rollback = await restore(join(result.preservation, "library.db"), active, confirmed);
  assert.equal(rollback.counts.genres, 1);
  assert.equal(validate(join(rollback.preservation, "library.db")).genres, 2);
  resume(active);
});

test("preserves committed WAL data and removes old sidecars before installation", async (t) => {
  const directory = fixture(t), active = join(directory, "active.db"), input = join(directory, "input.db");
  create(active);
  create(input, 2);
  const child = await openInChild(t, active,
    "PRAGMA journal_mode = WAL; INSERT INTO genres VALUES (3, 'WAL fixture');");
  const exited = once(child, "exit");
  child.kill("SIGKILL");
  await exited;
  assert(existsSync(active + "-wal"));
  const result = await restore(input, active, confirmed);
  assert.equal(result.counts.genres, 2);
  assert(!existsSync(active + "-wal"));
  assert(existsSync(join(result.preservation, "library.db-wal")));
  // Reconstruct only a copy; don't let SQLite change the archived set.
  const replay = join(directory, "replay");
  mkdirSync(replay);
  for (const name of readdirSync(result.preservation).filter((name) => name.startsWith("library.db"))) {
    copyFileSync(join(result.preservation, name), join(replay, name));
  }
  mutate(join(replay, "library.db"), "PRAGMA wal_checkpoint(TRUNCATE); PRAGMA journal_mode = DELETE;");
  assert.equal(validate(join(replay, "library.db")).genres, 2); // Original genre plus committed WAL row.
  resume(active);
});

test("invalid active data cannot resume and a damaged original is still preserved", async (t) => {
  const directory = fixture(t), active = join(directory, "active.db"), input = join(directory, "input.db");
  writeFileSync(active, "damaged original");
  create(input);
  mkdirSync(active + ".recovery-lock");
  assert.throws(() => resume(active));
  assert(existsSync(active + ".recovery-lock"));
  rmSync(active + ".recovery-lock", { recursive: true });
  const result = await restore(input, active, confirmed);
  assert.equal(readFileSync(join(result.preservation, "library.db"), "utf8"), "damaged original");
  resume(active);
});

test("CLI validates read-only and refuses unconfirmed restore", (t) => {
  const directory = fixture(t), active = join(directory, "active.db"), input = join(directory, "input.db");
  create(active);
  create(input, 2);
  const tool = fileURLToPath(new URL("./library-recovery.mjs", import.meta.url));
  const checked = spawnSync(process.execPath, [tool, "validate", input], { encoding: "utf8" });
  assert.equal(checked.status, 0, checked.stderr);
  assert.match(checked.stdout, /Valid backup/);
  const refused = spawnSync(process.execPath, [tool, "restore", input, "--database", active], { encoding: "utf8" });
  assert.equal(refused.status, 1);
  assert.equal(validate(active).genres, 1);
  assertNoOpenDatabase(active);
});
