package com.skillup.student.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class StudentStatusRequest {

    @NotBlank(message = "Status is required")
    private String status;   // ACTIVE | INACTIVE | SUSPENDED | GRADUATED

    private String reason;   // optional reason, stored in notes
}
