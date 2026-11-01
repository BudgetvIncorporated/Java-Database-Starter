package com.example.dbstarter;

public record UserLibraryStats(
        String username,
        int savedGames,
        int totalPlayCount,
        Double averageRating) {
}
