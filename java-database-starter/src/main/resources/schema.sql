-- Game Library schema
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

CREATE TABLE IF NOT EXISTS genres (
    genre_id INTEGER PRIMARY KEY,
    name TEXT NOT NULL UNIQUE
        CHECK (length(trim(name)) > 0)
);

-- Junction table: a game can have many genres, and a genre can describe many games.
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

CREATE INDEX IF NOT EXISTS idx_games_published_on
    ON games (published_on);

CREATE INDEX IF NOT EXISTS idx_games_developer_id
    ON games (developer_id);

CREATE INDEX IF NOT EXISTS idx_game_genres_genre_id
    ON game_genres (genre_id);
