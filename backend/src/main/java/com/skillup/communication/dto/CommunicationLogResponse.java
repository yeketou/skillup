package com.skillup.communication.dto;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class CommunicationLogResponse {

    private UUID id;

    private UUID studentId;
    private String studentRef;
    private String studentName;

    private UUID templateId;
    private String templateName;

    private String recipientPhone;
    private String recipientName;
    private String messageCategory;
    private String renderedContent;

    private String status;              // PENDING | SENT | FAILED | DELIVERED
    private OffsetDateTime sentAt;
    private String providerMessageId;
    private String errorMessage;

    private OffsetDateTime createdAt;
}
