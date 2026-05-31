package com.skillup.report.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

/** Per-branch student count — embedded in {@link DashboardResponse}. */
@Data
@Builder
public class BranchSummaryRow {
    private UUID   branchId;
    private String branchName;
    private String branchCode;
    private long   activeStudents;
}
