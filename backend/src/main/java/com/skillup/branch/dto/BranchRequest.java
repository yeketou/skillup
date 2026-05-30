package com.skillup.branch.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class BranchRequest {

    @NotBlank(message = "Branch name is required")
    @Size(max = 150)
    private String name;

    @NotBlank(message = "Branch code is required")
    @Size(max = 20)
    @Pattern(regexp = "^[A-Z0-9_-]+$", message = "Code must be uppercase alphanumeric (A-Z, 0-9, -, _)")
    private String code;

    @Size(max = 500)
    private String address;

    @Size(max = 20)
    private String phone;

    @Email
    @Size(max = 150)
    private String email;
}
