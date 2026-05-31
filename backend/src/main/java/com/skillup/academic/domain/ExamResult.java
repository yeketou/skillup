package com.skillup.academic.domain;

import com.skillup.branch.domain.Branch;
import com.skillup.classmanagement.domain.ClassTemplate;
import com.skillup.classmanagement.domain.Subject;
import com.skillup.common.BaseEntity;
import com.skillup.student.domain.Student;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Records a student's score for any exam or test.
 *
 * Covers both school exams (student brings their marked paper) and
 * internal tuition-centre tests. When {@code subjectId} is set the result
 * is linked to a known subject; otherwise {@code subjectLabel} holds the
 * free-text subject name from the school paper.
 *
 * Grade is auto-calculated by {@link com.skillup.academic.service.AcademicResultService}
 * using the Malaysian SPM grading scale but can be overridden manually.
 */
@Entity
@Table(name = "exam_results")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamResult extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    /** Linked to our subject catalogue — null for external school exams. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id")
    private Subject subject;

    /** Linked to an internal class — null for school exams. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id")
    private ClassTemplate template;

    /** Branch that recorded this result. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    /** Display name for this exam, e.g. "Peperiksaan Pertengahan Tahun 2026". */
    @Column(name = "exam_name", nullable = false, length = 200)
    private String examName;

    /**
     * Free-text subject name for school exams where the subject is not in our
     * system. Ignored (superseded by {@code subject.getName()}) when subject is set.
     */
    @Column(name = "subject_label", length = 100)
    private String subjectLabel;

    @Enumerated(EnumType.STRING)
    @Column(name = "exam_type", nullable = false, length = 30)
    @Builder.Default
    private ExamType examType = ExamType.INTERNAL_EXAM;

    @Column(name = "exam_date", nullable = false)
    private LocalDate examDate;

    /** Raw score achieved. */
    @Column(name = "score", nullable = false, precision = 5, scale = 1)
    private BigDecimal score;

    /** Maximum possible score — used to compute the percentage for grading. */
    @Column(name = "max_score", nullable = false, precision = 5, scale = 1)
    @Builder.Default
    private BigDecimal maxScore = new BigDecimal("100");

    /**
     * Malaysian grading letter (A+, A, A-, B+, … E).
     * Auto-set from score % on save; can be overridden manually.
     */
    @Column(name = "grade", length = 5)
    private String grade;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // ── Computed helpers ──────────────────────────────────────────────────────

    /**
     * Returns the effective subject display name:
     * subject entity name if linked, otherwise subjectLabel, otherwise null.
     */
    public String resolveSubjectName() {
        if (subject != null) return subject.getName();
        return subjectLabel;
    }

    /** Score as a percentage (0–100). */
    public double percentage() {
        if (maxScore.compareTo(BigDecimal.ZERO) == 0) return 0;
        return score.doubleValue() / maxScore.doubleValue() * 100.0;
    }
}
