package com.finaxys.skillsrh.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class KeycloakAdminService {

    public String getLastError() { return lastErrorMessage; }

    private static final Logger logger = LoggerFactory.getLogger(KeycloakAdminService.class);

    private final RestTemplate rest = new RestTemplate();

    @Value("${app.security.keycloak-admin.token-url:}")
    private String tokenUrl;

    @Value("${app.security.keycloak-admin.users-url:}")
    private String usersUrl;

    @Value("${app.security.keycloak-admin.client-id:admin-cli}")
    private String clientId;

    @Value("${app.security.keycloak-admin.username:}")
    private String adminUsername;

    @Value("${app.security.keycloak-admin.password:}")
    private String adminPassword;

    // last error message for debugging (best-effort; overwritten by concurrent calls)
    private volatile String lastErrorMessage = null;

    private String getAdminToken() {
        // provide sensible defaults if not configured
        if (tokenUrl == null || tokenUrl.isBlank()) {
            tokenUrl = "https://awskeycloak.lab-finaxys.net/realms/master/protocol/openid-connect/token";
            logger.debug("Using default Keycloak token URL: {}", tokenUrl);
        }
        if (usersUrl == null || usersUrl.isBlank()) {
            usersUrl = "https://awskeycloak.lab-finaxys.net/admin/realms/Template-app-Java-angular/users";
            logger.debug("Using default Keycloak users URL: {}", usersUrl);
        }
        if (adminUsername == null || adminUsername.isBlank()) {
            adminUsername = "temp-admin";
            logger.debug("Using default Keycloak admin username: {}", adminUsername);
        }
        if (adminPassword == null || adminPassword.isBlank()) {
            adminPassword = "admin";
            logger.debug("Using default Keycloak admin password from config");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "password");
        body.add("client_id", clientId);
        body.add("username", adminUsername);
        body.add("password", adminPassword);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
        try {
            ResponseEntity<Map> resp = rest.postForEntity(tokenUrl, request, Map.class);
            logger.debug("Keycloak token endpoint responded: {}", resp.getStatusCode());
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                Object token = resp.getBody().get("access_token");
                String t = token != null ? token.toString() : null;
                if (t == null) lastErrorMessage = "No access_token in token response";
                return t;
            } else {
                lastErrorMessage = "Token request returned status " + resp.getStatusCode();
            }
        } catch (Exception e) {
            logger.warn("Failed to get admin token from Keycloak", e);
            lastErrorMessage = "Token request failed: " + e.getMessage();
        }
        return null;
    }

    public String createUserIfNotExists(String email, String firstName, String lastName) {
        String token = getAdminToken();
        if (token == null) return createUserIfNotExistsFallbackSearch(null, email);
        String created = createUserIfNotExistsWithBearer(token, email, firstName, lastName);
        if (created != null) return created;
        return createUserIfNotExistsFallbackSearch(token, email);
    }

    /**
     * Create user (using admin bearer), set the user password via reset-password endpoint,
     * and assign the realm role 'collaborator'. Returns Keycloak id or null.
     */
    public String createUserAndSetPassword(String email, String firstName, String lastName, String password) {
        String adminToken = getAdminToken();
        if (adminToken == null) {
            logger.warn("Admin token not available to create user");
            lastErrorMessage = "Admin token not available";
            return null;
        }

        String userId = createUserIfNotExistsWithBearer(adminToken, email, firstName, lastName);
        if (userId == null) {
            // maybe already exists - try search
            userId = createUserIfNotExistsFallbackSearch(adminToken, email);
        }

        if (userId != null) {
            boolean ok = setPasswordForUser(adminToken, userId, password);
            if (!ok) {
                lastErrorMessage = "Failed to set password for user " + userId;
                logger.warn(lastErrorMessage);
            }
            // assign client role 'collaborator' to the client 'template-app-spring-api' (best-effort)
            try {
                boolean roleAssigned = addClientRoleToUser(adminToken, userId, "template-app-spring-api", "collaborator");
                if (!roleAssigned) {
                    logger.warn("Failed to assign client role collaborator to user {}: {}", userId, lastErrorMessage);
                }
            } catch (Exception e) {
                logger.warn("Exception while assigning role to user {}", userId, e);
            }
            return userId;
        }
        return null;
    }

    /**
     * Assign a client role to the user. Fetches the client ID first, then assigns the role.
     * Returns true if successful.
     */
    private boolean addClientRoleToUser(String bearerToken, String userId, String clientName, String roleName) {
        try {
            if (bearerToken == null) {
                lastErrorMessage = "No token available to assign role";
                return false;
            }
            // derive base admin URL from usersUrl (remove trailing /users)
            String baseAdmin = usersUrl;
            if (baseAdmin.endsWith("/users")) {
                baseAdmin = baseAdmin.substring(0, baseAdmin.length() - "/users".length());
            }
            
            // Step 1: Get client ID by client name
            String clientsUrl = baseAdmin + "/clients";
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(bearerToken);
            URI clientsUri = URI.create(clientsUrl + "?clientId=" + clientName);
            HttpEntity<Void> req = new HttpEntity<>(headers);
            ResponseEntity<List> clientsResp = rest.exchange(clientsUri, HttpMethod.GET, req, List.class);
            
            if (!(clientsResp.getStatusCode().is2xxSuccessful()) || clientsResp.getBody() == null || clientsResp.getBody().isEmpty()) {
                lastErrorMessage = "Client " + clientName + " not found";
                logger.warn("Client {} not found", clientName);
                return false;
            }
            
            Object firstClient = clientsResp.getBody().get(0);
            if (!(firstClient instanceof Map<?,?>)) {
                lastErrorMessage = "Invalid client response format";
                return false;
            }
            
            Map<?,?> clientMap = (Map<?,?>) firstClient;
            Object clientIdObj = clientMap.get("id");
            if (clientIdObj == null) {
                lastErrorMessage = "Client response has no id";
                return false;
            }
            String clientId = clientIdObj.toString();
            logger.debug("Found client {} with id {}", clientName, clientId);
            
            // Step 2: Get the role representation
            String roleUrl = baseAdmin + "/clients/" + clientId + "/roles/" + roleName;
            ResponseEntity<Map> roleResp = rest.exchange(roleUrl, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
            if (!(roleResp.getStatusCode().is2xxSuccessful()) || roleResp.getBody() == null) {
                lastErrorMessage = "Failed to fetch client role " + roleName + " status: " + roleResp.getStatusCode();
                logger.warn("Role {} not found for client {}", roleName, clientId);
                return false;
            }
            
            Map<?,?> roleMap = roleResp.getBody();
            Object roleIdObj = roleMap.get("id");
            if (roleIdObj == null) {
                lastErrorMessage = "Role response has no id";
                return false;
            }
            String roleId = roleIdObj.toString();
            logger.debug("Found role {} with id {}", roleName, roleId);
            
            // Step 3: Assign client role to user
            Map<String,Object> roleRep = Map.of("id", roleId, "name", roleName);
            List<Map<String,Object>> roles = List.of(roleRep);
            String mappingUrl = usersUrl + "/" + userId + "/role-mappings/clients/" + clientId;
            HttpEntity<List<Map<String,Object>>> postReq = new HttpEntity<>(roles, headers);
            ResponseEntity<Void> postResp = rest.exchange(mappingUrl, HttpMethod.POST, postReq, Void.class);
            
            if (postResp.getStatusCode().is2xxSuccessful() || postResp.getStatusCode() == HttpStatus.NO_CONTENT) {
                lastErrorMessage = null;
                logger.info("Successfully assigned client role {} to user {}", roleName, userId);
                return true;
            } else {
                lastErrorMessage = "Role mapping returned " + postResp.getStatusCode();
                logger.warn("Failed to assign role: {}", lastErrorMessage);
                return false;
            }
        } catch (Exception e) {
            logger.warn("Failed to assign client role {} to user {}", roleName, userId, e);
            lastErrorMessage = "assignClientRole exception: " + e.getMessage();
            return false;
        }
    }

    public String createUserIfNotExistsWithBearer(String bearerToken, String email, String firstName, String lastName) {
        try {
            if (bearerToken == null) {
                lastErrorMessage = "No bearer token provided";
                logger.debug("createUserIfNotExistsWithBearer: no bearer token");
                return null;
            }
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(bearerToken);

            Map<String, Object> payload = Map.of(
                "username", email,
                "email", email,
                "firstName", firstName != null ? firstName : "",
                "lastName", lastName != null ? lastName : "",
                "enabled", true
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
            ResponseEntity<Void> resp = rest.postForEntity(usersUrl, request, Void.class);
            logger.debug("createUserWithBearer: status {}", resp.getStatusCode());
            if (resp.getStatusCode() == HttpStatus.CREATED) {
                URI location = resp.getHeaders().getLocation();
                if (location != null) {
                    String path = location.getPath();
                    String id = path.substring(path.lastIndexOf('/') + 1);
                    lastErrorMessage = null;
                    return id;
                }
            } else {
                lastErrorMessage = "Create user returned status " + resp.getStatusCode();
            }
        } catch (HttpClientErrorException.Conflict e) {
            logger.info("User already exists: {}", email);
            lastErrorMessage = "User already exists (409)";
        } catch (HttpClientErrorException.Forbidden e) {
            logger.warn("Caller token not allowed to create users: {}", e.getMessage());
            lastErrorMessage = "Forbidden: caller token cannot create users";
            return null;
        } catch (Exception e) {
            logger.warn("Failed to create user with caller token", e);
            lastErrorMessage = "createWithBearer exception: " + e.getMessage();
        }
        return null;
    }

    private String createUserIfNotExistsFallbackSearch(String bearerToken, String email) {
        try {
            String token = bearerToken != null ? bearerToken : getAdminToken();
            if (token == null) {
                lastErrorMessage = "No token available to search user";
                logger.debug("createUserIfNotExistsFallbackSearch: no token");
                return null;
            }
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);

            URI uri = URI.create(usersUrl + "?email=" + email);
            logger.debug("Searching user by email: {} -> {}", email, uri);
            HttpEntity<Void> req = new HttpEntity<>(headers);
            ResponseEntity<List> resp = rest.exchange(uri, HttpMethod.GET, req, List.class);
            logger.debug("Search user response: {}", resp.getStatusCode());
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null && !resp.getBody().isEmpty()) {
                Object first = resp.getBody().get(0);
                if (first instanceof Map<?, ?> map && map.get("id") != null) {
                    lastErrorMessage = null;
                    return map.get("id").toString();
                }
            } else {
                lastErrorMessage = "Search returned status " + resp.getStatusCode();
            }
        } catch (Exception e) {
            logger.warn("Failed to search user by email", e);
            lastErrorMessage = "search exception: " + e.getMessage();
        }

        return null;
    }

    private boolean setPasswordForUser(String bearerToken, String userId, String password) {
        try {
            if (bearerToken == null) {
                lastErrorMessage = "No token available to set password";
                return false;
            }
            String url = usersUrl + "/" + userId + "/reset-password";
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(bearerToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> payload = Map.of(
                "type", "password",
                "temporary", false,
                "value", password
            );
            HttpEntity<Map<String, Object>> req = new HttpEntity<>(payload, headers);
            ResponseEntity<Void> resp = rest.exchange(url, HttpMethod.PUT, req, Void.class);
            logger.debug("setPasswordForUser response: {}", resp.getStatusCode());
            if (resp.getStatusCode().is2xxSuccessful() || resp.getStatusCode() == HttpStatus.NO_CONTENT) {
                lastErrorMessage = null;
                return true;
            } else {
                lastErrorMessage = "reset-password returned " + resp.getStatusCode();
                return false;
            }
        } catch (Exception e) {
            logger.warn("Failed to set password for user {}", userId, e);
            lastErrorMessage = "setPassword exception: " + e.getMessage();
            return false;
        }
    }
}
