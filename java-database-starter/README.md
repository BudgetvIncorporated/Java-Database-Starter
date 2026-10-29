# Game Library

A Java and SQLite catalog of games, developers, and genres. A game can have
multiple genres and belongs to one developer. Users can also save games in
personal libraries with ratings and play counts. Saved links open official or
store pages; this app does not host full games.

## Run the console app

From this directory, run:

```sh
mvn compile exec:java
```

The app uses `game-library.db` by default. On startup, it applies `schema.sql`
and then `seed.sql`. Re-running the sample seed does not create duplicate
records. To use another database file, add
`-Dgame.catalog.db=/path/to/catalog.db` to the Maven command.

The menu lets you browse and sort games, search titles, filter by genre, open a
saved URL, view user activity, and list a user's saved games with ratings and
play counts. It also supports full create, read, update, and delete workflows
for games and developers. Game forms let users choose a developer and assign
existing genres. Games cannot be saved with blank titles, invalid links, or
invalid dates; developers still referenced by games cannot be deleted. User
values are bound through prepared statements.

## Import the SQL files into SQLite

With a tool such as DB Browser for SQLite:

1. Create or open a file named `game-library.db`.
2. In **Execute SQL**, run `src/main/resources/schema.sql`.
3. Run `src/main/resources/seed.sql`.
4. Browse `developers`, `genres`, `games`, `game_genres`, `users`, and
   `user_games` to inspect the records.

Run the schema before the seed because the seed inserts into those tables. To
rebuild the sample database, close it, remove `game-library.db`, create a fresh
database file with that name, and run the scripts in order. Removing that file
deletes its contents.

The earlier draft database, `database.db`, is left untouched. The current Java
program uses `game-library.db` so the old draft data is not overwritten.

## Where things live

- `src/main/resources/schema.sql` creates developers, games, genres,
  game-genre links, users, and user-game records.
- `src/main/resources/seed.sql` inserts sample games, developers, genres,
  users, and user-game ratings/play counts.
- `Database.java` opens SQLite and enables foreign-key checks.
- `SchemaInitializer.java` applies the schema and seed scripts.
- `GameCatalogDao.java` contains prepared queries for browsing and reports.
- `GameDao.java` and `DeveloperDao.java` keep catalog CRUD SQL out of the menu.
- `PROJECT_PLAN.md` contains the ER diagram, normalization notes, SQL examples,
  and the assignment sequence.
