package com.skillup.attendance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

/**
 * A single student's attendance mark — used as an item in {@link BulkAttendanceRequest}
 * and also for single-student PATCH updates (studentId ignored in PATCH; comes from path).
 */
@Data
public class AttendanceMarkRequest {

    @NotNull(message = "Student ID is required")
    private UUID studentId;

    /** PRESENT | ABSENT | LATE | EXCUSED */
    @NotBlank(message = "Status is required")
    private String status;

    private String notes;
}
