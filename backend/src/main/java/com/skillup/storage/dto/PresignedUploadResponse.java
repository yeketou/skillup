package com.skillup.storage.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Response for the presigned-upload endpoint.
 *
 * Workflow:
 *  1. Client calls GET /portal/uploads/presign-upload?fileName=x&contentType=application/pdf
 *  2. Client receives this response
 *  3. Client PUTs the file directly to {@code uploadUrl} (no auth header needed — URL is self-signed)
 *  4. Client includes {@code fileKey} in the subsequent submit-assignment request body
 */
@Data
@Builder
public class PresignedUploadResponse {

    /** Self-signed PUT URL — valid for {@code expiresInSeconds} seconds. */
    private String uploadUrl;

    /** MinIO/S3 object key to include in the {@link com.skillup.assignment.dto.AttachmentRequest}. */
    private String fileKey;

    /** Seconds until the presigned URL expires (default 15 minutes = 900). */
    private int expiresInSeconds;

    /** Maximum upload size in bytes enforced by the configured file limit. */
    private long maxFileSizeBytes;
}
