package com.example.dbstarter;

public record UserGame(int userId, int gameId, Integer rating, int playCount) {
    public UserGame {
        if (userId < 1) {
            throw new IllegalArgumentException("Choose a valid user.");
        }
        if (gameId < 1) {
            throw new IllegalArgumentException("Choose a valid game.");
        }
        if (rating != null && (rating < 1 || rating > 5)) {
            throw new IllegalArgumentException("Rating must be from 1 to 5.");
        }
        if (playCount < 0) {
            throw new IllegalArgumentException("Play count cannot be negative.");
        }
    }
}
