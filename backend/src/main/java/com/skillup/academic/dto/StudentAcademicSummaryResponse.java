package com.skillup.academic.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

/** Full academic overview for a student — performance broken down by subject. */
@Data
@Builder
public class StudentAcademicSummaryResponse {

    private UUID studentId;
    private String studentRef;
    private String studentName;
    private String currentGrade;   // school year / form level

    private int totalExams;
    private double overallAveragePercentage;
    private String overallGrade;

    /** Performance per subject (each has its own trend and recent history). */
    private List<SubjectPerformanceResponse> subjects;
}
