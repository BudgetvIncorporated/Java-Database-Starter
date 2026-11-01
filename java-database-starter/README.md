# Game Library

A Java console app backed by SQLite. Users can browse a game catalog, search and
filter it, save games to personal libraries, record ratings and play counts, and
open a game's saved link. The app opens external game pages; it does not host
playable games.

## Run and test

From this directory, run:

```sh
mvn test
mvn compile exec:java
```

The project targets Java 17 and uses the `sqlite-jdbc` driver declared in
`pom.xml`. The app uses `game-library.db` by default. On startup,
`SchemaInitializer` applies `schema.sql` and `seed.sql`. The seed can be rerun
without duplicating its sample records. To choose another database file, add
`-Dgame.catalog.db=/path/to/catalog.db` to the Maven command.

## Features

- Create, read, update, and delete games and users.
- Add a game to a user's library with an optional 1–5 rating and play count.
- Show which users saved which games; search by title and filter by genre.
- Open a selected game's saved link.
- Show each user's saved-game count, total play count, and average rating.
- Sort games by title in Java using insertion sort.
- Validate user input and use prepared statements for SQL values.

## Database model

The four tables match the project ER diagram:

- `genres` stores each genre once.
- `games` stores a title, link, release date, and one `genre_id`.
- `users` stores usernames and email addresses.
- `user_games` joins users to games and stores each user's rating and play count.

`genres` to `games` is one-to-many. `users` to `games` is many-to-many through
`user_games`, whose composite primary key prevents a user from saving the same
game twice. The rating and play count belong to that user/game relationship.

## Recreate the database manually

In SQLite or DB Browser for SQLite, run these files in order:

1. `src/main/resources/schema.sql`
2. `src/main/resources/seed.sql`

The schema must run first because the seed inserts rows into its tables. The
generated `.db` files and Maven `target` folder are ignored by Git; the SQL
scripts are the reproducible database setup.

## Questions the database can answer

### 1. Which games belong to a selected genre?

```sql
SELECT g.title, g.release_date, ge.genre_name
FROM games AS g
JOIN genres AS ge ON ge.genre_id = g.genre_id
WHERE ge.genre_name = 'Puzzle'
ORDER BY g.title;
```

### 2. Which games has a user saved?

```sql
SELECT u.username, g.title, ge.genre_name, ug.rating, ug.play_count
FROM users AS u
JOIN user_games AS ug ON ug.user_id = u.user_id
JOIN games AS g ON g.game_id = ug.game_id
JOIN genres AS ge ON ge.genre_id = g.genre_id
WHERE u.username = 'player01'
ORDER BY g.title;
```

This query joins four tables.

### 3. What are each user's saved-game count, total plays, and average rating?

```sql
SELECT u.username,
       COUNT(ug.game_id) AS saved_games,
       COALESCE(SUM(ug.play_count), 0) AS total_plays,
       ROUND(AVG(ug.rating), 2) AS average_rating
FROM users AS u
LEFT JOIN user_games AS ug ON ug.user_id = u.user_id
GROUP BY u.user_id, u.username
ORDER BY saved_games DESC, u.username;
```

### 4. Which games have the most total plays?

```sql
SELECT g.title, COALESCE(SUM(ug.play_count), 0) AS total_plays
FROM games AS g
LEFT JOIN user_games AS ug ON ug.game_id = g.game_id
GROUP BY g.game_id, g.title
ORDER BY total_plays DESC, g.title;
```

### 5. Which games have the highest average rating?

```sql
SELECT g.title, ROUND(AVG(ug.rating), 2) AS average_rating,
       COUNT(ug.rating) AS rating_count
FROM games AS g
JOIN user_games AS ug ON ug.game_id = g.game_id
GROUP BY g.game_id, g.title
HAVING COUNT(ug.rating) >= 2
ORDER BY average_rating DESC, rating_count DESC;
```

## Input validation, errors, and edge cases

The console checks for blank required text, basic email format, HTTP/HTTPS game
links, real `YYYY-MM-DD` dates, listed IDs, ratings from 1 to 5, and non-negative
play counts. Non-numeric menu choices and invalid field values prompt again.
Database errors such as duplicate usernames, duplicate emails, duplicate saved
games, or missing foreign-key records are reported without ending the menu.

Run `mvn test` to check validation and database edge cases, including malformed
email/URL/date values, blank text, duplicate records, missing references,
nonexistent update/delete IDs, SQL-like search text, and users with no saved
games.

## Schema reflection

I kept one genre per game because that matches the approved diagram and keeps
the project manageable. If I redesigned the schema, I would consider a
`game_genres` junction table so a game could belong to multiple genres, which
would better represent games that span genres.

## Five-minute demo outline

1. **0:00–1:00 — Explain the schema.** Show the four tables and explain the
   one-to-many genre relationship and the many-to-many user/game relationship
   through `user_games`.
2. **1:00–2:00 — Run the app.** Use menu option 1 to show the joined game/player
   list. Explain that the query combines `users`, `user_games`, and `games`.
3. **2:00–3:00 — Show search and filtering.** Use options 2 and 3, and point out
   that entered values are passed through prepared statements.
4. **3:00–4:00 — Show the reports.** Use option 12 for the `GROUP BY` report
   with `COUNT`, `SUM`, and `AVG`; option 13 demonstrates the Java insertion sort.
5. **4:00–5:00 — Demonstrate CRUD and validation.** Add, edit, and remove a
   temporary game or user, then mention the DAO tests. Avoid changing the seeded
   sample records during the demo.

## Main files

- `src/main/resources/schema.sql` creates the four tables and their constraints.
- `src/main/resources/seed.sql` adds sample genres, users, games, and libraries.
- `Database.java` opens SQLite and enables foreign-key checks.
- `CrudDao.java` defines the CRUD interface; `GameDao.java` and `UserDao.java`
  implement it.
- `GameCatalogDao.java` contains prepared queries for lists and reports.
- `GameAlgorithms.java` sorts titles in Java.
- `UserLibraryStats.java` holds the grouped count, play total, and average rating.
- `PROJECT_PLAN.md` contains the ER diagram, normalization notes, and milestone
  sequence.
