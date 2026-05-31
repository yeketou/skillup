package com.skillup.portal.dto;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** Portal account profile returned by GET /portal/me. */
@Data
@Builder
public class PortalMeResponse {

    private UUID   accountId;
    private String email;
    private String fullName;

    /** True once the admin has provisioned a Keycloak identity for this account. */
    private boolean keycloakProvisioned;

    private OffsetDateTime lastLoginAt;

    /** All students linked to this portal account (typically 1–3 siblings). */
    private List<PortalStudentSummary> students;
}
