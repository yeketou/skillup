package com.skillup.classmanagement.domain;

import com.skillup.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * A subject or topic taught at the centre (e.g. Math, BM, Science).
 * Subjects are centre-wide — not branch-specific.
 */
@Entity
@Table(name = "subjects")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Subject extends BaseEntity {

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /** Short uppercase code shown in timetables: MATH, BM, SCI, etc. */
    @Column(name = "code", nullable = false, length = 20, unique = true)
    private String code;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;
}
