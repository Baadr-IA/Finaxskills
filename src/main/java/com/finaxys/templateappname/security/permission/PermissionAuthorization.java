package com.finaxys.templateappname.security.permission;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.Optional;

@Component("permissions")
public class PermissionAuthorization {

    private final PermissionEvaluator permissionEvaluator;

    public PermissionAuthorization(PermissionEvaluator permissionEvaluator) {
        this.permissionEvaluator = permissionEvaluator;
    }

    public boolean has(Authentication authentication, String resource, String action, String scope) {
        Optional<ResourceKey> resourceKey = parseResource(resource);
        Optional<Action> requestedAction = parseAction(action);
        Optional<Scope> requestedScope = parseScope(scope);

        if (resourceKey.isEmpty() || requestedAction.isEmpty() || requestedScope.isEmpty()) {
            return false;
        }

        return permissionEvaluator.evaluatePermission(
            authentication,
            resourceKey.get(),
            requestedAction.get(),
            requestedScope.get(),
            PermissionContext.all()
        );
    }

    public boolean hasSelf(Authentication authentication, String resource, String action, String ownerUserId) {
        Optional<ResourceKey> resourceKey = parseResource(resource);
        Optional<Action> requestedAction = parseAction(action);
        if (resourceKey.isEmpty() || requestedAction.isEmpty() || !StringUtils.hasText(ownerUserId)) {
            return false;
        }

        String currentUserId = extractCurrentUserId(authentication);
        if (!StringUtils.hasText(currentUserId)) {
            return false;
        }

        return permissionEvaluator.evaluatePermission(
            authentication,
            resourceKey.get(),
            requestedAction.get(),
            Scope.SELF,
            PermissionContext.self(currentUserId, ownerUserId)
        );
    }

    private Optional<ResourceKey> parseResource(String resource) {
        return parseEnum(resource, ResourceKey.class);
    }

    private Optional<Action> parseAction(String action) {
        return parseEnum(action, Action.class);
    }

    private Optional<Scope> parseScope(String scope) {
        return parseEnum(scope, Scope.class);
    }

    private <E extends Enum<E>> Optional<E> parseEnum(String value, Class<E> enumType) {
        if (!StringUtils.hasText(value)) {
            return Optional.empty();
        }

        try {
            return Optional.of(Enum.valueOf(enumType, value.toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    private String extractCurrentUserId(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwtAuthenticationToken) {
            return jwtAuthenticationToken.getToken().getSubject();
        }
        return authentication != null ? authentication.getName() : null;
    }
}
