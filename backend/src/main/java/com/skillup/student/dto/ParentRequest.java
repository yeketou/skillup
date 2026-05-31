package com.skillup.student.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class ParentRequest {

    @NotBlank(message = "Parent name is required")
    @Size(max = 150)
    private String fullName;

    @Size(max = 20)
    private String icNumber;

    @NotBlank(message = "Parent phone is required")
    @Size(max = 20)
    private String phone;

    @Email(message = "Invalid email address")
    @Size(max = 150)
    private String email;

    @Size(max = 20)
    private String whatsappNumber;

    /**
     * PARENT | GUARDIAN | SIBLING | OTHER
     */
    private String relationship = "PARENT";

    /**
     * MS = Bahasa Malaysia (default), EN = English
     */
    private String preferredLanguage = "MS";

    private boolean primary = true;
}
