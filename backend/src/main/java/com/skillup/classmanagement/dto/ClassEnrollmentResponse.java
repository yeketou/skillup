package com.skillup.classmanagement.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class ClassEnrollmentResponse {
    private UUID id;

    private UUID studentId;
    private String studentRef;
    private String studentName;

    private UUID templateId;
    private String templateName;
    private String subjectName;
    private String dayOfWeek;

    private LocalDate enrolledDate;
    private LocalDate droppedDate;
    private String status;   // ACTIVE | DROPPED | COMPLETED
    private String notes;

    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
