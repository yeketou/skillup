package com.skillup.classmanagement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SubjectRequest {

    @NotBlank(message = "Subject name is required")
    @Size(max = 100)
    private String name;

    /** Short uppercase code, e.g. "MATH", "BM", "ADD_MATH". */
    @NotBlank(message = "Subject code is required")
    @Size(max = 20)
    @Pattern(regexp = "^[A-Z0-9_]+$", message = "Code must be uppercase letters, digits and underscores only")
    private String code;

    @Size(max = 2000)
    private String description;

    private Boolean active;
}
