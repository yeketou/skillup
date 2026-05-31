package com.skillup.report.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Exam performance analytics for a branch and date range. */
@Data
@Builder
public class AcademicReportResponse {

    private UUID      branchId;
    private String    branchName;
    private LocalDate fromDate;
    private LocalDate toDate;

    private int    totalExams;
    private double overallAveragePercentage;
    private String overallAverageGrade;
    private double overallPassRate;       // % scoring ≥ 50 %

    /** Per-subject breakdown. */
    private List<SubjectPerformanceRow> subjects;
}
