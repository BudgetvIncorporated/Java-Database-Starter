# Game Library

A school Java/SQLite game-library project with a browser companion and a separate classroom slideshow.

## Open and run

- Select **Game Library** in Preview for the browser app at `/`. It does not require a Run button or desktop/VNC pane.
- The managed workflows are `artifacts/game-library: web` and `artifacts/api-server: API Server`. Restart these after frontend/backend changes.
- The slideshow remains a separate artifact at `/game-library-presentation/`.
- The original Java Swing app remains in `java-database-starter`; its desktop workflow is `Game Library Swing`.

## Data and scope

- The browser companion uses durable PostgreSQL. The independent Java app keeps `java-database-starter/game-library.db` and its original SQLite model.
- Browser/Java records are now separate; exchange validated SQLite backups explicitly. Never seed, import, migrate schema, or fall back to SQLite on server startup.
- Preserve the approved four tables: genres, games (one genre per game), users, and user_games (rating and play count).
- Game links open external sites; this is a library manager, not a playable game host.
- Browser writes affect PostgreSQL, not the Java database. Deleting a game/player cascades their saved-game links; removing a saved link leaves both parents intact.
- The browser catalog, summary, genres, and statistics are public read-only. All mutations, player details, saved-game records, and backups require the approved manager's Clerk sign-in.
- `LIBRARY_MANAGER_EMAIL` configures the sole manager in each environment. The API checks a verified email from Clerk, never a library player email or browser-provided role. An unset value denies all management. Sign-ups do not grant access.
- Development and production have separate PostgreSQL data and Clerk identities; the owner must verify their configured email in each environment.
- Backup downloads include player emails. `/api/library/backup` takes a consistent PostgreSQL snapshot, builds the exact approved SQLite schema in a private temporary file, transfers it, and deletes it. No download is retained.
- Management operations and backups require same-origin browser Fetch Metadata as well as manager authorization. Clerk cookies authenticate same-origin web requests; do not add browser bearer tokens.
- Recovery is a separate, offline administrator operation, not a browser upload or API endpoint. Follow `java-database-starter/RECOVERY.md`; the Node 24 CLI validates backups, requires explicit replacement confirmation and stopped writers, preserves the current DB plus sidecars, and leaves a maintenance gate until explicit validated resume.
- Java and the SQLite compatibility/test store refuse a `.recovery-lock` gate. The browser no longer opens that file. Keep Java and SQLite tools stopped during Java recovery. Private `library-recovery/` archives and `.library-stage-*` files must never be committed or published.
- Java seeds only a fresh empty schema; restarting it must never reinsert sample data into an existing/restored library.
- Follow `BROWSER-PERSISTENCE.md` for browser import/recovery and the production cutover. Initial import refuses nonempty PostgreSQL; replacement requires stopped writers, explicit confirmation, and a preserved SQLite before-image. Managed production schema/data transfer belongs to the owner's Publish flow, never a startup/build migration.

## Implementation and checks

- Browser: `artifacts/game-library`, React/Vite.
- API: `artifacts/api-server`, Express/PostgreSQL; Node 24 `node:sqlite` produces compatible exports and supports offline tools/tests.
- Contract: `lib/api-spec/openapi.yaml`; regenerate with `pnpm --filter @workspace/api-spec run codegen`.
- Generated hooks: `@workspace/api-client-react`; validation: `@workspace/api-zod`.
- `pnpm --filter @workspace/api-server test` exercises the SQLite compatibility API on disposable snapshots, and PostgreSQL CRUD, process-restart persistence, export validation, and atomic recovery on a disposable schema. No production fixtures.
- `pnpm run typecheck` checks workspace types.
- `cd java-database-starter && mvn test` checks the Java app and recovery startup protection.
- `node --test java-database-starter/recovery/library-recovery.test.mjs` checks offline validation, recovery refusal, preservation, and explicit resume on disposable databases.
