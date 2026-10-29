package com.example.dbstarter;

public record Genre(int id, String name) {
    public Genre {
        if (id < 1) {
            throw new IllegalArgumentException("Genre ID must be positive.");
        }
        name = CatalogValidation.requireText(name, "Genre name");
    }
}
