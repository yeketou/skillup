package com.skillup.student.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class ParentResponse {
    private UUID id;
    private String fullName;
    private String icNumber;
    private String phone;
    private String email;
    private String whatsappNumber;
    private String relationship;
    private String preferredLanguage;
    private boolean primary;
}
