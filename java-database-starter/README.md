# Game Library

A Java and SQLite catalog for storing game titles, publisher/developer links,
release dates, and genres. Selecting a game can open its saved web page. The
sample links go to official game pages or store listings; they do not promise
that the game can be played inside this app.

## Run the Java starter

From this directory, run:

```sh
mvn compile exec:java
```

The app creates `database.db` if it does not exist, then applies `schema.sql`
and `seed.sql`. The seed uses `INSERT OR IGNORE`, so restarting the app does
not add duplicate sample rows. When it starts, a console menu lets you browse
games by title, search titles, filter by genre, open a saved game page in your
browser, and view a games-per-developer report. If a browser is unavailable,
the app prints the saved page instead.

Game search and genre filtering use prepared SQL statements. The catalog query
joins games, developers, and genres through the `game_genres` table; the
developer report uses `LEFT JOIN`, `GROUP BY`, and `COUNT` so developers with
no games would still appear. Browse/search results are sorted by title in Java.
Invalid menu choices and database errors are reported without a stack trace.

## Import the SQL files yourself

Use a SQLite tool such as DB Browser for SQLite:

1. Create or open `database.db`.
2. Open the **Execute SQL** view and run `src/main/resources/schema.sql`.
3. Run `src/main/resources/seed.sql`.
4. Browse the `games`, `developers`, `genres`, and `game_genres` tables to
   confirm the data loaded.

Run `schema.sql` first because `seed.sql` depends on its tables. To start over,
close the database tool, delete `database.db`, create a fresh database, and run
the two files in that order.

## Project order and deliverables

See `PROJECT_PLAN.md` for the proposed ER diagram, database questions, and
milestone-by-milestone steps from the assignment.

## Where things live

- `src/main/resources/schema.sql` defines the tables and relationships.
- `src/main/resources/seed.sql` adds sample data: 20 games, developers,
  genres, and game-to-genre links.
- `Database.java` opens SQLite and enables foreign-key checks.
- `SchemaInitializer.java` applies both SQL files.
- `Main.java` starts the program and closes the connection.
