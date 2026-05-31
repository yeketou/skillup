package com.skillup.classmanagement.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class ClassEnrollmentRequest {

    @NotNull(message = "Student ID is required")
    private UUID studentId;

    /** Defaults to today if not provided. */
    private LocalDate enrolledDate;

    private String notes;
}
