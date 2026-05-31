package com.skillup.report.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Monthly fee collection report for a date range. */
@Data
@Builder
public class FinancialReportResponse {

    private UUID      branchId;
    private String    branchName;
    private LocalDate fromMonth;
    private LocalDate toMonth;

    /** One row per billing month within the requested range. */
    private List<MonthlyFeeRow> months;

    // ── Totals across all months ──────────────────────────────────────────────
    private BigDecimal totalGenerated;
    private BigDecimal totalCollected;
    private BigDecimal totalOutstanding;
    private double     overallCollectionRate;
    private int        totalRecords;
}
