package com.finaxys.skillsrh.domain;

public enum Status {
    EN_ATTENTE("en attente"),
    EN_COURS("en cours"),
    COMPLETE("completé");

    private final String label;

    Status(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static Status fromLabel(String label) {
        if (label == null) return null;
        String normalized = label.trim();
        for (Status s : values()) {
            if (s.label.equalsIgnoreCase(normalized) || s.name().equalsIgnoreCase(normalized)) {
                return s;
            }
        }
        throw new IllegalArgumentException("Unknown status label: " + label);
    }
}

