package com.skillup.classmanagement.repository;

import com.skillup.classmanagement.domain.PublicHoliday;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PublicHolidayRepository extends JpaRepository<PublicHoliday, UUID> {

    /** All non-deleted holidays for a given year, ordered by date. */
    List<PublicHoliday> findByYearAndDeletedAtIsNullOrderByHolidayDateAsc(int year);

    /** Lookup by exact date (for duplicate-check on creation). */
    Optional<PublicHoliday> findByHolidayDateAndDeletedAtIsNull(LocalDate holidayDate);

    /**
     * Returns all non-deleted holiday records whose date falls within [from, to].
     * Used by the session generator to build the holiday skip-set.
     */
    @Query("""
            SELECT h FROM PublicHoliday h
            WHERE h.deletedAt IS NULL
              AND h.holidayDate BETWEEN :from AND :to
            ORDER BY h.holidayDate
            """)
    List<PublicHoliday> findByDateRange(@Param("from") LocalDate from,
                                        @Param("to")   LocalDate to);
}
