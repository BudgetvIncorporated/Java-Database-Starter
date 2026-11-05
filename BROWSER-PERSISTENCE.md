# Browser persistence and Java-compatible backups

The owner approved PostgreSQL for the browser companion. The school Java project
still uses its original four-table SQLite database. They no longer share a live
file; transferring a validated backup is an explicit administrator operation.

## Why this is necessary

The current published hosting is Replit Autoscale. Its local filesystem is
temporary: neither Autoscale nor Reserved VM supplies a durable SQLite volume.
The browser therefore uses Replit's managed PostgreSQL database, which is
independent of server processes and release files. Server startup only checks
the expected columns; it never creates tables, imports data, or seeds examples.
A missing database/schema is an error, not an empty SQLite fallback.

Sources:
- https://docs.replit.com/features/publishing/overview
- https://docs.replit.com/features/data-and-storage/sql-database
- https://docs.replit.com/features/data-and-storage/object-storage

## Production cutover — owner action required

**Development changes do not migrate the currently published SQLite records.**
Do not republish over the old library until its latest records are preserved.

1. Sign in as the approved manager on the **published** site and download a fresh
   SQLite backup. Keep it privately outside the repository.
2. Pause library edits until cutover is complete, so the backup remains current.
3. Bring that standalone backup into the development workspace privately.
   Stop the API workflow before importing. Use the initial import command below
   only for an empty browser database; if development already has records, use the
   explicit replacement process with its preserved before-image.
4. Validate/review the imported records and counts in Preview.
5. Publish using Replit's normal schema-diff flow to create the four production
   PostgreSQL tables. In publishing's data-transfer/overwrite option, deliberately
   copy the reviewed development database to production. This replaces production
   PostgreSQL data: preserve any existing PostgreSQL records first. Do not proceed
   if that option is unavailable or the reviewed data is not the latest library.
6. On the published site, sign in with its separately verified manager account;
   compare record counts and download/validate a new SQLite backup. Keep the
   pre-cutover backup until this is confirmed.

No code here automatically publishes, writes production schema, copies
development data into production, or fetches private production records.
Subsequent normal publishes should **not** overwrite production data.

## Initial import into the development browser database

With API writers stopped, from the workspace root:

```sh
pnpm --filter @workspace/api-server library:import /private/path/library.db \
  --writers-stopped --confirm IMPORT-SQLITE-TO-BROWSER
```

The command validates the exact approved SQLite schema, checks integrity and
foreign keys, rejects sidecars, imports only into an empty development PostgreSQL
library, and preserves IDs/relationships. It refuses production runtime use and
never changes the source SQLite file or the independent Java database.
It does not create schema: use the normal development Drizzle schema flow first.

## Recovery/replacement

For the Java app, continue following `java-database-starter/RECOVERY.md`.
PostgreSQL exports use that same strict SQLite schema and validator.

For the development browser library, stop all API writers and run:

```sh
pnpm --filter @workspace/api-server library:import /private/path/library.db \
  --writers-stopped --replace --confirm REPLACE-BROWSER-LIBRARY
```

Before replacing records, this preserves a private SQLite before-image under
`java-database-starter/library-recovery/browser-before-*/library.db`. Replacement
is one PostgreSQL transaction with exclusive table locks. A failed import rolls
back the data change. The before-image is captured while those locks are held;
failure to preserve it aborts replacement before deleting any records.
Archives are excluded from Git and publishing. Treat them
as sensitive: they contain player emails. Restoring a before-image uses the same
validated replacement command; there is no browser upload/recovery endpoint.

For production recovery, stage and verify the recovery in development, preserve
a current production export, and use the owner's explicit publishing
data-transfer flow. Do not add production DDL, startup migration scripts, or
automatic restore-on-boot behavior.

## Verification

```sh
pnpm --filter @workspace/api-server test
```

The PostgreSQL suite creates and drops a unique **development-only** schema with
synthetic records. It edits through the real API router, shuts down the server
and pool, starts a fresh child process in an empty temporary directory, verifies
the committed values, exports SQLite, runs the Java recovery validator, and
checks that an invalid replacement leaves the original rows intact.
It never writes test records into the application tables or production.
