package com.skillup.report.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Attendance analytics for a branch and date range. */
@Data
@Builder
public class AttendanceReportResponse {

    private UUID      branchId;
    private String    branchName;
    private LocalDate fromDate;
    private LocalDate toDate;

    // ── Centre-wide totals ────────────────────────────────────────────────────
    private int    totalMarked;
    private int    totalPresent;
    private int    totalLate;
    private int    totalAbsent;
    private int    totalExcused;
    private double overallAttendanceRate;   // (PRESENT + LATE) / totalMarked × 100

    /** Per-class breakdown. */
    private List<ClassAttendanceRow> classes;

    /**
     * Students whose attendance rate falls below the configured threshold.
     * Threshold is passed as a request parameter (default 80 %).
     */
    private int                      lowAttendanceThreshold;
    private List<LowAttendanceStudent> lowAttendanceStudents;
}
