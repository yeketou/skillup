package com.skillup.classmanagement.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class ClassSessionResponse {
    private UUID id;
    private UUID templateId;
    private String templateName;
    private String subjectName;
    private LocalDate sessionDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String teacherName;
    private String status;   // SCHEDULED | COMPLETED | CANCELLED
    private String notes;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
