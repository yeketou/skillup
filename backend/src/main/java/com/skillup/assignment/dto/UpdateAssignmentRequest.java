package com.skillup.assignment.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * All fields are optional — only non-null values are applied.
 * Only allowed while the assignment is in DRAFT status.
 */
@Data
public class UpdateAssignmentRequest {

    private UUID sessionId;

    @Size(max = 200, message = "Title must not exceed 200 characters")
    private String title;

    private String description;

    /** HOMEWORK | PROJECT | QUIZ | CLASSWORK */
    private String type;

    private OffsetDateTime dueDate;

    private BigDecimal maxScore;

    private Boolean allowLate;
}
