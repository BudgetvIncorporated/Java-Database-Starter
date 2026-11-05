# Administrator-only library recovery

**Scope:** This procedure restores the independent Java/SQLite library. The
browser companion now uses durable PostgreSQL; its SQLite downloads remain
compatible with this Java recovery tool. For browser import/recovery and
production cutover, follow [BROWSER-PERSISTENCE.md](../BROWSER-PERSISTENCE.md).
Restoring this Java file does not change browser records.

Downloading **Back up database** only creates a SQLite snapshot. It never restores,
merges, uploads, or replaces the library. Recovery deliberately has **no browser
button or HTTP endpoint**. A public visitor, signed-in non-manager, or library
player cannot request recovery.

Only the owner/approved administrator with trusted operating-system access to
the database host may run this procedure. A library player record is not an
administrator identity. Do not grant shell or file access to players. The
browser's existing verified-manager and same-origin checks still protect backup
downloads; this offline tool does not change or bypass them. Filesystem access
is an administrative capability, independent of Clerk sign-in.

## Before starting

- Schedule downtime and tell users not to edit. Restoring replaces **all four
  tables**, discarding every change made after the chosen backup. It is not a
  merge. Confirm the date and source of the downloaded backup with the owner.
- Work on the host/environment that owns the active SQLite file. A development
  workspace restore does not recover a separate published app. This procedure
  does not solve published-host persistence or synchronize environments.
- Use Node 24+ and Linux with `/proc` available, as in this workspace. Run as the
  database file's operating-system owner, without `sudo`. All app writers must
  run under that same account. The process scan is not protection against a
  separate administrator deliberately opening SQLite during maintenance.
- Keep the downloaded `.db` private: it includes player emails. Do not commit it,
  attach it to public messages, or place it in a frontend/public directory.
- Ensure sufficient free disk space for a staged backup, the complete current
  database and its sidecars, and temporary SQLite work. Keep a separate secure
  copy of the downloaded backup and any previous recovery archives.

Commands below run from the repository root. The default active file is exactly
`java-database-starter/game-library.db`; a missing file is an error, not a request
to initialize an empty library. For another environment, explicitly add
`--database /absolute/path/to/game-library.db` to **restore and resume**.

## 1. Validate without changing the active library

Privately transfer the browser download to the database host. For example, use
`java-database-starter/library-recovery/incoming.db` inside a directory created
with permissions `700` and a file with permissions `600`. That recovery directory
is excluded from Git and deployment. Do not overwrite the live `.db`.

```sh
umask 077
mkdir -p java-database-starter/library-recovery
chmod 700 java-database-starter/library-recovery
# Transfer the downloaded backup to .../library-recovery/incoming.db, then:
chmod 600 java-database-starter/library-recovery/incoming.db
node java-database-starter/recovery/library-recovery.mjs validate \
  java-database-starter/library-recovery/incoming.db
```

Validation opens the standalone input read-only in SQLite immutable mode, so a
WAL-mode download cannot create journal sidecars. A file with existing journal
sidecars is not treated as a standalone download; use the rollback staging steps
for a preserved multi-file set. Validation requires:

- SQLite `integrity_check` to return exactly `ok`.
- Exactly `genres`, `games`, `users`, and `user_games`, matching the approved
  schema's columns, keys, unique/check constraints, foreign-key actions and
  indexes. No triggers, views, extra tables, or automatic schema conversions.
  Formatting differences are accepted; unfamiliar schemas fail closed.
- No foreign-key violations, even if the file was written with enforcement off.
- Ratings of `NULL` or whole numbers 1–5, and nonnegative whole-number play
  counts within JavaScript's safe integer range. Fractional values are rejected.

Output contains only table counts, never player records. Review the expected
counts. If validation fails, stop and choose another backup; do not loosen the
schema or edit the original download to force acceptance.

## 2. Stop every writer

1. Stop **Game Library Swing** (close the desktop app and stop its workflow).
2. Stop any **older SQLite-backed API copies**. The current PostgreSQL browser API
   does not hold this Java file and need not stop for Java-only recovery.
3. Close SQLite viewers, scripts, scheduled jobs, or other Java/SQLite API copies.
   Stop equivalent writers on other hosts and disable automatic restarts.
4. Do not run Maven's desktop launch or restart SQLite writers until recovery ends.

The tool scans the database and journal/WAL file handles and refuses replacement
if a same-account process still has them open. It also installs a persistent
`game-library.db.recovery-lock` gate. Updated Java/SQLite-store startup checks that
gate both before and after opening a connection. Older app versions and external
SQLite tools do not honor it; keeping those stopped is the administrator's duty.

## 3. Explicitly replace the library

Only after the owner has approved the loss of newer changes:

```sh
node java-database-starter/recovery/library-recovery.mjs restore \
  java-database-starter/library-recovery/incoming.db \
  --writers-stopped --confirm REPLACE-LIBRARY
```

There is no default restore action. Missing confirmation, active writers, missing
database, invalid input, or an existing maintenance gate causes failure.

The command validates again, takes the gate, stages and validates a standalone
SQLite snapshot on the same filesystem, then preserves the **current** database
and every existing `-wal`, `-shm`, and `-journal` sidecar in a unique private
`library-recovery/before-*` directory. It records SHA-256 hashes in
`manifest.json` and flushes the preserved files to disk before replacement.
Even an already-damaged current database is preserved for investigation.

Only then are old sidecars removed and the staged database atomically renamed
into place. Old sidecars must never accompany the restored database. The command
validates the installed result and reports table counts and the preservation
directory. **It does not restart services or remove the gate.**

Record the printed preservation directory. It is also recorded in
`game-library.db.recovery-lock/preserved-path.txt` once preservation is complete.
Keep the archive until the owner has confirmed the recovered data.

## 4. Review and restart

Compare the reported counts with step 1. While all services remain stopped:

```sh
node java-database-starter/recovery/library-recovery.mjs validate \
  java-database-starter/game-library.db
node java-database-starter/recovery/library-recovery.mjs resume \
  --confirm RESUME-LIBRARY
```

`resume` checks that no handles are open and validates the active library before
removing the gate. Then:

1. Restart **Game Library Swing**. The Java initializer seeds
   only a new empty schema; it does not reinsert sample rows into a recovered
   library. An intentionally empty recovered library stays empty.
2. Confirm representative records, ratings, play counts and counts. If incorrect,
   stop services again and roll back before accepting further edits.
3. Browser records remain independent. To transfer this recovered snapshot to the
   browser, follow the explicit offline import in `BROWSER-PERSISTENCE.md`.
   Recovery never restores Clerk identities, permissions, or environment settings.

## If a command fails or the process/machine stops

Do **not** automatically restart or delete the gate. The current database may
still be the old file, or installation may have completed before interruption.
Read the error and preservation path; stop any remaining processes and validate
the current file. If it is the correct library, `resume` explicitly releases the
gate. If it is not, use the rollback procedure below.

Failure before any active-file changes leaves the original in place. A full
disk or permission failure during preservation prevents replacement. If a crash
occurs between sidecar removal and replacement, recover from the complete
preserved set, not from the possibly incomplete active set. Leftover
`.library-stage-*` directories are private temporary snapshots; remove them only
after the result has been established. They are excluded from Git/deployment.

## Roll back to the preserved library

Rollback also replaces data; obtain approval and stop **all** writers again.
Never copy just the main file from an archive that has a WAL or hot journal:
committed changes may exist only in those sidecars.

If the preserved archive contains only `library.db` and `manifest.json`, validate
it and use the normal `restore` command on that `library.db` after releasing an
existing gate **only while services stay disabled**. The command will preserve
the rejected current library in another `before-*` archive.

For an archive with sidecars, or when the current database is invalid and
`resume` correctly refuses to release the gate, use this fully offline route.
Do not alter the original archive:

1. Keep/create the `game-library.db.recovery-lock` directory, and keep all services
   and automatic restarts stopped. Do not run the next steps concurrently with
   the restore command.
2. Check archive files against `manifest.json` with `sha256sum` and compare each
   value. Stop on missing files or mismatches. The hashes detect accidental
   changes, not an untrusted administrator replacing both files and manifest.
3. Copy the current database **and any sidecars** to a new private directory for
   investigation. Flush those copies with `sync` before proceeding.
4. Prepare a private directory beside the active DB. Copy the archive's
   `library.db` and every archived sidecar there under those same names. Open
   **that copy**, not the archive, with SQLite to replay/checkpoint journals:

   ```sh
   # Substitute the private staging directory, never the live or archive path.
   node --input-type=module - /absolute/private/staging/library.db <<'NODE'
   import { DatabaseSync } from "node:sqlite";
   const db = new DatabaseSync(process.argv[2]);
   try {
     db.exec("PRAGMA wal_checkpoint(TRUNCATE); PRAGMA journal_mode = DELETE;");
   } finally { db.close(); }
   NODE
   node java-database-starter/recovery/library-recovery.mjs validate \
     /absolute/private/staging/library.db
   ```

5. If validation succeeds, ensure SQLite is closed. Remove the active
   `game-library.db-wal`, `game-library.db-shm`, and `game-library.db-journal`
   **only after the copies in step 3 exist**. Move the validated staged
   `library.db` over `game-library.db` using a same-filesystem rename, set mode
   `600`, and run `sync`. Never move a staged sidecar onto the recovered library.
6. Follow **Review and restart** above. `resume` must validate before removing
   the gate. If the preserved library itself is damaged, stop and choose an
   earlier valid backup; preservation cannot make already-corrupt data valid.

After either recovery or rollback, retain the private archives according to the
owner's retention policy, then delete them securely when no longer needed.
Never publish recovery archives, staging files, or the downloaded player data.

## Repeatable checks (temporary databases only)

```sh
node --test java-database-starter/recovery/library-recovery.test.mjs
cd java-database-starter && mvn test
```

These checks never replace the project's live database.
