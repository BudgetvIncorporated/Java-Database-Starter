package com.example.dbstarter;

public record SavedGameEntry(
        int userId,
        int gameId,
        String username,
        String gameTitle,
        Integer rating,
        int playCount) {
}
