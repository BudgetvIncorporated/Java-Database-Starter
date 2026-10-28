package com.example.dbstarter;

public record UserGameSummary(
        int userId,
        String username,
        int gameCount,
        double averageRating,
        int totalPlayCount) {
}
