package com.skillup.classmanagement.domain;

import com.skillup.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * A public holiday date that the session auto-generation scheduler will skip.
 * Managed by admin staff via the admin API.
 */
@Entity
@Table(name = "public_holidays")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PublicHoliday extends BaseEntity {

    /** The holiday date (unique — only one holiday per calendar date). */
    @Column(name = "holiday_date", nullable = false, unique = true)
    private LocalDate holidayDate;

    /** Display name, e.g. "Hari Merdeka", "Chinese New Year". */
    @Column(name = "name", nullable = false, length = 150)
    private String name;

    /** Denormalised year for efficient per-year queries. */
    @Column(name = "year", nullable = false)
    private int year;
}
