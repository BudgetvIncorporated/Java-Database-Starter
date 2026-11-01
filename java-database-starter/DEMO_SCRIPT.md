# Game Library Demo Speaking Notes

## Before the presentation

From a terminal, run:

```bash
cd java-database-starter
mvn test
mvn compile exec:java
```

The app creates `game-library.db` and seeds it the first time it runs. Keep the
database available when presenting; the generated database file is local and is
not committed to GitHub.

## Five-minute script

### 0:00–1:00 — Introduce the schema

> “This project is a game library built with Java and SQLite. It has four
> tables: `genres`, `games`, `users`, and `user_games`. A genre can classify
> multiple games. Users and games have a many-to-many relationship, which is
> represented by the `user_games` junction table. Ratings and play counts are
> stored on that relationship because they belong to a user's copy of a game.”

### 1:00–2:00 — Show the joined list

> “I’ll choose option 1. This list comes from a query that joins `users`,
> `user_games`, and `games`, so it shows which users have saved which games.
> The genre filter joins the `genres` table as well.”

### 2:00–3:00 — Show search and safe input

> “Options 2 and 3 let me search by title and filter by genre. The values I
> enter are bound through JDBC `PreparedStatement` parameters, rather than
> being pasted into SQL. That keeps search text from being interpreted as SQL.”

Search for a title such as `Mario`, then use the genre filter.

### 3:00–4:00 — Show SQL aggregates and the Java algorithm

> “Option 12 groups the library by user. It uses `COUNT` for saved games,
> `SUM` for total play count, and `AVG` for the average rating. Users without
> saved games still appear with zero counts. Option 13 sorts game titles with
> insertion sort written in Java.”

Choose options 12 and 13.

### 4:00–5:00 — Show CRUD and close

> “The menu supports creating, reading, updating, and deleting games and users.
> I keep the SQL in DAO classes so the menu and data access stay separate.
> The tests exercise full CRUD for both entities, as well as the aggregate
> report and sorting algorithm. This completes the database-backed game
> library.”

If demonstrating a write live, create a temporary user with option 4, then edit
and delete that same user with options 5 and 6. Do not delete seeded sample
records during the presentation.
