package com.skillup.classmanagement.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SessionStatusRequest {

    /** SCHEDULED | COMPLETED | CANCELLED */
    @NotBlank(message = "Status is required")
    private String status;

    private String notes;
}
