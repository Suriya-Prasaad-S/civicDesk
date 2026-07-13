package com.civicdesk.grievance.enums;

public enum UserStatus {
    ACT("A"),
    INA("I"),
    SUS("S");

    private final String label;

    UserStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static String normalize(String input) {
        if (input == null) {
            return null;
        }
        return switch (input.trim().toUpperCase()) {
            case "A", "ACT", "ACTIVE" -> ACT.label;
            case "I", "INA", "INACTIVE" -> INA.label;
            case "S", "SUS", "SUSPENDED" -> SUS.label;
            default -> null;
        };
    }
}
