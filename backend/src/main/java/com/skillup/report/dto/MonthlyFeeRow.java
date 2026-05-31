package com.skillup.report.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/** One billing month's fee statistics — row in {@link FinancialReportResponse}. */
@Data
@Builder
public class MonthlyFeeRow {

    private LocalDate billingMonth;     // first day of month, e.g. 2026-06-01

    private BigDecimal generated;       // sum of netAmount (amount − discount)
    private BigDecimal collected;       // sum of paidAmount
    private BigDecimal outstanding;     // sum of remaining balance

    private double collectionRate;      // collected / generated × 100

    private int countTotal;
    private int countPaid;
    private int countPartial;
    private int countPending;
    private int countOverdue;
    private int countWaived;
}
