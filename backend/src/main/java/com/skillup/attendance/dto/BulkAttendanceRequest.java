package com.skillup.attendance.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * Bulk attendance submission for a session.
 * Typically submitted by the teacher after class — upserts records for each student listed.
 */
@Data
public class BulkAttendanceRequest {

    @NotEmpty(message = "At least one attendance record is required")
    @Valid
    private List<AttendanceMarkRequest> records;
}
