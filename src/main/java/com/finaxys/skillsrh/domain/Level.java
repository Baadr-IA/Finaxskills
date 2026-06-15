package com.finaxys.skillsrh.domain;

public enum Level {
    NIVEAU_0("N/A"),
    NIVEAU_1("niveau 1"),
    NIVEAU_2("niveau 2"),
    NIVEAU_3("niveau 3"),
    NIVEAU_4("niveau 4");

    private final String label;

    Level(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static Level from(String value) {
        if (value == null) return null;
        String v = value.trim();
        // accept either label ("niveau 1") or enum name ("NIVEAU_1") or numeric ("1")
        for (Level l : values()) {
            if (l.name().equalsIgnoreCase(v) || l.label.equalsIgnoreCase(v)) return l;
        }
        if (v.matches("^[1-4]$")) {
            int idx = Integer.parseInt(v);
            return values()[idx - 1];
        }
        throw new IllegalArgumentException("Unknown level: " + value);
    }
}

