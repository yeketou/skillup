package com.skillup.portal.dto;

import com.skillup.assignment.dto.AttachmentRequest;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Portal-facing assignment submission request.
 * Identical in shape to {@link com.skillup.assignment.dto.SubmitAssignmentRequest}
 * but without {@code studentId} — the student is taken from the URL path and
 * portal account context so the parent cannot submit on behalf of someone else's child.
 */
@Data
public class PortalSubmitRequest {

    /** Free-text answer or notes. Optional when files are attached. */
    private String content;

    /**
     * Files already uploaded to MinIO/S3 via the presigned-upload endpoint.
     * Provide the returned {@code fileKey} plus display metadata for each file.
     */
    private List<AttachmentRequest> attachments = new ArrayList<>();
}
