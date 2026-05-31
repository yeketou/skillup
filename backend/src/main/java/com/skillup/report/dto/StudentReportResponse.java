package com.skillup.report.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Comprehensive per-student report — aggregates data from fees, attendance,
 * assignments, and exam results into a single parent-facing summary.
 */
@Data
@Builder
public class StudentReportResponse {

    // ── Identity ──────────────────────────────────────────────────────────────
    private UUID      studentId;
    private String    studentRef;
    private String    fullName;
    private String    preferredName;
    private String    currentGrade;
    private String    schoolName;
    private String    branchName;
    private LocalDate enrolledDate;
    private String    status;

    // ── Enrolled classes ──────────────────────────────────────────────────────
    private List<EnrolledClassRow> classes;

    // ── Fees ──────────────────────────────────────────────────────────────────
    private BigDecimal feesTotalBilled;
    private BigDecimal feesTotalPaid;
    private BigDecimal feesOutstanding;
    private int        feesOverdueCount;
    private LocalDate  feesLastPaymentDate;

    // ── Attendance ────────────────────────────────────────────────────────────
    private int    attendanceTotalSessions;
    private int    attendancePresent;
    private int    attendanceLate;
    private int    attendanceAbsent;
    private int    attendanceExcused;
    private double attendanceOverallRate;
    private List<ClassAttendanceRow> attendanceByClass;

    // ── Assignments ───────────────────────────────────────────────────────────
    private int    assignmentTotal;
    private int    assignmentSubmitted;   // SUBMITTED + LATE
    private int    assignmentGraded;
    private int    assignmentPending;
    private int    assignmentExcused;
    private double assignmentCompletionRate;
    private Double assignmentAverageScore; // null if no graded submissions

    // ── Academic / Exam results ───────────────────────────────────────────────
    private int    examTotal;
    private double examOverallAverage;
    private String examOverallGrade;
    private List<com.skillup.academic.dto.SubjectPerformanceResponse> examBySubject;

    // ── Inner types ───────────────────────────────────────────────────────────

    @Data
    @Builder
    public static class EnrolledClassRow {
        private UUID   templateId;
        private String className;
        private String subjectName;
        private String dayOfWeek;
        private String timeSlot;
        private String enrollmentStatus;
        private LocalDate enrolledDate;
    }
}
