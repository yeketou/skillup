package com.skillup.portal.controller;

import com.skillup.portal.dto.PortalLoginRequest;
import com.skillup.portal.dto.PortalRefreshRequest;
import com.skillup.portal.dto.PortalTokenResponse;
import com.skillup.portal.service.KeycloakAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Unauthenticated endpoints for the parent/student portal.
 * These paths are explicitly permitted in {@link com.skillup.common.security.SecurityConfig}.
 */
@RestController
@RequestMapping("/portal/auth")
@RequiredArgsConstructor
@Tag(name = "Portal — Auth", description = "Login and token-refresh for the parent/student portal (no JWT required)")
public class PortalAuthController {

    private final KeycloakAdminService keycloakAdminService;

    @PostMapping("/login")
    @Operation(
            summary = "Portal login",
            description = """
                    Authenticates with the parent's email and password.
                    Returns an access token (short-lived) and a refresh token (long-lived).
                    In dev mode (stub Keycloak) always succeeds and returns placeholder tokens.
                    """
    )
    public ResponseEntity<PortalTokenResponse> login(@Valid @RequestBody PortalLoginRequest req) {
        return ResponseEntity.ok(keycloakAdminService.login(req.getEmail(), req.getPassword()));
    }

    @PostMapping("/refresh")
    @Operation(
            summary = "Refresh portal access token",
            description = "Exchanges a valid refresh token for a new access + refresh token pair."
    )
    public ResponseEntity<PortalTokenResponse> refresh(@Valid @RequestBody PortalRefreshRequest req) {
        return ResponseEntity.ok(keycloakAdminService.refresh(req.getRefreshToken()));
    }
}
