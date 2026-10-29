package com.example.dbstarter;

public record Developer(int id, String name, String websiteUrl) {
    public Developer {
        if (id < 0) {
            throw new IllegalArgumentException("Developer ID cannot be negative.");
        }
        name = CatalogValidation.requireText(name, "Developer name");
        websiteUrl = CatalogValidation.optionalWebUrl(websiteUrl, "Developer website");
    }
}
