package com.skillup.portal.service;

import com.skillup.portal.dto.PortalTokenResponse;

/**
 * Keycloak integration for the Student Portal.
 *
 * <p>Two implementations are registered as Spring beans:
 * <ul>
 *   <li>{@link StubKeycloakAdminService} — no-op, used when {@code skillup.keycloak.enabled=false} (default in dev)</li>
 *   <li>{@link RealKeycloakAdminService} — calls Keycloak 24 REST API (production)</li>
 * </ul>
 */
public interface KeycloakAdminService {

    /**
     * Creates a Keycloak user account for a parent.
     *
     * @param email    login email — becomes the Keycloak username
     * @param fullName display name stored in Keycloak
     * @return Keycloak user UUID (the {@code sub} claim in the parent's JWT)
     */
    String createUser(String email, String fullName);

    /**
     * Sets or resets the user's password.
     *
     * @param keycloakUserId Keycloak user UUID
     * @param password       plain-text password to set
     * @param temporary      if true the user must change the password on first login
     */
    void setPassword(String keycloakUserId, String password, boolean temporary);

    /**
     * Authenticates via the Resource Owner Password Credentials (Direct Grant) flow.
     * Used by the portal login endpoint.
     *
     * @return access + refresh tokens
     */
    PortalTokenResponse login(String email, String password);

    /**
     * Exchanges a refresh token for a fresh access + refresh token pair.
     * Used by the portal token-refresh endpoint.
     */
    PortalTokenResponse refresh(String refreshToken);

    /**
     * Disables the Keycloak user — prevents further portal logins without
     * deleting the account or its history.
     */
    void disableUser(String keycloakUserId);
}
