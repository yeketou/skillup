package com.skillup.storage.service;

import com.skillup.storage.dto.PresignedDownloadResponse;
import com.skillup.storage.dto.PresignedUploadResponse;

import java.util.UUID;

/**
 * File storage abstraction — generates presigned URLs so the client uploads/downloads
 * directly to MinIO (dev) or S3 (prod) without routing file bytes through this backend.
 *
 * Two implementations:
 * <ul>
 *   <li>{@link StubStorageService} — no real storage, returns placeholder URLs (default in dev)</li>
 *   <li>{@link S3StorageService}   — real MinIO / AWS S3 presigned URLs (production)</li>
 * </ul>
 */
public interface StorageService {

    /**
     * Generates a presigned PUT URL for the client to upload a file.
     *
     * @param studentId   used to scope the object key under a student prefix
     * @param fileName    original filename (will be sanitised)
     * @param contentType MIME type — validated against the allowlist
     * @param fileSizeBytes expected size in bytes — validated against the configured limit
     * @return presigned PUT URL + the object key to pass back in the submission request
     */
    PresignedUploadResponse presignUpload(UUID studentId, String fileName,
                                         String contentType, long fileSizeBytes);

    /**
     * Generates a presigned GET URL for the client to download a stored file.
     *
     * @param fileKey the MinIO/S3 object key stored in {@link com.skillup.assignment.domain.SubmissionAttachment}
     * @return presigned GET URL
     */
    PresignedDownloadResponse presignDownload(String fileKey);
}
