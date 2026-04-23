package com.finaxys.skillsrh.security.permission;

import jakarta.annotation.PostConstruct;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class JsonPermissionProfileRepository implements PermissionProfileRepository {

    private final ObjectMapper objectMapper;
    private final ResourceLoader resourceLoader;
    private final PermissionProfilesProperties permissionProfilesProperties;

    private volatile Map<String, PermissionProfile> profilesByKey = Map.of();

    public JsonPermissionProfileRepository(
        ObjectMapper objectMapper,
        ResourceLoader resourceLoader,
        PermissionProfilesProperties permissionProfilesProperties
    ) {
        this.objectMapper = objectMapper;
        this.resourceLoader = resourceLoader;
        this.permissionProfilesProperties = permissionProfilesProperties;
    }

    @PostConstruct
    void loadProfiles() {
        Resource resource = resourceLoader.getResource(permissionProfilesProperties.getFile());
        if (!resource.exists()) {
            throw new IllegalStateException("Permission profiles file not found: " + permissionProfilesProperties.getFile());
        }

        try (InputStream inputStream = resource.getInputStream()) {
            List<PermissionProfile> profiles = objectMapper.readValue(inputStream, new TypeReference<>() {});
            profilesByKey = profiles.stream()
                .peek(this::validateProfile)
                .collect(Collectors.toUnmodifiableMap(
                    profile -> profile.key().toLowerCase(Locale.ROOT),
                    Function.identity(),
                    (left, right) -> {
                        throw new IllegalStateException("Duplicate permission profile key: " + left.key());
                    }
                ));
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load permission profiles from JSON", exception);
        }
    }

    @Override
    public Map<String, PermissionProfile> findAllByKey() {
        return profilesByKey;
    }

    private void validateProfile(PermissionProfile profile) {
        if (!StringUtils.hasText(profile.key())) {
            throw new IllegalStateException("Permission profile key cannot be blank");
        }
    }
}

