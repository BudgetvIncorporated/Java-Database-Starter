# Game Library Swing Demo

## Before presenting

1. Open the **Game Library Swing** workflow so the application window appears
   in Replit's VNC pane.
2. Wait until the catalog and sample data finish loading.
3. Keep the presentation database intact; do not delete the `.db` file.

If running from a desktop terminal instead:

```bash
cd java-database-starter
mvn test
mvn compile exec:java
```

## Five-minute walkthrough

### 0:00–1:00 — Purpose and schema

> “This is a Java Swing game library backed by SQLite. It stores games, genres,
> users, and `user_games`. One genre can classify many games. Users and games
> have a many-to-many relationship, so the junction table stores saved games,
> ratings, and play counts.”

Point to the four-table schema in the project README or slides.

### 1:00–2:00 — Browse and search

Open the **Game catalog** tab. Search a title such as `Portal`, clear the
search, then filter by a genre. Explain that game links point to external pages;
the project catalog does not host game files. Use **Sort by title (Java)** to
show the custom insertion-sort algorithm.

### 2:00–3:00 — JOIN and aggregate reports

Open **Library & reports**. Show the **Games & players (JOIN)** report, then
the **User statistics (GROUP BY)** report.

> “The first report joins the junction table to games and users. The second
> groups rows by user and calculates saved-game count, total plays, and average
> rating using `COUNT`, `SUM`, and `AVG`.”

### 3:00–4:00 — CRUD and input checks

In **Users**, add a temporary user with a unique username and email, edit the
username, then delete that same user. Confirm the deletion when prompted.

In **Game catalog**, add a temporary game with a valid HTTP/HTTPS link and date
in `YYYY-MM-DD` format; edit it and delete it. Point out the validation dialog
if a required field or date is invalid.

### 4:00–5:00 — Save a library entry and close

Use **Add game to a user's library** to add a game not already saved by that
user, with a rating and play count. Refresh the reports and show the new row.

> “The UI uses prepared statements through the DAO classes. That keeps SQL
> separate from the interface and avoids inserting user input directly into
> query text.”

## Reminder

Make sure the Swing window is visible in VNC before class. The database is
stored locally in `game-library.db`; keep that file if you want to preserve
changes between runs.
