package com.finaxys.templateappname.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
public class KeycloakJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
    private final String apiClientId;

    public KeycloakJwtAuthenticationConverter(AppOAuth2Properties oauth2Properties) {
        this.apiClientId = oauth2Properties.getApiClientId();
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Set<GrantedAuthority> authorities = new HashSet<>();
        Collection<GrantedAuthority> scopeAuthorities = jwtGrantedAuthoritiesConverter.convert(jwt);
        if (scopeAuthorities != null) {
            authorities.addAll(scopeAuthorities);
        }
        authorities.addAll(extractClientRoleAuthorities(jwt));

        String principalName = jwt.getClaimAsString("preferred_username");
        if (!StringUtils.hasText(principalName)) {
            principalName = jwt.getSubject();
        }

        return new JwtAuthenticationToken(jwt, authorities, principalName);
    }

    private Collection<GrantedAuthority> extractClientRoleAuthorities(Jwt jwt) {
        Object resourceAccessClaim = jwt.getClaims().get("resource_access");
        if (!(resourceAccessClaim instanceof Map<?, ?> resourceAccess)) {
            return Set.of();
        }

        Object clientAccessClaim = resourceAccess.get(apiClientId);
        if (!(clientAccessClaim instanceof Map<?, ?> clientAccess)) {
            return Set.of();
        }

        Object rolesClaim = clientAccess.get("roles");
        if (!(rolesClaim instanceof Collection<?> rolesCollection)) {
            return Set.of();
        }

        Collection<GrantedAuthority> authorities = new ArrayList<>();
        for (Object roleObj : rolesCollection) {
            if (roleObj instanceof String role && StringUtils.hasText(role)) {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase(Locale.ROOT)));
            }
        }

        return authorities;
    }
}
