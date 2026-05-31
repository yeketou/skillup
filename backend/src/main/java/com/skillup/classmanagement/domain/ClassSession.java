package com.skillup.classmanagement.domain;

import com.skillup.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * An individual class occurrence, generated from a {@link ClassTemplate}.
 *
 * One session row per class date. Sessions are created in bulk for a term or month
 * via the generate API (POST /classes/{id}/sessions/generate).
 */
@Entity
@Table(name = "class_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassSession extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private ClassTemplate template;

    @Column(name = "session_date", nullable = false)
    private LocalDate sessionDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    /** Override teacher for this session (e.g. substitute). Null = use template's teacher. */
    @Column(name = "teacher_name", length = 150)
    private String teacherName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ClassSessionStatus status = ClassSessionStatus.SCHEDULED;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
}
