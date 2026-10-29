-- Four-table game library schema matching the approved ER diagram.
-- Run this file before seed.sql.

PRAGMA foreign_keys = ON;

-- Parent table: a genre can be assigned to many games.
CREATE TABLE IF NOT EXISTS genres (
    genre_id INTEGER PRIMARY KEY,
    genre_name TEXT NOT NULL UNIQUE
        CHECK (length(trim(genre_name)) > 0)
);

-- Parent table: a user can save many games.
CREATE TABLE IF NOT EXISTS users (
    user_id INTEGER PRIMARY KEY,
    username TEXT NOT NULL UNIQUE
        CHECK (length(trim(username)) > 0),
    email TEXT NOT NULL UNIQUE
        CHECK (instr(email, '@') > 1)
);

-- Each game belongs to one existing genre.
CREATE TABLE IF NOT EXISTS games (
    game_id INTEGER PRIMARY KEY,
    genre_id INTEGER NOT NULL,
    title TEXT NOT NULL
        CHECK (length(trim(title)) > 0),
    launch_url TEXT NOT NULL
        CHECK (
            launch_url LIKE 'https://%'
            OR launch_url LIKE 'http://%'
        ),
    release_date TEXT NOT NULL
        CHECK (
            date(release_date) IS NOT NULL
            AND date(release_date) = release_date
        ),
    UNIQUE (title, release_date),
    FOREIGN KEY (genre_id)
        REFERENCES genres (genre_id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT
);

-- Junction table: users can save many games, and games can belong to many users.
CREATE TABLE IF NOT EXISTS user_games (
    user_id INTEGER NOT NULL,
    game_id INTEGER NOT NULL,
    rating INTEGER
        CHECK (rating IS NULL OR rating BETWEEN 1 AND 5),
    play_count INTEGER NOT NULL DEFAULT 0
        CHECK (play_count >= 0),
    PRIMARY KEY (user_id, game_id),
    FOREIGN KEY (user_id)
        REFERENCES users (user_id)
        ON UPDATE CASCADE
        ON DELETE CASCADE,
    FOREIGN KEY (game_id)
        REFERENCES games (game_id)
        ON UPDATE CASCADE
        ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_games_genre_id
    ON games (genre_id);

CREATE INDEX IF NOT EXISTS idx_user_games_game_id
    ON user_games (game_id);
