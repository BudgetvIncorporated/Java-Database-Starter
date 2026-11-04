# Java SQLite Game Library

A desktop catalog for browsing games and managing a small user's library. Each
game has a title, launch link, publication date, and genre. A user can save a
game to their library with a rating and play count. The saved link opens the
game's official page or store page; this project does not host or run game
files.

## What works

- Java Swing screens for the game catalog, users, and library reports.
- Search by game title and filter by genre.
- Create, read, update, and delete games and users.
- Save a game to a user's library with optional 1–5 rating and non-negative
  play count.
- Open a saved game link when a browser is available; otherwise, show the link
  for copying.
- A three-table JOIN report shows which players saved each game.
- A `GROUP BY` report calculates saved-game `COUNT`, play-count `SUM`, and
  rating `AVG` for each user.
- Game titles can be sorted with a custom Java insertion-sort algorithm.
- Form validation, SQL `PreparedStatement` parameters, and useful database
  error messages.
- Maven tests cover database CRUD, query behavior, and input validation.

## Tools and project components

| Component | Purpose |
| --- | --- |
| Java 17+ and Swing | Desktop application and user interface |
| SQLite | Local relational database stored in `game-library.db` |
| sqlite-jdbc | JDBC driver connecting Java to SQLite |
| Maven | Dependency management, tests, build, and launch |
| DAO classes | Keep SQL operations separate from the UI |
| JUnit 4 | Automated checks for persistence and validation |
| GitHub | Source-code version history and project backup |
| Replit VNC | Displays the Swing desktop window in the browser |

Main Java pieces:

- `Main` starts the desktop app, initializes the schema and sample data, then
  opens the Swing window.
- `GameLibraryFrame` provides the catalog, user, and reports tabs.
- `GameDao` and `UserDao` handle CRUD and user-library writes.
- `GameCatalogDao` supplies catalog lists, JOIN results, and grouped reports.
- `CatalogValidation` checks titles, email addresses, links, and dates.
- `GameAlgorithms` contains the Java insertion sort.
- `schema.sql` defines the four tables; `seed.sql` inserts example rows.

## Run the application

The Swing window needs a graphical desktop. In Replit, run it through the
**Game Library Swing** VNC workflow (or another VNC/desktop session), not a
headless terminal:

```bash
cd java-database-starter
mvn test
mvn compile exec:java
```

If running manually in a graphical terminal, use the same commands. Maven
downloads the JDBC driver and test dependency on the first build. The app
creates/updates `game-library.db` in the current working directory, then
applies the schema and inserts any missing sample rows. The local database
contains user-entered changes and should not be deleted if you want to keep
them.

To build without opening the app:

```bash
cd java-database-starter
mvn clean test package
```

The automated test suite can run in a headless shell; only the Swing window
requires a display.

## Using the interface

1. **Game catalog:** search by title, filter by genre, add/edit/delete games,
   open a game's link, or sort the visible results with Java insertion sort.
2. **Users:** add, edit, and delete a user. Deleting a user also removes their
   saved-game links.
3. **Library & reports:** add a game to a user's library, then view the
   game/player JOIN and per-user statistics. Deleting a game also removes its
   saved-game links.

Dates must use `YYYY-MM-DD`. Game links must begin with `http://` or `https://`.
Ratings are optional and must be from 1 to 5; play counts must be zero or
greater. The database also enforces unique usernames/emails and a unique
game-title/publication-date pair.

## Database schema

The database has four tables:

- `genres` — one row per genre.
- `games` — title, URL, publication date, and a foreign key to one genre.
- `users` — username and email.
- `user_games` — junction table connecting users and games; stores the user's
  rating and play count for that game.

`genres` to `games` is one-to-many. `users` and `games` are many-to-many through
`user_games`, whose composite primary key `(user_id, game_id)` prevents
duplicate saves for a user. Foreign keys, checks, and unique constraints help
protect the data. See `src/main/resources/schema.sql` and
`src/main/resources/seed.sql`.

## Example database questions

1. **Which games are in a selected genre?** Join `games` to `genres` and filter
   on the selected genre.
2. **Which players saved each game?** Join `user_games`, `games`, and `users`.
   This is the report shown in the app.
3. **How many games and plays does each user have, and what is their average
   rating?** Group by user and calculate `COUNT`, `SUM`, and `AVG`. This report
   is shown in the app.
4. **Which games have the highest average rating?** Group library ratings by
   game, then sort the results by average rating.

The full sample SQL for these questions is in `PROJECT_PLAN.md`.

## Testing and presentation demo

Run `mvn test` before presenting. The suite checks DAO operations, reports,
search/filter behavior, and input validation.

Suggested short demo:

1. Open the game catalog and filter by a genre, then search for a title.
2. Sort by title and describe that the ordering uses Java insertion sort.
3. Show the game/player JOIN report and the user aggregate report.
4. Add a temporary game or user, edit it, and delete it to demonstrate CRUD.
5. Add a game to a user's library and explain the junction table.

See `DEMO_SCRIPT.md` for speaking notes. Explain the schema in your own words
and verify the application is open in VNC before presenting.

## Reflection

The junction table is important because a user can save many games and each
game can be saved by many users. Ratings and play counts belong on that
relationship, rather than on the game itself. If this project grew, I would
consider adding a dedicated screen to edit or remove an individual saved-game
entry and adding more automated tests for Swing interactions.
