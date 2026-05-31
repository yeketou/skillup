package com.skillup.portal.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** Assignment + submission state as shown to a parent on the portal. */
@Data
@Builder
public class PortalAssignmentView {

    // ── Assignment ─────────────────────────────────────────────────────────────
    private UUID          assignmentId;
    private String        title;
    private String        description;
    private String        type;         // HOMEWORK | QUIZ | PROJECT | TEST | OTHER
    private OffsetDateTime dueDate;
    private boolean       allowLate;
    private BigDecimal    maxScore;     // null = ungraded
    private String        className;

    // ── This student's submission ──────────────────────────────────────────────
    private UUID           submissionId;
    private String         submissionStatus; // PENDING | SUBMITTED | LATE | GRADED | EXCUSED
    private OffsetDateTime submittedAt;
    private BigDecimal     score;
    private String         feedback;
    private List<AttachmentView> attachments;

    /** Attachment metadata — the actual fileKey is not exposed to the portal. */
    @Data
    @Builder
    public static class AttachmentView {
        private UUID   attachmentId;
        private String fileName;
        private long   fileSize;
        private String contentType;
    }
}
