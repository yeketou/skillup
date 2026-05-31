package com.skillup.storage.controller;

import com.skillup.storage.dto.PresignedDownloadResponse;
import com.skillup.storage.dto.PresignedUploadResponse;
import com.skillup.storage.service.StorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Presigned URL endpoints for direct-to-storage file transfers.
 *
 * <p>Upload flow:
 * <ol>
 *   <li>Client calls {@code GET /portal/uploads/presign-upload} and receives a presigned PUT URL + fileKey.</li>
 *   <li>Client PUTs the file directly to the presigned URL (no auth headers needed).</li>
 *   <li>Client includes the {@code fileKey} in the subsequent submit-assignment request.</li>
 * </ol>
 *
 * <p>Download flow:
 * <ol>
 *   <li>Client calls {@code GET /portal/uploads/presign-download?fileKey=…} to get a time-limited GET URL.</li>
 *   <li>Client GETs the file directly from that URL.</li>
 * </ol>
 */
@RestController
@RequestMapping("/portal/uploads")
@RequiredArgsConstructor
@Tag(name = "Portal — File Uploads", description = "Presigned URL generation for direct MinIO/S3 uploads and downloads")
public class StorageController {

    private final StorageService storageService;

    @GetMapping("/presign-upload")
    @Operation(
            summary = "Request a presigned PUT URL for direct file upload to storage",
            description = """
                    Returns a self-signed PUT URL valid for 15 minutes (configurable).
                    The client uploads the file directly to the URL — file bytes never pass through this backend.
                    After a successful upload, include the returned `fileKey` in the assignment submission body.
                    """
    )
    public ResponseEntity<PresignedUploadResponse> presignUpload(
            @Parameter(description = "The student who owns this submission", required = true)
            @RequestParam UUID studentId,
            @Parameter(description = "Original filename (e.g. homework.pdf)", required = true)
            @RequestParam String fileName,
            @Parameter(description = "MIME type: application/pdf | image/jpeg | image/png", required = true)
            @RequestParam String contentType,
            @Parameter(description = "File size in bytes — validated against the 10 MB limit", required = true)
            @RequestParam long fileSizeBytes) {

        return ResponseEntity.ok(
                storageService.presignUpload(studentId, fileName, contentType, fileSizeBytes));
    }

    @GetMapping("/presign-download")
    @Operation(
            summary = "Request a presigned GET URL to download a submitted file",
            description = """
                    Returns a self-signed GET URL valid for 15 minutes (configurable).
                    Use the `fileKey` from the attachment record in the submission response.
                    """
    )
    public ResponseEntity<PresignedDownloadResponse> presignDownload(
            @Parameter(description = "MinIO/S3 object key from the attachment record", required = true)
            @RequestParam String fileKey) {

        return ResponseEntity.ok(storageService.presignDownload(fileKey));
    }
}
