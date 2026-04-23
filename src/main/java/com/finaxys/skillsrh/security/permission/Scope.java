package com.finaxys.skillsrh.security.permission;

public enum Scope {
    SELF(1),
    TEAM(2),
    ORG(3),
    ALL(4);

    private final int rank;

    Scope(int rank) {
        this.rank = rank;
    }

    public boolean covers(Scope requiredScope) {
        return this.rank >= requiredScope.rank;
    }
}

