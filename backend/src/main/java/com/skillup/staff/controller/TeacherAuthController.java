package com.skillup.staff.controller;

import com.skillup.portal.dto.PortalLoginRequest;
import com.skillup.portal.dto.PortalRefreshRequest;
import com.skillup.portal.dto.PortalTokenResponse;
import com.skillup.portal.service.KeycloakAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * JWT authentication for the teacher portal.
 *
 * <p>Uses the same Keycloak {@code skillup-portal} client and Direct Grant flow
 * as the parent portal. The JWT {@code sub} claim (Keycloak user UUID) is what
 * distinguishes a teacher from a parent — {@link com.skillup.staff.service.StaffContextService}
 * looks it up against the {@code staff.keycloak_id} column, while
 * {@link com.skillup.portal.service.PortalContextService} checks
 * {@code student_portal_accounts.keycloak_id}.
 *
 * <p>In local dev, skip auth entirely by passing the {@code X-Staff-ID} header.
 */
@RestController
@RequestMapping("/teacher/auth")
@RequiredArgsConstructor
@Tag(name = "Teacher Auth", description = "JWT login and token refresh for the teacher portal")
public class TeacherAuthController {

    private final KeycloakAdminService keycloakAdminService;

    @PostMapping("/login")
    @Operation(
            summary  = "Teacher login",
            description = """
                    Authenticates a staff member with email + password and returns a JWT.
                    The returned access token must be passed as `Authorization: Bearer <token>`
                    on all `/teacher/*` endpoints (except when local-dev-bypass is enabled).
                    """
    )
    public ResponseEntity<PortalTokenResponse> login(
            @Valid @RequestBody PortalLoginRequest req) {
        return ResponseEntity.ok(
                keycloakAdminService.login(req.getEmail(), req.getPassword()));
    }

    @PostMapping("/refresh")
    @Operation(
            summary = "Refresh teacher JWT",
            description = "Exchanges a refresh token for a new access token without re-entering credentials."
    )
    public ResponseEntity<PortalTokenResponse> refresh(
            @Valid @RequestBody PortalRefreshRequest req) {
        return ResponseEntity.ok(
                keycloakAdminService.refresh(req.getRefreshToken()));
    }
}
