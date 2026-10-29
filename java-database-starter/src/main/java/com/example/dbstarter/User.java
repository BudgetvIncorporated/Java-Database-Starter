package com.example.dbstarter;

public record User(int id, String username, String email) {
    public User {
        if (id < 0) {
            throw new IllegalArgumentException("User ID cannot be negative.");
        }
        username = CatalogValidation.requireText(username, "Username");
        email = CatalogValidation.requireEmail(email);
    }
}
