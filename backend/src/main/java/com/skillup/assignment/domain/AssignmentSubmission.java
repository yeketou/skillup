package com.skillup.assignment.domain;

import com.skillup.common.BaseEntity;
import com.skillup.student.domain.Student;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A student's submission for an assignment.
 *
 * Created automatically (status = PENDING) when an assignment is published.
 * Updated when the student submits, and again when the teacher grades it.
 */
@Entity
@Table(name = "assignment_submissions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignmentSubmission extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id", nullable = false)
    private Assignment assignment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private SubmissionStatus status = SubmissionStatus.PENDING;

    /** Student's text answer or notes submitted with the assignment. */
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    /** Timestamp when the student hit "Submit". Null until submitted. */
    @Column(name = "submitted_at")
    private OffsetDateTime submittedAt;

    /** Teacher's score. Null until graded. Must be ≤ assignment.maxScore. */
    @Column(name = "score", precision = 5, scale = 1)
    private BigDecimal score;

    /** Teacher's written feedback. */
    @Column(name = "feedback", columnDefinition = "TEXT")
    private String feedback;

    @Column(name = "graded_at")
    private OffsetDateTime gradedAt;

    @Column(name = "graded_by", length = 100)
    private String gradedBy;

    @OneToMany(mappedBy = "submission", fetch = FetchType.LAZY,
               cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SubmissionAttachment> attachments = new ArrayList<>();
}
