package com.skillup.communication.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateMessageTemplateRequest {

    @NotBlank(message = "Template name is required")
    @Size(max = 100)
    private String name;

    /** FEE_REMINDER | ATTENDANCE_ALERT | ASSIGNMENT_REMINDER | WELCOME | GENERAL */
    @NotBlank(message = "Category is required")
    private String category;

    private String description;

    @NotBlank(message = "Content is required")
    private String content;

    /** Meta Business Manager pre-approved template name. */
    @Size(max = 100)
    private String waTemplateName;
}
