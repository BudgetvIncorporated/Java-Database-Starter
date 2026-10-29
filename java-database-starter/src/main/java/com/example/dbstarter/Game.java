package com.example.dbstarter;

public record Game(int id, String title, String url, String publishedOn,
                   int developerId, String developerName, String genres) {
    public Game {
        if (id < 0) {
            throw new IllegalArgumentException("Game ID cannot be negative.");
        }
        title = CatalogValidation.requireText(title, "Title");
        url = CatalogValidation.requireWebUrl(url, "Game URL");
        publishedOn = CatalogValidation.requireIsoDate(publishedOn);
        if (developerId < 1) {
            throw new IllegalArgumentException("Choose a valid developer.");
        }
        developerName = developerName == null ? "" : developerName;
        genres = genres == null ? "" : genres;
    }

    public String releaseDate() {
        return publishedOn;
    }

    public String genreName() {
        return genres;
    }
}
