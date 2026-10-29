---
name: Game library schema scope
description: The user's approved table design and whether the app hosts playable games.
---

The user is building a game library for a school Java/SQLite assignment. The user explicitly chose to match their four-table diagram: `genres`, `games` (one genre per game), `users`, and `user_games` (the user/game junction with rating and play count). The app must let users enter information and show which user plays which game. Do not add developers or a separate game/genre junction table unless the user or teacher asks for that change. The app opens saved game links; hosting games inside it is not part of the current scope.

**Why:** The user confirmed that the SQL should match the diagram, and a later expanded model no longer matched that decision.

**How to apply:** Keep future schema and Java persistence changes consistent with the four tables, provide an entry workflow, and show the user/game association. Check the current Java code before running it against the simplified schema, because it may still expect the expanded model. Do not assume in-site game hosting is approved.
