package com.skillup.academic.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Patch an existing exam result — only non-null fields are applied.
 * Useful for correcting a data-entry mistake.
 */
@Data
public class UpdateExamResultRequest {

    @Size(max = 200)
    private String examName;

    private LocalDate examDate;

    @DecimalMin(value = "0.0", message = "Score must be zero or greater")
    private BigDecimal score;

    @DecimalMin(value = "0.1", message = "Max score must be greater than zero")
    private BigDecimal maxScore;

    /** Explicit grade override — pass null to reset to auto-calculated. */
    @Size(max = 5)
    private String grade;

    /** Pass empty string "" to clear notes. */
    private String notes;
}
