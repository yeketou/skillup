package com.skillup.academic.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Records the same exam (same name, date, max score) for multiple students at once.
 * Typical use: entering results after an internal class test.
 */
@Data
public class BulkRecordExamResultRequest {

    @NotBlank(message = "Exam name is required")
    @Size(max = 200)
    private String examName;

    /** SCHOOL_EXAM | INTERNAL_EXAM | TRIAL_EXAM | MOCK_EXAM. Defaults to INTERNAL_EXAM. */
    private String examType;

    @NotNull(message = "Exam date is required")
    private LocalDate examDate;

    @NotNull(message = "Max score is required")
    @DecimalMin(value = "0.1", message = "Max score must be greater than zero")
    private BigDecimal maxScore;

    /** Link all results to this class template (optional). */
    private UUID templateId;

    /** Link all results to this subject (optional). */
    private UUID subjectId;

    /** Free-text subject for school exams (used when subjectId is null). */
    @Size(max = 100)
    private String subjectLabel;

    private UUID branchId;

    @NotEmpty(message = "At least one student score is required")
    @Valid
    private List<StudentScoreEntry> scores;
}
