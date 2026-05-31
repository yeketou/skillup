package com.skillup.academic.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

/** One student's score within a bulk exam result submission. */
@Data
public class StudentScoreEntry {

    @NotNull(message = "Student ID is required")
    private UUID studentId;

    @NotNull(message = "Score is required")
    @DecimalMin(value = "0.0", message = "Score must be zero or greater")
    private BigDecimal score;

    /** Optional grade override (auto-calculated if omitted). */
    private String grade;

    private String notes;
}
