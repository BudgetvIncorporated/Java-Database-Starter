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
- Show saved-game counts per user using a `GROUP BY` query.
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

### 3. How many games has each user saved?

```sql
SELECT u.username, COUNT(ug.game_id) AS saved_games
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

## Schema reflection

I kept one genre per game because that matches the approved diagram and keeps
the project manageable. If I redesigned the schema, I would consider a
`game_genres` junction table so a game could belong to multiple genres, which
would better represent games that span genres.

## Main files

- `src/main/resources/schema.sql` creates the four tables and their constraints.
- `src/main/resources/seed.sql` adds sample genres, users, games, and libraries.
- `Database.java` opens SQLite and enables foreign-key checks.
- `GameDao.java`, `UserDao.java`, and `GameCatalogDao.java` separate SQL from
  the menu.
- `PROJECT_PLAN.md` contains the ER diagram, normalization notes, and milestone
  sequence.
