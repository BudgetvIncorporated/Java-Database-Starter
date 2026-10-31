package com.example.dbstarter;

import java.util.ArrayList;
import java.util.List;

public final class GameAlgorithms {
    private GameAlgorithms() {
    }

    public static List<Game> sortByTitle(List<Game> games) {
        List<Game> sorted = new ArrayList<>(games);

        // Insertion sort demonstrates a Java-side algorithm instead of SQL sorting.
        for (int index = 1; index < sorted.size(); index++) {
            Game current = sorted.get(index);
            int previous = index - 1;

            while (previous >= 0 && compareTitles(sorted.get(previous), current) > 0) {
                sorted.set(previous + 1, sorted.get(previous));
                previous--;
            }
            sorted.set(previous + 1, current);
        }

        return List.copyOf(sorted);
    }

    private static int compareTitles(Game first, Game second) {
        int caseInsensitive = String.CASE_INSENSITIVE_ORDER.compare(
                first.title(), second.title());
        return caseInsensitive != 0
                ? caseInsensitive
                : first.title().compareTo(second.title());
    }
}
