package com.ceo.trading_platform_backend.uml_objects;

public enum Role {
    CLIENT("Client - Self-directed investor with personal portfolio"),
    ANALYST("Analyst - Reports on trading volumes and client activity"),
    ADMIN("Admin - System oversight and trade accountability");

    private final String description;

    Role(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public static Role fromString(String roleString) {
        try {
            return Role.valueOf(roleString.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid role: " + roleString);
        }
    }
}
