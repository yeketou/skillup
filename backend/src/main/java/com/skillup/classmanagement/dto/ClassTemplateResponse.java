package com.skillup.classmanagement.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class ClassTemplateResponse {
    private UUID id;
    private String name;

    private UUID subjectId;
    private String subjectCode;
    private String subjectName;

    private UUID branchId;
    private String branchName;

    /** Staff UUID of the assigned teacher (null if only free-text teacherName is set). */
    private UUID   teacherId;

    /** Resolved display name — from the linked Staff record if set, else the free-text field. */
    private String teacherName;
    private String dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
    private int maxCapacity;

    /** Current number of ACTIVE enrolments — compared against maxCapacity for availability. */
    private long enrolledCount;

    private BigDecimal feeAmount;
    private boolean active;
    private LocalDate startDate;
    private LocalDate endDate;

    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
