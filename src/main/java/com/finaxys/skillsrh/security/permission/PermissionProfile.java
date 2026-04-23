package com.finaxys.skillsrh.security.permission;

import java.util.List;
import java.util.Objects;

public record PermissionProfile(String key, String label, List<PermissionGrant> permissions) {

    public PermissionProfile {
        key = Objects.requireNonNullElse(key, "");
        label = Objects.requireNonNullElse(label, key);
        permissions = permissions == null ? List.of() : List.copyOf(permissions);
    }
}

