package com.skillup.fees.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Outstanding balance for a student across all unpaid fee records. */
@Data
@Builder
public class StudentBalanceResponse {
    private UUID studentId;
    private String studentRef;
    private String studentName;

    /** Sum of netAmount across all PENDING + PARTIAL + OVERDUE fee records. */
    private BigDecimal totalDue;

    /** Sum of paidAmount across those same records. */
    private BigDecimal totalPaid;

    /** totalDue - totalPaid = what the student still owes. */
    private BigDecimal totalOutstanding;

    /** Number of overdue fee records. */
    private int overdueCount;

    /** Individual unpaid / partially paid fee records, most-overdue first. */
    private List<FeeRecordResponse> unpaidFees;
}
