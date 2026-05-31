package com.skillup.assignment.dto;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class SubmissionAttachmentResponse {

    private UUID id;
    private String fileKey;
    private String fileName;
    private long fileSize;
    private String contentType;
    private OffsetDateTime uploadedAt;  // maps to createdAt from BaseEntity
}
