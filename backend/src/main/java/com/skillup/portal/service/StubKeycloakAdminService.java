package com.skillup.portal.service;

import com.skillup.portal.dto.PortalTokenResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Local-dev stub — no real Keycloak connection.
 * All operations are logged and return plausible dummy values.
 *
 * Active when {@code skillup.keycloak.enabled=false} (the default).
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "skillup.keycloak.enabled", havingValue = "false", matchIfMissing = true)
public class StubKeycloakAdminService implements KeycloakAdminService {

    @Override
    public String createUser(String email, String fullName) {
        String fakeId = UUID.randomUUID().toString();
        log.info("[KEYCLOAK STUB] createUser → email={}, name={}, keycloakId={}", email, fullName, fakeId);
        return fakeId;
    }

    @Override
    public void setPassword(String keycloakUserId, String password, boolean temporary) {
        log.info("[KEYCLOAK STUB] setPassword → userId={}, temporary={}", keycloakUserId, temporary);
    }

    @Override
    public PortalTokenResponse login(String email, String password) {
        log.info("[KEYCLOAK STUB] login → email={}", email);
        return PortalTokenResponse.builder()
                .accessToken("stub-access-" + UUID.randomUUID())
                .refreshToken("stub-refresh-" + UUID.randomUUID())
                .expiresIn(86400)
                .tokenType("Bearer")
                .build();
    }

    @Override
    public PortalTokenResponse refresh(String refreshToken) {
        log.info("[KEYCLOAK STUB] refresh");
        return PortalTokenResponse.builder()
                .accessToken("stub-access-" + UUID.randomUUID())
                .refreshToken("stub-refresh-" + UUID.randomUUID())
                .expiresIn(86400)
                .tokenType("Bearer")
                .build();
    }

    @Override
    public void disableUser(String keycloakUserId) {
        log.info("[KEYCLOAK STUB] disableUser → userId={}", keycloakUserId);
    }
}
