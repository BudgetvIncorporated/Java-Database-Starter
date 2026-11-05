---
name: Game library schema scope
description: Approved school model, browser access, and the boundary of storage-migration consent.
---

The user is building a game library for a school Java/SQLite assignment. The user explicitly chose to match their four-table diagram: `genres`, `games` (one genre per game), `users`, and `user_games` (the user/game junction with rating and play count). The `games` object stores a title, link, and release date; the user selects a game and opens its link. The user has considered making games run inside the site, but has not confirmed that as the project scope. Do not add developers or a separate game/genre junction table unless the user or teacher asks for that change.

**Why:** The user confirmed that the SQL should match the diagram, and a later expanded model no longer matched that decision. Opening links is the current, lower-effort direction; hosting playable games would be a separate scope choice.

**How to apply:** Keep future schema and Java persistence changes consistent with the four tables, provide an entry workflow, and show the user/game association. Check the current Java code before running it against the simplified schema, because it may still expect the expanded model. Keep external game links as the default unless the user explicitly chooses to build and run games in the site.

The user approved a browser companion alongside the Java assignment and slideshow because their workspace did not show a Run button or the desktop/VNC pane, despite the Java window running.

**Why:** Repeated instructions to run Swing or look for VNC did not give the user access to the app.

**How to apply:** Make browser Preview the primary user-facing entry point. Do not describe a running native Java window as proof that the user can see or use the app.

The owner explicitly chose the durable-browser-database option rather than
external SQLite hosting, accepting that the browser and Java apps exchange
SQLite backups instead of sharing one live file. That consent is browser-only.

**Why:** Published local files are temporary; durability must not require changing
the Java/SQLite school assignment.

**How to apply:** Do not interpret this approval as permission to migrate the Java
assignment away from SQLite or expand its four-table model.
