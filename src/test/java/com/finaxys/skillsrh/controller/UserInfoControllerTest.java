package com.finaxys.skillsrh.controller;

import com.finaxys.skillsrh.config.DataInitializer;
import com.finaxys.skillsrh.domain.Collaborator;
import com.finaxys.skillsrh.repository.CollaboratorRepository;
import com.finaxys.skillsrh.security.permission.Action;
import com.finaxys.skillsrh.security.permission.PermissionGrant;
import com.finaxys.skillsrh.security.permission.PermissionProfile;
import com.finaxys.skillsrh.security.permission.PermissionProfileRepository;
import com.finaxys.skillsrh.security.permission.ResourceKey;
import com.finaxys.skillsrh.security.permission.Scope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "app.security.oauth2.issuer-uri=https://example.test/realms/skills-rh",
    "app.security.oauth2.jwk-set-uri=https://example.test/realms/skills-rh/protocol/openid-connect/certs",
    "app.security.oauth2.audience=skills-rh-api",
    "app.security.oauth2.api-client-id=skills-rh-api",
    "app.security.cors.allowed-origins[0]=http://localhost:4200"
})
class UserInfoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DataInitializer dataInitializer;

    @MockitoBean
    private CollaboratorRepository collaboratorRepository;

    @MockitoBean
    private PermissionProfileRepository permissionProfileRepository;

    @BeforeEach
    void setUp() {
        when(permissionProfileRepository.findAllByKey()).thenReturn(Map.of(
            "guest", new PermissionProfile("guest", "Guest", List.of()),
            "hr", new PermissionProfile(
                "hr",
                "HR",
                List.of(new PermissionGrant(ResourceKey.COLLABORATORS, Set.of(Action.READ), Scope.ALL))
            )
        ));
    }

    @Test
    void meCreatesCollaboratorWhenMissing() throws Exception {
        when(collaboratorRepository.findByKeycloakId("kc-user-123")).thenReturn(Optional.empty());
        when(collaboratorRepository.save(any(Collaborator.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(get("/api/me")
                .with(jwt().jwt(token -> token
                    .subject("kc-user-123")
                    .claim("preferred_username", "alice")
                    .claim("given_name", "Alice")
                    .claim("family_name", "Martin")
                    .claim("email", "alice.martin@finaxys.com"))
                    .authorities(new SimpleGrantedAuthority("ROLE_GUEST"))))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith("application/json"))
            .andExpect(jsonPath("$.permissions").isArray());

        verify(collaboratorRepository).save(argThat(collaborator ->
            "kc-user-123".equals(collaborator.getKeycloakId())
        ));
    }
}
