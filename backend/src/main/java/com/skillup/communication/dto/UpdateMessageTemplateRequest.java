package com.skillup.communication.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/** All fields optional — only non-null values are applied. */
@Data
public class UpdateMessageTemplateRequest {

    @Size(max = 100)
    private String name;

    private String category;
    private String description;
    private String content;

    @Size(max = 100)
    private String waTemplateName;

    private Boolean active;
}
