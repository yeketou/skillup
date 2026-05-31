package com.skillup.portal.service;

import com.skillup.common.exception.BusinessException;
import com.skillup.portal.dto.PortalTokenResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Production Keycloak integration — wraps the Keycloak 24 Admin REST API
 * and the OpenID Connect token endpoint for the Student Portal.
 *
 * Active when {@code skillup.keycloak.enabled=true}.
 *
 * <p>Admin operations obtain a short-lived admin token using the Resource Owner
 * Password grant on the {@code master} realm with the {@code admin-cli} client.
 * Portal login/refresh use the Direct Grant against the {@code skillup} realm
 * with the {@code skillup-portal} confidential client.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "skillup.keycloak.enabled", havingValue = "true")
public class RealKeycloakAdminService implements KeycloakAdminService {

    @Value("${skillup.keycloak.base-url}")
    private String baseUrl;

    @Value("${skillup.keycloak.realm}")
    private String realm;

    @Value("${skillup.keycloak.portal-client-id}")
    private String portalClientId;

    @Value("${skillup.keycloak.portal-client-secret}")
    private String portalClientSecret;

    @Value("${skillup.keycloak.admin-username}")
    private String adminUsername;

    @Value("${skillup.keycloak.admin-password}")
    private String adminPassword;

    private final RestClient restClient = RestClient.create();

    // ── Admin token ────────────────────────────────────────────────────────────

    /** Obtains a fresh admin token from the master realm. */
    @SuppressWarnings("unchecked")
    private String getAdminToken() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("client_id", "admin-cli");
        form.add("username", adminUsername);
        form.add("password", adminPassword);

        Map<String, Object> resp = restClient.post()
                .uri(baseUrl + "/realms/master/protocol/openid-connect/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(Map.class);

        return (String) resp.get("access_token");
    }

    // ── User management ────────────────────────────────────────────────────────

    @Override
    public String createUser(String email, String fullName) {
        String adminToken = getAdminToken();

        Map<String, Object> userPayload = Map.of(
                "username",      email,
                "email",         email,
                "firstName",     extractFirstName(fullName),
                "lastName",      extractLastName(fullName),
                "enabled",       true,
                "emailVerified", true
        );

        try {
            var response = restClient.post()
                    .uri(baseUrl + "/admin/realms/" + realm + "/users")
                    .header("Authorization", "Bearer " + adminToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(userPayload)
                    .retrieve()
                    .toBodilessEntity();

            // Keycloak returns 201 Created with Location: .../users/{keycloakId}
            String location = response.getHeaders().getFirst("Location");
            if (location == null) {
                throw new BusinessException("KEYCLOAK_ERROR",
                        "Keycloak did not return a user location header");
            }
            String keycloakId = location.substring(location.lastIndexOf('/') + 1);
            log.info("Keycloak user created: email={}, keycloakId={}", email, keycloakId);
            return keycloakId;

        } catch (HttpClientErrorException.Conflict e) {
            throw new BusinessException("PORTAL_ACCOUNT_EXISTS",
                    "A Keycloak account already exists for email: " + email);
        }
    }

    @Override
    public void setPassword(String keycloakUserId, String password, boolean temporary) {
        String adminToken = getAdminToken();
        Map<String, Object> cred = Map.of(
                "type",      "password",
                "value",     password,
                "temporary", temporary
        );
        restClient.put()
                .uri(baseUrl + "/admin/realms/" + realm + "/users/" + keycloakUserId + "/reset-password")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(cred)
                .retrieve()
                .toBodilessEntity();
        log.info("Password set for Keycloak user: {}", keycloakUserId);
    }

    // ── Token endpoints ────────────────────────────────────────────────────────

    @Override
    @SuppressWarnings("unchecked")
    public PortalTokenResponse login(String email, String password) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type",    "password");
        form.add("client_id",     portalClientId);
        form.add("client_secret", portalClientSecret);
        form.add("username",      email);
        form.add("password",      password);

        try {
            Map<String, Object> resp = restClient.post()
                    .uri(baseUrl + "/realms/" + realm + "/protocol/openid-connect/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(Map.class);

            return buildTokenResponse(resp);

        } catch (HttpClientErrorException.Unauthorized e) {
            throw new BusinessException("INVALID_CREDENTIALS", "Invalid email or password");
        } catch (HttpClientErrorException.BadRequest e) {
            throw new BusinessException("PORTAL_NOT_ACTIVATED",
                    "Portal account is not activated. Contact the tuition centre.");
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public PortalTokenResponse refresh(String refreshToken) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type",    "refresh_token");
        form.add("client_id",     portalClientId);
        form.add("client_secret", portalClientSecret);
        form.add("refresh_token", refreshToken);

        try {
            Map<String, Object> resp = restClient.post()
                    .uri(baseUrl + "/realms/" + realm + "/protocol/openid-connect/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(Map.class);

            return buildTokenResponse(resp);

        } catch (HttpClientErrorException e) {
            throw new BusinessException("INVALID_REFRESH_TOKEN",
                    "Refresh token is invalid or has expired. Please log in again.");
        }
    }

    @Override
    public void disableUser(String keycloakUserId) {
        String adminToken = getAdminToken();
        restClient.put()
                .uri(baseUrl + "/admin/realms/" + realm + "/users/" + keycloakUserId)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("enabled", false))
                .retrieve()
                .toBodilessEntity();
        log.info("Keycloak user disabled: {}", keycloakUserId);
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private PortalTokenResponse buildTokenResponse(Map<String, Object> resp) {
        return PortalTokenResponse.builder()
                .accessToken((String) resp.get("access_token"))
                .refreshToken((String) resp.get("refresh_token"))
                .expiresIn(((Number) resp.get("expires_in")).intValue())
                .tokenType("Bearer")
                .build();
    }

    private String extractFirstName(String fullName) {
        if (fullName == null || fullName.isBlank()) return "";
        int idx = fullName.lastIndexOf(' ');
        return idx > 0 ? fullName.substring(0, idx).trim() : fullName;
    }

    private String extractLastName(String fullName) {
        if (fullName == null || fullName.isBlank()) return "";
        int idx = fullName.lastIndexOf(' ');
        return idx > 0 ? fullName.substring(idx + 1).trim() : "";
    }
}
