package com.skillup.report.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;
import java.util.UUID;

/** Exam performance for one subject — row in {@link AcademicReportResponse}. */
@Data
@Builder
public class SubjectPerformanceRow {

    private UUID   subjectId;     // null if results used free-text subjectLabel
    private String subjectName;

    private int    examCount;
    private double averagePercentage;
    private String averageGrade;
    private double passRate;        // % of results with percentage ≥ 50

    /** Grade distribution: {"A+": 5, "A": 12, "A-": 8, ... "E": 2} */
    private Map<String, Long> gradeDistribution;
}
