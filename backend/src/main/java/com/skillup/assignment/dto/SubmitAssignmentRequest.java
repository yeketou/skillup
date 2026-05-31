package com.skillup.assignment.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
public class SubmitAssignmentRequest {

    /** The student submitting. Required — teacher may also submit on behalf of a student. */
    @NotNull(message = "Student ID is required")
    private UUID studentId;

    /** Free-text answer / notes. Optional if files are attached. */
    private String content;

    /** Files already uploaded to S3/MinIO. Metadata recorded here. */
    private List<AttachmentRequest> attachments = new ArrayList<>();
}
