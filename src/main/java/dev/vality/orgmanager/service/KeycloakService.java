package dev.vality.orgmanager.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.keycloak.representations.AccessToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class KeycloakService {

    private final ObjectMapper objectMapper;

    public AccessToken getAccessToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new IllegalStateException("Jwt principal is required");
        }
        return objectMapper.convertValue(normalizedClaims(jwt.getClaims()), AccessToken.class);
    }

    private Map<String, Object> normalizedClaims(Map<String, Object> claims) {
        Map<String, Object> normalized = new HashMap<>(claims);
        normalizeEpochClaim(normalized, "exp");
        normalizeEpochClaim(normalized, "iat");
        normalizeEpochClaim(normalized, "nbf");
        return normalized;
    }

    private void normalizeEpochClaim(Map<String, Object> claims, String claimName) {
        Object claim = claims.get(claimName);
        if (claim instanceof Instant instant) {
            claims.put(claimName, instant.getEpochSecond());
        }
    }
}
