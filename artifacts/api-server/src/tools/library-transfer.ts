import { copyFile, mkdir, chmod } from "node:fs/promises";
import { spawnSync } from "node:child_process";
import { join, resolve } from "node:path";
import { pool } from "@workspace/db";
import { PostgresLibraryStore } from "../lib/library-postgres";
import { findJavaProject, readSqliteSnapshot, writeSqliteSnapshot, type Snapshot } from "../lib/library-snapshot";

async function main() {
  const [file, ...flags] = process.argv.slice(2);
  const replace = flags.includes("--replace");
  const expected = replace ? "REPLACE-BROWSER-LIBRARY" : "IMPORT-SQLITE-TO-BROWSER";
  if (!file || flags.some((flag) => !["--replace", "--writers-stopped", "--confirm", expected].includes(flag)) ||
      flags[flags.indexOf("--confirm") + 1] !== expected || !flags.includes("--writers-stopped")) {
    throw new Error(`Usage: library:import <standalone-backup.db> --writers-stopped ${replace ? "--replace " : ""}--confirm ${expected}`);
  }
  // This is a development/offline data tool, not a production schema migrator.
  // Production receives schema/data only through the owner's Publish flow.
  if (process.env.NODE_ENV === "production" || process.env.REPLIT_DEPLOYMENT === "1") {
    throw new Error("Run this offline tool in development. Use Publish's schema/data transfer for production.");
  }
  const source = resolve(file);
  const project = findJavaProject();
  const check = spawnSync(process.execPath, [join(project, "recovery/library-recovery.mjs"), "validate", source], {
    encoding: "utf8",
  });
  if (check.status !== 0) throw new Error(`Backup validation refused import: ${check.stderr.trim()}`);
  const rows = readSqliteSnapshot(source);
  const store = new PostgresLibraryStore(pool);
  await store.assertReady();
  const preserve = async (before: Snapshot) => {
    const backup = await writeSqliteSnapshot(before);
    try {
      const directory = join(project, "library-recovery", `browser-before-${Date.now()}`);
      await mkdir(directory, { recursive: true, mode: 0o700 });
      const target = join(directory, "library.db");
      await copyFile(backup.path, target, 1); // COPYFILE_EXCL: never overwrite an archive.
      await chmod(target, 0o600);
      console.log(`Preserved the previous browser library at ${target}`);
    } finally { await backup.cleanup(); }
  };
  await store.importSnapshot(rows, replace, replace ? preserve : undefined);
  console.log("Imported the validated SQLite snapshot into the development browser database.");
  console.log("The Java SQLite database and published database were not changed.");
}

main().catch((error) => {
  console.error(error instanceof Error ? error.message : "Library import failed.");
  process.exitCode = 1;
}).finally(() => pool.end());
