package com.skillup.student.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Full student detail — used for GET /students/{id}
 */
@Data
@Builder
public class StudentResponse {

    private UUID id;
    private String studentRef;          // STU-2026-0001

    // Personal
    private String fullName;
    private String preferredName;
    private String icNumber;
    private LocalDate dateOfBirth;
    private String gender;
    private String nationality;
    private String photoUrl;

    // Academic
    private String schoolName;
    private String currentGrade;

    // Enrollment
    private String status;
    private UUID branchId;
    private String branchName;
    private LocalDate enrolledDate;
    private LocalDate graduatedDate;
    private String notes;

    // Parents
    private List<ParentResponse> parents;

    // Portal account
    private boolean portalAccountExists;
    private boolean portalAccountActive;

    // Audit
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private String createdBy;
}
