package com.skillup.portal.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

/** Quick-glance summary of one child shown on the portal home screen. */
@Data
@Builder
public class PortalStudentSummary {

    private UUID   studentId;
    private String studentRef;
    private String fullName;
    private String preferredName;
    private String currentGrade;
    private String schoolName;
    private String branchName;
    private String status;

    // ── Quick-glance stats ─────────────────────────────────────────────────────
    /** Number of assignments in PENDING status (not yet submitted). */
    private int pendingAssignments;

    /** Number of fee records currently OVERDUE. */
    private int overdueFeesCount;

    /** Total outstanding fee balance across all months. */
    private BigDecimal outstandingBalance;
}
