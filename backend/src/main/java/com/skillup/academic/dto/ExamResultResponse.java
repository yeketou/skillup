package com.skillup.academic.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class ExamResultResponse {

    private UUID id;

    // Student
    private UUID studentId;
    private String studentRef;
    private String studentName;

    // Subject / Class links
    private UUID subjectId;
    private String subjectName;    // resolved: subject entity name or free-text subjectLabel
    private UUID templateId;
    private String templateName;
    private UUID branchId;
    private String branchName;

    // Exam details
    private String examName;
    private String examType;       // SCHOOL_EXAM | INTERNAL_EXAM | TRIAL_EXAM | MOCK_EXAM
    private LocalDate examDate;

    // Scores
    private BigDecimal score;
    private BigDecimal maxScore;
    private double percentage;     // score / maxScore × 100
    private String grade;          // A+, A, A-, B+, B, B-, C+, C, C-, D, E

    private String notes;

    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private String recordedBy;
}
