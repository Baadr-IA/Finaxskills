package com.finaxys.skillsrh.security.permission;

import java.util.Set;

public record PermissionGrant(ResourceKey resource, Set<Action> actions, Scope scope) {

    public PermissionGrant {
        actions = actions == null ? Set.of() : Set.copyOf(actions);
    }
}

