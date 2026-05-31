package com.skillup.assignment.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class AssignmentResponse {

    private UUID id;

    private UUID templateId;
    private String templateName;
    private String subjectName;

    private UUID sessionId;
    private String sessionLabel;   // e.g. "Mon 09:00 – 10:30 (2026-03-10)" when linked to a session

    private String title;
    private String description;

    private String type;           // HOMEWORK | PROJECT | QUIZ | CLASSWORK
    private String status;         // DRAFT | PUBLISHED | CLOSED

    private OffsetDateTime dueDate;
    private BigDecimal maxScore;   // null = ungraded
    private boolean allowLate;

    // Submission summary — only populated in list/detail views where aggregation is cheap
    private Integer totalSubmissions;
    private Integer pendingCount;
    private Integer submittedCount;
    private Integer gradedCount;

    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private String createdBy;
}
