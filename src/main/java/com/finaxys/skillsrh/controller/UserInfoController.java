package com.finaxys.skillsrh.controller;

import com.finaxys.skillsrh.security.permission.Action;
import com.finaxys.skillsrh.security.permission.PermissionEvaluator;
import com.finaxys.skillsrh.security.permission.PermissionGrant;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

@RestController
@RequestMapping("/api")
public class UserInfoController {

    private final PermissionEvaluator permissionEvaluator;

    public UserInfoController(PermissionEvaluator permissionEvaluator) {
        this.permissionEvaluator = permissionEvaluator;
    }

    @GetMapping("/me")
    public UserInfoResponse me(Authentication authentication) {
        List<String> profileKeys = permissionEvaluator.resolveProfileKeys(authentication).stream()
            .sorted()
            .toList();

        List<PermissionView> permissions = mergePermissions(permissionEvaluator.resolveEffectivePermissions(authentication));

        return new UserInfoResponse(authentication.getName(), profileKeys, permissions);
    }

    private List<PermissionView> mergePermissions(List<PermissionGrant> grants) {
        Map<String, MutablePermissionBucket> buckets = new LinkedHashMap<>();

        grants.stream()
            .sorted(Comparator
                .comparing((PermissionGrant grant) -> grant.resource().name())
                .thenComparing(grant -> grant.scope().name()))
            .forEach(grant -> {
                String key = grant.resource().name() + "|" + grant.scope().name();
                MutablePermissionBucket bucket = buckets.computeIfAbsent(
                    key,
                    ignored -> new MutablePermissionBucket(grant.resource().name(), grant.scope().name())
                );

                grant.actions().stream()
                    .map(Action::name)
                    .forEach(bucket.actions::add);
            });

        return buckets.values().stream()
            .map(bucket -> new PermissionView(bucket.resource, List.copyOf(bucket.actions), bucket.scope))
            .toList();
    }

    public record UserInfoResponse(String username, List<String> profileKeys, List<PermissionView> permissions) {
    }

    public record PermissionView(String resource, List<String> actions, String scope) {
    }

    private static final class MutablePermissionBucket {
        private final String resource;
        private final String scope;
        private final Set<String> actions = new TreeSet<>();

        private MutablePermissionBucket(String resource, String scope) {
            this.resource = resource;
            this.scope = scope;
        }
    }
}

