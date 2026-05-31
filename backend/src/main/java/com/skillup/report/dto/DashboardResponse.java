package com.skillup.report.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Centre-wide snapshot for the admin dashboard. */
@Data
@Builder
public class DashboardResponse {

    private LocalDate asOf;
    private LocalDate currentBillingMonth;

    // ── Students ──────────────────────────────────────────────────────────────
    private long totalActiveStudents;
    private List<BranchSummaryRow> branches;

    // ── Fees (current billing month) ──────────────────────────────────────────
    private BigDecimal feesGenerated;       // gross billed this month
    private BigDecimal feesCollected;       // total received this month
    private BigDecimal feesOutstanding;     // unpaid balance
    private double    feeCollectionRate;    // collected / generated × 100
    private int       feeCountPaid;
    private int       feeCountPending;
    private int       feeCountOverdue;

    // ── Attendance (current calendar month) ──────────────────────────────────
    private int    attendanceTotalSessions;
    private int    attendanceTotalMarked;
    private double attendanceOverallRate;   // (PRESENT+LATE) / marked × 100

    // ── Assignments ───────────────────────────────────────────────────────────
    private long pendingSubmissions;        // PENDING status across all published assignments
}
