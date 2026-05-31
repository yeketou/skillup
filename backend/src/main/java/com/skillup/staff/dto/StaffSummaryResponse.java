package com.skillup.staff.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

/** Lightweight staff record used in list/search results. */
@Data
@Builder
public class StaffSummaryResponse {
    private UUID   id;
    private String staffRef;
    private String fullName;
    private String phone;
    private String email;
    private String role;
    private UUID   branchId;
    private String branchName;
    private boolean active;
    private boolean keycloakProvisioned;
}
