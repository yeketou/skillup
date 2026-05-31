package com.skillup.assignment.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class SubmissionResponse {

    private UUID id;

    // Assignment
    private UUID assignmentId;
    private String assignmentTitle;
    private OffsetDateTime dueDate;
    private BigDecimal maxScore;

    // Student
    private UUID studentId;
    private String studentRef;
    private String studentName;

    // Submission
    private String status;          // PENDING | SUBMITTED | LATE | GRADED | EXCUSED
    private String content;
    private OffsetDateTime submittedAt;

    // Grading
    private BigDecimal score;
    private String feedback;
    private OffsetDateTime gradedAt;
    private String gradedBy;

    // Attachments
    private List<SubmissionAttachmentResponse> attachments;

    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
