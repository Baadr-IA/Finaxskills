package com.finaxys.skillsrh.security.permission;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PermissionEvaluator {

    private final PermissionProfileRepository permissionProfileRepository;

    public PermissionEvaluator(PermissionProfileRepository permissionProfileRepository) {
        this.permissionProfileRepository = permissionProfileRepository;
    }

    public boolean evaluatePermission(
        Authentication authentication,
        ResourceKey resource,
        Action action,
        Scope requiredScope,
        PermissionContext context
    ) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        PermissionContext resolvedContext = context == null ? PermissionContext.all() : context;
        if (!matchesContext(requiredScope, resolvedContext)) {
            return false;
        }

        return resolveEffectivePermissions(authentication).stream()
            .anyMatch(grant -> grant.resource() == resource
                && grant.actions().contains(action)
                && grant.scope().covers(requiredScope));
    }

    public Set<String> resolveProfileKeys(Authentication authentication) {
        if (authentication == null || authentication.getAuthorities() == null) {
            return Set.of();
        }

        return authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .filter(authority -> authority.startsWith("ROLE_"))
            .map(authority -> authority.substring("ROLE_".length()).toLowerCase(Locale.ROOT))
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public List<PermissionGrant> resolveEffectivePermissions(Authentication authentication) {
        Map<String, PermissionProfile> profilesByKey = permissionProfileRepository.findAllByKey();

        return resolveProfileKeys(authentication).stream()
            .map(profilesByKey::get)
            .filter(Objects::nonNull)
            .flatMap(profile -> profile.permissions().stream())
            .distinct()
            .toList();
    }

    private boolean matchesContext(Scope requiredScope, PermissionContext context) {
        return switch (requiredScope) {
            case ALL -> true;
            case ORG -> context.isOrgMatch();
            case TEAM -> context.isTeamMatch();
            case SELF -> context.isSelf();
        };
    }
}

