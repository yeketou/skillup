package com.skillup.attendance.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

/** Overall attendance summary for a student, with per-class breakdown. */
@Data
@Builder
public class StudentAttendanceSummaryResponse {
    private UUID studentId;
    private String studentRef;
    private String studentName;

    // Totals across all classes
    private int totalSessions;
    private int present;
    private int absent;
    private int late;
    private int excused;

    /** Overall attendance rate: (present + late) / total × 100. */
    private double attendanceRate;

    /** Breakdown by class. */
    private List<ClassAttendanceSummary> perClass;
}
