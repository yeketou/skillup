package com.skillup.assignment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class GradeSubmissionRequest {

    /**
     * Score awarded. May be null if the assignment is ungraded
     * (i.e., maxScore is null — completion-only marking).
     * Must be ≤ assignment.maxScore when provided.
     */
    @DecimalMin(value = "0.0", message = "Score must be zero or greater")
    private BigDecimal score;

    /** Teacher's written feedback shown to the student. */
    private String feedback;
}
