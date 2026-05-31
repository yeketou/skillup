package com.skillup.communication.dto;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class MessageTemplateResponse {
    private UUID id;
    private String name;
    private String category;
    private String description;
    private String content;
    private String waTemplateName;
    private boolean active;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
