package com.example.dbstarter;

public record Game(int id, int genreId, String title, String url,
                   String releaseDate, String genreName) {
    public Game {
        if (id < 0) {
            throw new IllegalArgumentException("Game ID cannot be negative.");
        }
        if (genreId < 1) {
            throw new IllegalArgumentException("Choose a genre.");
        }
        title = CatalogValidation.requireText(title, "Title");
        url = CatalogValidation.requireWebUrl(url, "Game URL");
        releaseDate = CatalogValidation.requireIsoDate(releaseDate);
        genreName = genreName == null ? "" : genreName.trim();
    }
}
