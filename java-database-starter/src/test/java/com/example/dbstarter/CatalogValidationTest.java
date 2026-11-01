package com.example.dbstarter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

public class CatalogValidationTest {
    @Test
    public void requiredTextRejectsBlankAndTrimsValidText() {
        assertEquals("Puzzle", CatalogValidation.requireText("  Puzzle  ", "Genre"));
        assertThrows(IllegalArgumentException.class,
                () -> CatalogValidation.requireText("   ", "Genre"));
    }

    @Test
    public void emailRequiresLocalPartAndDomain() {
        assertEquals("player@example.com",
                CatalogValidation.requireEmail(" player@example.com "));
        assertThrows(IllegalArgumentException.class,
                () -> CatalogValidation.requireEmail("not-an-email"));
        assertThrows(IllegalArgumentException.class,
                () -> CatalogValidation.requireEmail("player @example.com"));
    }

    @Test
    public void webLinkRequiresHttpOrHttpsAndAHost() {
        assertEquals("https://example.com/game",
                CatalogValidation.requireWebUrl(" https://example.com/game ", "Game link"));
        assertThrows(IllegalArgumentException.class,
                () -> CatalogValidation.requireWebUrl("ftp://example.com/game", "Game link"));
        assertThrows(IllegalArgumentException.class,
                () -> CatalogValidation.requireWebUrl("https:///game", "Game link"));
        assertThrows(IllegalArgumentException.class,
                () -> CatalogValidation.requireWebUrl("not a URL", "Game link"));
    }

    @Test
    public void dateMustBeARealIsoCalendarDate() {
        assertEquals("2024-02-29", CatalogValidation.requireIsoDate("2024-02-29"));
        assertThrows(IllegalArgumentException.class,
                () -> CatalogValidation.requireIsoDate("2025-02-29"));
        assertThrows(IllegalArgumentException.class,
                () -> CatalogValidation.requireIsoDate("02/29/2024"));
    }
}
