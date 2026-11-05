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
- Browser access is currently for the workspace demo, without account-based authorization. Do not treat it as a protected public service or publish it without deciding access control and durable SQLite storage.
- Backup downloads include player emails. `/api/library/backup` uses SQLite's online backup API, transfers a private temporary file, then deletes it. Nothing is retained server-side or written into the repository.
- Until authenticated management access exists, backups fail closed outside `NODE_ENV=development` and require same-origin browser Fetch Metadata. This is a workspace-demo boundary, not user authentication; management-policy changes must update the backup guard too.

## Implementation and checks

- Browser: `artifacts/game-library`, React/Vite.
- API: `artifacts/api-server`, Express and Node 24's built-in `node:sqlite`.
- Contract: `lib/api-spec/openapi.yaml`; regenerate with `pnpm --filter @workspace/api-spec run codegen`.
- Generated hooks: `@workspace/api-client-react`; validation: `@workspace/api-zod`.
- `pnpm --filter @workspace/api-server test` exercises CRUD, validation, cascades, and persisted writes against a temporary backup of the real SQLite database, never against the original.
- `pnpm run typecheck` checks workspace types.
- `cd java-database-starter && mvn test` checks the unchanged Java app.
