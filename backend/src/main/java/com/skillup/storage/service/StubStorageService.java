package com.skillup.storage.service;

import com.skillup.common.exception.BusinessException;
import com.skillup.storage.dto.PresignedDownloadResponse;
import com.skillup.storage.dto.PresignedUploadResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Development stub — no real MinIO/S3 connection.
 * Returns placeholder upload URLs so the rest of the flow can be tested
 * without a running object store.
 *
 * Active when {@code skillup.storage.enabled=false} (the default).
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "skillup.storage.enabled", havingValue = "false", matchIfMissing = true)
public class StubStorageService implements StorageService {

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "application/pdf", "image/jpeg", "image/png");

    @Value("${skillup.file.max-size-mb:10}")
    private int maxSizeMb;

    @Value("${skillup.storage.presign-expiry-minutes:15}")
    private int expiryMinutes;

    @Override
    public PresignedUploadResponse presignUpload(UUID studentId, String fileName,
                                                 String contentType, long fileSizeBytes) {
        validateUpload(contentType, fileSizeBytes);

        String fileKey = buildKey(studentId, fileName);
        String stubUrl  = "http://localhost:9000/skillup-uploads/" + fileKey
                + "?X-Amz-Expires=" + (expiryMinutes * 60) + "&X-Amz-Stub=true";

        log.info("[STORAGE STUB] presignUpload: key={}, size={} bytes, type={}", fileKey, fileSizeBytes, contentType);

        return PresignedUploadResponse.builder()
                .uploadUrl(stubUrl)
                .fileKey(fileKey)
                .expiresInSeconds(expiryMinutes * 60)
                .maxFileSizeBytes((long) maxSizeMb * 1024 * 1024)
                .build();
    }

    @Override
    public PresignedDownloadResponse presignDownload(String fileKey) {
        String stubUrl = "http://localhost:9000/skillup-uploads/" + fileKey
                + "?X-Amz-Expires=" + (expiryMinutes * 60) + "&X-Amz-Stub=true";

        log.info("[STORAGE STUB] presignDownload: key={}", fileKey);

        return PresignedDownloadResponse.builder()
                .downloadUrl(stubUrl)
                .expiresInSeconds(expiryMinutes * 60)
                .build();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void validateUpload(String contentType, long fileSizeBytes) {
        if (!ALLOWED_TYPES.contains(contentType)) {
            throw new BusinessException("INVALID_FILE_TYPE",
                    "File type not allowed: " + contentType
                    + ". Allowed: " + String.join(", ", ALLOWED_TYPES));
        }
        long maxBytes = (long) maxSizeMb * 1024 * 1024;
        if (fileSizeBytes > maxBytes) {
            throw new BusinessException("FILE_TOO_LARGE",
                    "File size " + fileSizeBytes + " bytes exceeds the " + maxSizeMb + " MB limit");
        }
    }

    static String buildKey(UUID studentId, String fileName) {
        String safe = fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
        LocalDate today = LocalDate.now();
        return String.format("submissions/%d/%02d/%s/%s-%s",
                today.getYear(), today.getMonthValue(),
                studentId, UUID.randomUUID(), safe);
    }
}
