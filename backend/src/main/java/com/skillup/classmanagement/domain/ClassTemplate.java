package com.skillup.classmanagement.domain;

import com.skillup.branch.domain.Branch;
import com.skillup.common.BaseEntity;
import com.skillup.staff.domain.Staff;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * A recurring class definition — the timetable slot.
 *
 * Example: "Math Year 4 Morning — every Monday 10:00-11:30 at HQ, max 15 students, RM 150/month"
 *
 * Sessions (individual occurrences) are generated from this template.
 * Students are enrolled into the template, not individual sessions.
 */
@Entity
@Table(name = "class_templates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassTemplate extends BaseEntity {

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    /**
     * Free-text teacher name — kept for backwards compatibility and as a per-session
     * override (e.g. substitute). Prefer linking a {@link Staff} via {@link #teacher}.
     */
    @Column(name = "teacher_name", length = 150)
    private String teacherName;

    /**
     * Assigned teacher (Phase 12). Nullable — templates created before Phase 12
     * may only have the free-text {@link #teacherName}.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id")
    private Staff teacher;

    /** Day of the week this class recurs (uses java.time.DayOfWeek). */
    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false, length = 9)
    private DayOfWeek dayOfWeek;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "max_capacity", nullable = false)
    @Builder.Default
    private int maxCapacity = 20;

    /** Monthly fee per student for this class (RM). */
    @Column(name = "fee_amount", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal feeAmount = BigDecimal.ZERO;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;

    /** First date this class can run. */
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    /** Last date this class runs. NULL = indefinite. */
    @Column(name = "end_date")
    private LocalDate endDate;
}
