package com.skillup.student.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Used for PUT /students/{id} — updates personal and academic details only.
 * To change parents, use PATCH /students/{id}/parents.
 * To change status, use PATCH /students/{id}/status.
 */
@Data
public class UpdateStudentRequest {

    @NotBlank(message = "Student full name is required")
    @Size(max = 150)
    private String fullName;

    @Size(max = 80)
    private String preferredName;

    @Size(max = 20)
    private String icNumber;

    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    private String gender;

    @Size(max = 60)
    private String nationality;

    @Size(max = 150)
    private String schoolName;

    @Size(max = 30)
    private String currentGrade;

    private UUID branchId;

    @Size(max = 2000)
    private String notes;
}
