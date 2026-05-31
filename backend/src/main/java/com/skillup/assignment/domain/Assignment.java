package com.skillup.assignment.domain;

import com.skillup.classmanagement.domain.ClassSession;
import com.skillup.classmanagement.domain.ClassTemplate;
import com.skillup.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * An assignment (homework, quiz, project) given to all students in a class.
 *
 * Lifecycle: DRAFT → PUBLISHED → CLOSED
 * Publishing auto-creates a PENDING {@link AssignmentSubmission} for every
 * currently enrolled student so the teacher can see who has / hasn't submitted.
 */
@Entity
@Table(name = "assignments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Assignment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private ClassTemplate template;

    /** Optional: ties this assignment to a specific class session. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private ClassSession session;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    @Builder.Default
    private AssignmentType type = AssignmentType.HOMEWORK;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private AssignmentStatus status = AssignmentStatus.DRAFT;

    /** Submission deadline — stored with timezone. */
    @Column(name = "due_date", nullable = false)
    private OffsetDateTime dueDate;

    /** Null means the assignment is not graded (practice / completion-based). */
    @Column(name = "max_score", precision = 5, scale = 1)
    private BigDecimal maxScore;

    /** If true, submissions after dueDate are accepted but marked LATE. */
    @Column(name = "allow_late", nullable = false)
    @Builder.Default
    private boolean allowLate = false;
}
