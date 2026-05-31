package com.skillup.storage.service;

import com.skillup.common.exception.BusinessException;
import com.skillup.storage.dto.PresignedDownloadResponse;
import com.skillup.storage.dto.PresignedUploadResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URI;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;

/**
 * Production storage service — generates real presigned URLs for MinIO or AWS S3.
 * Active when {@code skillup.storage.enabled=true}.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "skillup.storage.enabled", havingValue = "true")
public class S3StorageService implements StorageService {

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "application/pdf", "image/jpeg", "image/png");

    private final S3Presigner presigner;
    private final String      bucket;
    private final int         expiryMinutes;
    private final int         maxSizeMb;

    public S3StorageService(
            @Value("${skillup.storage.endpoint}")          String endpoint,
            @Value("${skillup.storage.region}")            String region,
            @Value("${skillup.storage.access-key}")        String accessKey,
            @Value("${skillup.storage.secret-key}")        String secretKey,
            @Value("${skillup.storage.bucket}")            String bucket,
            @Value("${skillup.storage.presign-expiry-minutes:15}") int expiryMinutes,
            @Value("${skillup.file.max-size-mb:10}")       int maxSizeMb) {

        this.bucket        = bucket;
        this.expiryMinutes = expiryMinutes;
        this.maxSizeMb     = maxSizeMb;

        this.presigner = S3Presigner.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .build();

        log.info("S3StorageService initialised: endpoint={}, bucket={}", endpoint, bucket);
    }

    @Override
    public PresignedUploadResponse presignUpload(UUID studentId, String fileName,
                                                 String contentType, long fileSizeBytes) {
        validateUpload(contentType, fileSizeBytes);

        String fileKey = StubStorageService.buildKey(studentId, fileName);

        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(fileKey)
                .contentType(contentType)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(expiryMinutes))
                .putObjectRequest(putRequest)
                .build();

        String uploadUrl = presigner.presignPutObject(presignRequest).url().toString();

        log.info("presignUpload: key={}, type={}, size={}", fileKey, contentType, fileSizeBytes);

        return PresignedUploadResponse.builder()
                .uploadUrl(uploadUrl)
                .fileKey(fileKey)
                .expiresInSeconds(expiryMinutes * 60)
                .maxFileSizeBytes((long) maxSizeMb * 1024 * 1024)
                .build();
    }

    @Override
    public PresignedDownloadResponse presignDownload(String fileKey) {
        GetObjectRequest getRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(fileKey)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(expiryMinutes))
                .getObjectRequest(getRequest)
                .build();

        String downloadUrl = presigner.presignGetObject(presignRequest).url().toString();

        log.info("presignDownload: key={}", fileKey);

        return PresignedDownloadResponse.builder()
                .downloadUrl(downloadUrl)
                .expiresInSeconds(expiryMinutes * 60)
                .build();
    }

    // ── Validation ─────────────────────────────────────────────────────────────

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
}
