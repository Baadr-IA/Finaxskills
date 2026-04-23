package com.finaxys.skillsrh.security.permission;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PermissionEvaluatorTest {

    private final PermissionProfileRepository repository = () -> Map.of(
        "self_profile", new PermissionProfile(
            "self_profile",
            "Self Profile",
            List.of(new PermissionGrant(ResourceKey.COLLABORATORS, Set.of(Action.READ), Scope.SELF))
        ),
        "team_profile", new PermissionProfile(
            "team_profile",
            "Team Profile",
            List.of(new PermissionGrant(ResourceKey.COLLABORATORS, Set.of(Action.CREATE), Scope.TEAM))
        ),
        "org_profile", new PermissionProfile(
            "org_profile",
            "Org Profile",
            List.of(new PermissionGrant(ResourceKey.COLLABORATORS, Set.of(Action.UPDATE), Scope.ORG))
        ),
        "all_profile", new PermissionProfile(
            "all_profile",
            "All Profile",
            List.of(new PermissionGrant(ResourceKey.COLLABORATORS, Set.of(Action.DELETE), Scope.ALL))
        )
    );

    private final PermissionEvaluator permissionEvaluator = new PermissionEvaluator(repository);

    @Test
    void selfScopeAllowsOnlyOwner() {
        Authentication auth = authenticationWithProfile("self_profile");

        boolean allowed = permissionEvaluator.evaluatePermission(
            auth,
            ResourceKey.COLLABORATORS,
            Action.READ,
            Scope.SELF,
            PermissionContext.self("user-1", "user-1")
        );

        boolean denied = permissionEvaluator.evaluatePermission(
            auth,
            ResourceKey.COLLABORATORS,
            Action.READ,
            Scope.SELF,
            PermissionContext.self("user-1", "user-2")
        );

        assertThat(allowed).isTrue();
        assertThat(denied).isFalse();
    }

    @Test
    void teamScopeRequiresTeamIntersection() {
        Authentication auth = authenticationWithProfile("team_profile");

        PermissionContext allowedContext = new PermissionContext(
            "user-1",
            null,
            Set.of("team-a"),
            Set.of("team-a", "team-b"),
            null,
            null
        );

        PermissionContext deniedContext = new PermissionContext(
            "user-1",
            null,
            Set.of("team-c"),
            Set.of("team-a"),
            null,
            null
        );

        assertThat(permissionEvaluator.evaluatePermission(
            auth,
            ResourceKey.COLLABORATORS,
            Action.CREATE,
            Scope.TEAM,
            allowedContext
        )).isTrue();

        assertThat(permissionEvaluator.evaluatePermission(
            auth,
            ResourceKey.COLLABORATORS,
            Action.CREATE,
            Scope.TEAM,
            deniedContext
        )).isFalse();
    }

    @Test
    void orgScopeRequiresSameOrganization() {
        Authentication auth = authenticationWithProfile("org_profile");

        PermissionContext context = new PermissionContext(
            "user-1",
            null,
            Set.of(),
            Set.of(),
            "org-42",
            "org-42"
        );

        assertThat(permissionEvaluator.evaluatePermission(
            auth,
            ResourceKey.COLLABORATORS,
            Action.UPDATE,
            Scope.ORG,
            context
        )).isTrue();
    }

    @Test
    void allScopeDoesNotNeedBusinessContext() {
        Authentication auth = authenticationWithProfile("all_profile");

        assertThat(permissionEvaluator.evaluatePermission(
            auth,
            ResourceKey.COLLABORATORS,
            Action.DELETE,
            Scope.ALL,
            PermissionContext.all()
        )).isTrue();
    }

    private Authentication authenticationWithProfile(String profileKey) {
        return new TestingAuthenticationToken(
            "user-1",
            "n/a",
            "ROLE_" + profileKey.toUpperCase(Locale.ROOT)
        );
    }
}

