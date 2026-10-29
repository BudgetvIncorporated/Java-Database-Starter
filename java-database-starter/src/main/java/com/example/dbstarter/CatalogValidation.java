package com.example.dbstarter;

import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

final class CatalogValidation {
    private CatalogValidation() {
    }

    static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be empty.");
        }
        return value.trim();
    }

    static String requireWebUrl(String value, String fieldName) {
        String normalized = requireText(value, fieldName);
        try {
            URI uri = URI.create(normalized);
            String scheme = uri.getScheme();
            if (uri.getHost() == null || scheme == null
                    || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
                throw new IllegalArgumentException(
                        fieldName + " must be a valid http:// or https:// address.");
            }
        } catch (IllegalArgumentException exception) {
            if (exception.getMessage() != null && exception.getMessage().startsWith(fieldName)) {
                throw exception;
            }
            throw new IllegalArgumentException(
                    fieldName + " must be a valid http:// or https:// address.");
        }
        return normalized;
    }

    static String optionalWebUrl(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return requireWebUrl(value, fieldName);
    }

    static String requireIsoDate(String value) {
        String normalized = requireText(value, "Publication date");
        try {
            LocalDate date = LocalDate.parse(normalized);
            if (!date.toString().equals(normalized)) {
                throw new IllegalArgumentException("Use a date in YYYY-MM-DD format.");
            }
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("Use a valid date in YYYY-MM-DD format.");
        }
        return normalized;
    }
}
