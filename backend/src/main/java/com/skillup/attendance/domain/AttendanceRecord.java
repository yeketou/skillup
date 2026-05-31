package com.skillup.attendance.domain;

import com.skillup.classmanagement.domain.ClassSession;
import com.skillup.common.BaseEntity;
import com.skillup.student.domain.Student;
import jakarta.persistence.*;
import lombok.*;

/**
 * One attendance mark per student per class session.
 *
 * Records are created (or upserted) when a teacher marks attendance for a session.
 * Students who are enrolled but not yet marked have no record — the attendance sheet
 * shows them as "unmarked" by joining enrollments to records.
 */
@Entity
@Table(name = "attendance_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceRecord extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private ClassSession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private AttendanceStatus status;

    /** Reason for absence, lateness or excuse note. */
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
}
