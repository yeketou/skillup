package com.skillup.assignment.domain;

import com.skillup.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * Metadata for a file attached to an {@link AssignmentSubmission}.
 * The actual file lives in S3 / MinIO; this table stores the key and display metadata.
 *
 * Phase 9 will wire up presigned-URL generation and actual upload logic.
 */
@Entity
@Table(name = "submission_attachments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubmissionAttachment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submission_id", nullable = false)
    private AssignmentSubmission submission;

    /** S3 / MinIO object key, e.g. "submissions/2026/06/student-uuid/filename.pdf" */
    @Column(name = "file_key", nullable = false, length = 500)
    private String fileKey;

    /** Original filename shown in the UI. */
    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    /** File size in bytes. */
    @Column(name = "file_size", nullable = false)
    private long fileSize;

    /** MIME type: application/pdf | image/jpeg | image/png */
    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;
}
