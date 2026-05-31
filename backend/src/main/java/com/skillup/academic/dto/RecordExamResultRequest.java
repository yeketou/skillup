package com.skillup.academic.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class RecordExamResultRequest {

    @NotNull(message = "Student ID is required")
    private UUID studentId;

    /** Link to our subject catalogue — omit for school exams not in our system. */
    private UUID subjectId;

    /** Link to a class template — omit for school exams. */
    private UUID templateId;

    /** Branch recording this result. */
    private UUID branchId;

    @NotBlank(message = "Exam name is required")
    @Size(max = 200, message = "Exam name must not exceed 200 characters")
    private String examName;

    /**
     * Free-text subject name — used only when subjectId is not provided.
     * E.g. "Mathematics" from the school paper header.
     */
    @Size(max = 100)
    private String subjectLabel;

    /**
     * SCHOOL_EXAM | INTERNAL_EXAM | TRIAL_EXAM | MOCK_EXAM
     * Defaults to INTERNAL_EXAM.
     */
    private String examType;

    @NotNull(message = "Exam date is required")
    private LocalDate examDate;

    @NotNull(message = "Score is required")
    @DecimalMin(value = "0.0", message = "Score must be zero or greater")
    private BigDecimal score;

    /** Defaults to 100 when omitted. */
    @DecimalMin(value = "0.1", message = "Max score must be greater than zero")
    private BigDecimal maxScore;

    /**
     * Override auto-calculated grade.
     * Leave null to let the service derive it from the score percentage.
     */
    @Size(max = 5)
    private String grade;

    private String notes;
}
