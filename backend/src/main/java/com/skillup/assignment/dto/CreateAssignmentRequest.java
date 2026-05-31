package com.skillup.assignment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
public class CreateAssignmentRequest {

    // Set from path variable in the controller — not required in the request body
    private UUID templateId;

    /** Optional — ties assignment to a specific class session. */
    private UUID sessionId;

    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title must not exceed 200 characters")
    private String title;

    private String description;

    /**
     * Assignment type: HOMEWORK | PROJECT | QUIZ | CLASSWORK
     * Defaults to HOMEWORK if omitted.
     */
    private String type;

    @NotNull(message = "Due date is required")
    private OffsetDateTime dueDate;

    /**
     * Maximum score — null means the assignment is ungraded (completion-based).
     * Must be > 0 when provided.
     */
    private BigDecimal maxScore;

    /** If true, submissions after dueDate are accepted but marked LATE. Defaults to false. */
    private boolean allowLate = false;
}
