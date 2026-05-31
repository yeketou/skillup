package com.skillup.assignment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/**
 * Metadata for a file that has already been uploaded to S3 / MinIO.
 * Phase 9 will add presigned-URL generation; for now the client provides the key directly.
 */
@Data
public class AttachmentRequest {

    /** S3 / MinIO object key, e.g. "submissions/2026/06/student-uuid/filename.pdf" */
    @NotBlank(message = "File key is required")
    private String fileKey;

    /** Original filename shown in the UI. */
    @NotBlank(message = "File name is required")
    private String fileName;

    /** File size in bytes. */
    @NotNull(message = "File size is required")
    @Positive(message = "File size must be positive")
    private Long fileSize;

    /** MIME type: application/pdf | image/jpeg | image/png */
    @NotBlank(message = "Content type is required")
    private String contentType;
}
