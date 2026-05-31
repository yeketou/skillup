package com.skillup.attendance.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * Full attendance sheet for a single class session.
 * Returned by GET and POST /sessions/{id}/attendance.
 */
@Data
@Builder
public class SessionAttendanceResponse {
    private UUID sessionId;
    private LocalDate sessionDate;
    private LocalTime startTime;
    private LocalTime endTime;

    private UUID templateId;
    private String templateName;
    private String subjectName;
    private String sessionStatus;   // SCHEDULED | COMPLETED | CANCELLED

    // Summary counts
    private int totalEnrolled;
    private int present;
    private int absent;
    private int late;
    private int excused;
    private int unmarked;           // enrolled but not yet marked

    // Per-student rows (sorted by student name)
    private List<AttendanceRecordResponse> records;
}
