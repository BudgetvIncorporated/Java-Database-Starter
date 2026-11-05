# Game Library

A school Java/SQLite game-library project with a browser companion and a separate classroom slideshow.

## Open and run

- Select **Game Library** in Preview for the browser app at `/`. It does not require a Run button or desktop/VNC pane.
- The managed workflows are `artifacts/game-library: web` and `artifacts/api-server: API Server`. Restart these after frontend/backend changes.
- The slideshow remains a separate artifact at `/game-library-presentation/`.
- The original Java Swing app remains in `java-database-starter`; its desktop workflow is `Game Library Swing`.

## Data and scope

- Both browser and Java apps use the existing `java-database-starter/game-library.db` SQLite file.
- Do not replace this with PostgreSQL, reseed it on server startup, or create an empty fallback database.
- Preserve the approved four tables: genres, games (one genre per game), users, and user_games (rating and play count).
- Game links open external sites; this is a library manager, not a playable game host.
- Browser writes and parent deletions affect the same data the Java app uses. Deleting a game/player cascades their saved-game links; removing a saved link leaves both parents intact.
- The browser catalog, summary, genres, and statistics are public read-only. All mutations, player details, saved-game records, and backups require the approved manager's Clerk sign-in.
- `LIBRARY_MANAGER_EMAIL` configures the sole manager in each environment. The API checks a verified email from Clerk, never a library player email or browser-provided role. An unset value denies all management. Sign-ups do not grant access.
- Development and production use separate Clerk identities; the owner must verify their configured email in each environment. Durable SQLite storage still needs a decision before public deployment.
- Backup downloads include player emails. `/api/library/backup` uses SQLite's online backup API, transfers a private temporary file, then deletes it. Nothing is retained server-side or written into the repository.
- Management operations and backups require same-origin browser Fetch Metadata as well as manager authorization. Clerk cookies authenticate same-origin web requests; do not add browser bearer tokens.

## Implementation and checks

- Browser: `artifacts/game-library`, React/Vite.
- API: `artifacts/api-server`, Express and Node 24's built-in `node:sqlite`.
- Contract: `lib/api-spec/openapi.yaml`; regenerate with `pnpm --filter @workspace/api-spec run codegen`.
- Generated hooks: `@workspace/api-client-react`; validation: `@workspace/api-zod`.
- `pnpm --filter @workspace/api-server test` exercises CRUD, validation, cascades, and persisted writes against a temporary backup of the real SQLite database, never against the original.
- `pnpm run typecheck` checks workspace types.
- `cd java-database-starter && mvn test` checks the unchanged Java app.
