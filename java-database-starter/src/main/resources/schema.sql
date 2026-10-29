-- Game library schema based on the approved ER diagram.
-- Run this file before seed.sql.

PRAGMA foreign_keys = ON;

CREATE TABLE IF NOT EXISTS developers (
    developer_id INTEGER PRIMARY KEY,
    name TEXT NOT NULL UNIQUE
        CHECK (length(trim(name)) > 0),
    website_url TEXT
        CHECK (
            website_url IS NULL
            OR website_url LIKE 'https://%'
            OR website_url LIKE 'http://%'
        )
);

CREATE TABLE IF NOT EXISTS genres (
    genre_id INTEGER PRIMARY KEY,
    genre_name TEXT NOT NULL UNIQUE
        CHECK (length(trim(genre_name)) > 0)
);

CREATE TABLE IF NOT EXISTS users (
    user_id INTEGER PRIMARY KEY,
    username TEXT NOT NULL UNIQUE
        CHECK (length(trim(username)) > 0),
    email TEXT NOT NULL UNIQUE
        CHECK (instr(email, '@') > 1)
);

CREATE TABLE IF NOT EXISTS games (
    game_id INTEGER PRIMARY KEY,
    title TEXT NOT NULL
        CHECK (length(trim(title)) > 0),
    game_url TEXT NOT NULL
        CHECK (
            game_url LIKE 'https://%'
            OR game_url LIKE 'http://%'
        ),
    published_on TEXT NOT NULL
        CHECK (
            date(published_on) IS NOT NULL
            AND date(published_on) = published_on
        ),
    developer_id INTEGER NOT NULL,
    UNIQUE (title, published_on),
    FOREIGN KEY (developer_id)
        REFERENCES developers (developer_id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT
);

-- Games and genres form a many-to-many relationship.
CREATE TABLE IF NOT EXISTS game_genres (
    game_id INTEGER NOT NULL,
    genre_id INTEGER NOT NULL,
    PRIMARY KEY (game_id, genre_id),
    FOREIGN KEY (game_id)
        REFERENCES games (game_id)
        ON UPDATE CASCADE
        ON DELETE CASCADE,
    FOREIGN KEY (genre_id)
        REFERENCES genres (genre_id)
        ON UPDATE CASCADE
        ON DELETE CASCADE
);

-- This junction table connects users to the games in their libraries.
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

CREATE INDEX IF NOT EXISTS idx_games_published_on
    ON games (published_on);

CREATE INDEX IF NOT EXISTS idx_games_developer_id
    ON games (developer_id);

CREATE INDEX IF NOT EXISTS idx_game_genres_genre_id
    ON game_genres (genre_id);

CREATE INDEX IF NOT EXISTS idx_user_games_game_id
    ON user_games (game_id);
