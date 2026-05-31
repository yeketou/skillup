package com.skillup.staff.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class UpdateStaffRequest {

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

    private String role;

    private UUID branchId;

    private Boolean active;

    /** Replaces the full subject list when provided. */
    private List<UUID> subjectIds;

    private String notes;
}
