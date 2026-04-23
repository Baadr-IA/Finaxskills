package com.finaxys.skillsrh.security.permission;

import java.util.Objects;
import java.util.Set;

public record PermissionContext(
    String currentUserId,
    String resourceOwnerUserId,
    Set<String> currentUserTeamIds,
    Set<String> resourceTeamIds,
    String currentUserOrgId,
    String resourceOrgId
) {

    public PermissionContext {
        currentUserTeamIds = currentUserTeamIds == null ? Set.of() : Set.copyOf(currentUserTeamIds);
        resourceTeamIds = resourceTeamIds == null ? Set.of() : Set.copyOf(resourceTeamIds);
    }

    public static PermissionContext all() {
        return new PermissionContext(null, null, Set.of(), Set.of(), null, null);
    }

    public static PermissionContext self(String currentUserId, String resourceOwnerUserId) {
        return new PermissionContext(currentUserId, resourceOwnerUserId, Set.of(), Set.of(), null, null);
    }

    public boolean isSelf() {
        return currentUserId != null && currentUserId.equals(resourceOwnerUserId);
    }

    public boolean isTeamMatch() {
        return !currentUserTeamIds.isEmpty() && !resourceTeamIds.isEmpty() &&
            currentUserTeamIds.stream().anyMatch(resourceTeamIds::contains);
    }

    public boolean isOrgMatch() {
        return currentUserOrgId != null && Objects.equals(currentUserOrgId, resourceOrgId);
    }
}

