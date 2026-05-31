package com.skillup.staff.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class CreateStaffRequest {

    @NotBlank(message = "Full name is required")
    @Size(max = 150)
    private String fullName;

    @Size(max = 20)
    private String icNumber;

    @Size(max = 20)
    private String phone;

    @Size(max = 150)
    private String email;

    @Size(max = 20)
    private String whatsappNumber;

    /**
     * Staff role: TEACHER | BRANCH_ADMIN | CENTRE_ADMIN.
     * Defaults to TEACHER if omitted.
     */
    private String role;

    /** Branch UUID — required for TEACHER and BRANCH_ADMIN, optional for CENTRE_ADMIN. */
    private UUID branchId;

    /** Subject UUIDs this staff member teaches. Optional — can be updated later. */
    private List<UUID> subjectIds;

    private String notes;
}
