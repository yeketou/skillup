package com.skillup.report.controller;

import com.skillup.report.dto.*;
import com.skillup.report.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Read-only analytics and reporting endpoints.
 *
 * All endpoints are scoped to a single branch via the optional {@code branchId}
 * query parameter. Omitting it returns centre-wide aggregated data.
 */
@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
@Tag(name = "Reports", description = "Read-only analytics and reporting endpoints")
public class ReportController {

    private final ReportService reportService;

    // ─────────────────────────────────────────────────────────────────────────
    // DASHBOARD
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/dashboard")
    @Operation(
            summary = "Centre-wide dashboard snapshot",
            description = """
                    Returns current-month KPIs: active students, fee collection rate,
                    attendance rate, and pending assignment count.
                    Pass `branchId` to narrow the snapshot to one branch.
                    """
    )
    public ResponseEntity<DashboardResponse> getDashboard(
            @Parameter(description = "Optional branch filter. Omit for all branches.")
            @RequestParam(required = false) UUID branchId) {
        return ResponseEntity.ok(reportService.getDashboard(branchId));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FINANCIAL REPORT
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/financial")
    @Operation(
            summary = "Financial report — fees by billing month",
            description = """
                    Aggregates fee records across a range of billing months.
                    `fromMonth` and `toMonth` may be any date within the desired month
                    (the service normalises to the first of each month).
                    Example: `?fromMonth=2026-01-01&toMonth=2026-05-01`
                    """
    )
    public ResponseEntity<FinancialReportResponse> getFinancialReport(
            @Parameter(description = "Start of the billing-month range (any date in the month, ISO 8601)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromMonth,
            @Parameter(description = "End of the billing-month range (any date in the month, ISO 8601)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toMonth,
            @Parameter(description = "Optional branch filter. Omit for all branches.")
            @RequestParam(required = false) UUID branchId) {
        return ResponseEntity.ok(reportService.getFinancialReport(branchId, fromMonth, toMonth));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ATTENDANCE REPORT
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/attendance")
    @Operation(
            summary = "Attendance report with per-class breakdown",
            description = """
                    Overall and per-class attendance analytics for a date range.
                    The `lowThreshold` (default 80) defines the attendance-rate percentage
                    below which a student is flagged in the low-attendance list.
                    """
    )
    public ResponseEntity<AttendanceReportResponse> getAttendanceReport(
            @Parameter(description = "Report start date (ISO 8601, inclusive)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @Parameter(description = "Report end date (ISO 8601, inclusive)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @Parameter(description = "Optional branch filter. Omit for all branches.")
            @RequestParam(required = false) UUID branchId,
            @Parameter(description = "Attendance rate (%) below which students are flagged. Default: 80.")
            @RequestParam(defaultValue = "80") int lowThreshold) {
        return ResponseEntity.ok(reportService.getAttendanceReport(branchId, fromDate, toDate, lowThreshold));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ACADEMIC REPORT
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/academic")
    @Operation(
            summary = "Academic performance report by subject",
            description = """
                    Exam result analytics for a branch and date range.
                    Returns overall average, pass rate, and a per-subject breakdown
                    including grade distribution and pass rate.
                    """
    )
    public ResponseEntity<AcademicReportResponse> getAcademicReport(
            @Parameter(description = "Report start date (ISO 8601, inclusive)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @Parameter(description = "Report end date (ISO 8601, inclusive)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @Parameter(description = "Optional branch filter. Omit for all branches.")
            @RequestParam(required = false) UUID branchId) {
        return ResponseEntity.ok(reportService.getAcademicReport(branchId, fromDate, toDate));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PER-STUDENT REPORT
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/students/{studentId}")
    @Operation(
            summary = "Comprehensive per-student report",
            description = """
                    Parent-facing all-in-one report for a single student.
                    Aggregates: enrolled classes, fee history, attendance by class,
                    assignment completion and grades, and exam results by subject.
                    """
    )
    public ResponseEntity<StudentReportResponse> getStudentReport(
            @Parameter(description = "Student UUID")
            @PathVariable UUID studentId) {
        return ResponseEntity.ok(reportService.getStudentReport(studentId));
    }
}
