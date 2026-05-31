package com.skillup.staff.dto;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class StaffResponse {

    private UUID   id;
    private String staffRef;
    private String fullName;
    private String icNumber;
    private String phone;
    private String email;
    private String whatsappNumber;
    private String role;

    private UUID   branchId;
    private String branchName;

    private boolean active;
    private String  notes;

    private boolean keycloakProvisioned;
    private OffsetDateTime lastLoginAt;

    /** Subjects this staff member teaches. */
    private List<SubjectSummary> subjects;

    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    @Data
    @Builder
    public static class SubjectSummary {
        private UUID   id;
        private String code;
        private String name;
    }
}
