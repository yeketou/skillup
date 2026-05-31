package com.skillup.storage.dto;

import lombok.Builder;
import lombok.Data;

/** Response for the presigned-download endpoint — time-limited GET URL for a stored file. */
@Data
@Builder
public class PresignedDownloadResponse {

    /** Self-signed GET URL — valid for {@code expiresInSeconds} seconds. */
    private String downloadUrl;

    /** Seconds until the presigned URL expires. */
    private int expiresInSeconds;
}
