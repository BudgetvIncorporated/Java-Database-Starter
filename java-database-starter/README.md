# Game Library

A Java and SQLite catalog based on the project's ER diagram. It stores games,
one genre per game, users, and the games saved by each user. A saved launch URL
opens an official or store page; this app does not host full games.

## Run the console app

From this directory, run:

```sh
mvn compile exec:java
```

The app uses `game-library.db`. On startup, it applies `schema.sql` and then
`seed.sql`. Re-running the sample seed does not create duplicate records.

The menu lets you browse and sort games, search titles, filter by genre, open a
saved URL, view an activity summary, and list a user's saved games with ratings
and play counts. Search and user lookups use prepared statements.

## Import the SQL files into SQLite

With a tool such as DB Browser for SQLite:

1. Create or open a file named `game-library.db`.
2. In **Execute SQL**, run `src/main/resources/schema.sql`.
3. Run `src/main/resources/seed.sql`.
4. Browse `genres`, `users`, `games`, and `user_games` to inspect the records.

Run the schema before the seed because the seed inserts into those tables. To
rebuild the sample database, close it, remove `game-library.db`, create a fresh
database file with that name, and run the scripts in order. Removing that file
deletes its contents.

The earlier draft database, `database.db`, is left untouched. The current Java
program uses `game-library.db` so the old draft data is not overwritten.

## Where things live

- `src/main/resources/schema.sql` creates the four tables and their
  relationships.
- `src/main/resources/seed.sql` inserts 22 games, 20 sample users, 15 genres,
  and 40 user-game records.
- `Database.java` opens SQLite and enables foreign-key checks.
- `SchemaInitializer.java` applies the schema and seed scripts.
- `GameCatalogDao.java` contains prepared queries for the menu and reports.
- `PROJECT_PLAN.md` contains the ER diagram, normalization notes, SQL examples,
  and the assignment sequence.
