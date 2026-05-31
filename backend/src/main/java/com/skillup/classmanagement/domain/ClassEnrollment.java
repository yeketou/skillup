package com.skillup.classmanagement.domain;

import com.skillup.common.BaseEntity;
import com.skillup.student.domain.Student;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * Enrolment of a student in a recurring class (ClassTemplate).
 *
 * A student enrolled in a template is expected to attend every session of that class.
 * Capacity is enforced at enrolment time: ACTIVE enrolments cannot exceed maxCapacity.
 */
@Entity
@Table(name = "class_enrollments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassEnrollment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private ClassTemplate template;

    @Column(name = "enrolled_date", nullable = false)
    private LocalDate enrolledDate;

    /** Set when status changes to DROPPED. */
    @Column(name = "dropped_date")
    private LocalDate droppedDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private EnrollmentStatus status = EnrollmentStatus.ACTIVE;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
}
