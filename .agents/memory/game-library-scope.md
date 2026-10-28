---
name: Game catalog scope
description: The user's assignment project idea and current boundary between a game catalog and hosting games.
---

The user is building a game library for a school Java/SQLite assignment. Their chosen ERD has Games (one genre per game), Genre, Users, and UserGames (a many-to-many junction storing each user's rating and play count). The app opens a game's saved link. They are undecided about instead making a game and running it in the site.

**Why:** This is the user's stated project direction, and the scope affects the schema, data, and Java features.

**How to apply:** Treat this four-table ERD as the current database model and keep SQLite mandatory. Do not assume in-site game hosting is approved; revisit scope if the user chooses it. The assignment calls for teacher approval of the data model before new Java feature development.
