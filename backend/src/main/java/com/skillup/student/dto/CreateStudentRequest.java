package com.skillup.student.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
public class CreateStudentRequest {

    // ── Personal ──────────────────────────────────────────────────────────────

    @NotBlank(message = "Student full name is required")
    @Size(max = 150)
    private String fullName;

    @Size(max = 80)
    private String preferredName;

    @Size(max = 20)
    private String icNumber;

    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    /**
     * MALE | FEMALE | UNSPECIFIED
     */
    private String gender = "UNSPECIFIED";

    @Size(max = 60)
    private String nationality = "Malaysian";

    // ── Academic ──────────────────────────────────────────────────────────────

    @Size(max = 150)
    private String schoolName;

    @Size(max = 30)
    private String currentGrade;

    // ── Enrollment ────────────────────────────────────────────────────────────

    @NotNull(message = "Branch ID is required")
    private UUID branchId;

    private LocalDate enrolledDate;     // defaults to today if null

    @Size(max = 2000)
    private String notes;

    // ── Parents ───────────────────────────────────────────────────────────────

    @Valid
    @NotEmpty(message = "At least one parent/guardian contact is required")
    private List<ParentRequest> parents = new ArrayList<>();
}
