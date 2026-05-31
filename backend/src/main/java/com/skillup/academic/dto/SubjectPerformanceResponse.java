package com.skillup.academic.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Per-subject academic performance summary for a single student. */
@Data
@Builder
public class SubjectPerformanceResponse {

    /** UUID if linked to our subject catalogue, null for free-text entries. */
    private UUID subjectId;

    /** Resolved subject name (entity name or free-text label). */
    private String subjectName;

    private int resultCount;
    private double averagePercentage;   // across all recorded exams for this subject
    private String averageGrade;        // derived from averagePercentage

    /** Most recent exam's score and grade — quick at-a-glance result. */
    private BigDecimal latestScore;
    private BigDecimal latestMaxScore;
    private double latestPercentage;
    private String latestGrade;

    /**
     * Simple trend: "IMPROVING", "DECLINING", "STABLE", or "INSUFFICIENT_DATA"
     * Calculated by comparing the latest result to the previous one.
     */
    private String trend;

    /** Last 5 results for this subject, newest first. */
    private List<ExamResultResponse> recentResults;
}
