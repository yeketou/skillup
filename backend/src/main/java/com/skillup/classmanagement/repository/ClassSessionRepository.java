package com.skillup.classmanagement.repository;

import com.skillup.classmanagement.domain.ClassSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClassSessionRepository extends JpaRepository<ClassSession, UUID> {

    @Query("SELECT s FROM ClassSession s WHERE s.id = :id AND s.deletedAt IS NULL")
    Optional<ClassSession> findActiveById(@Param("id") UUID id);

    /** All sessions for a template, ordered by date. */
    @Query("SELECT s FROM ClassSession s WHERE s.template.id = :templateId AND s.deletedAt IS NULL ORDER BY s.sessionDate")
    List<ClassSession> findByTemplateId(@Param("templateId") UUID templateId);

    /** Sessions for a template within a date range (inclusive). */
    @Query("""
            SELECT s FROM ClassSession s
            WHERE s.template.id = :templateId
              AND s.sessionDate BETWEEN :from AND :to
              AND s.deletedAt IS NULL
            ORDER BY s.sessionDate
            """)
    List<ClassSession> findByTemplateIdAndDateRange(@Param("templateId") UUID templateId,
                                                    @Param("from") LocalDate from,
                                                    @Param("to") LocalDate to);

    /** Check if a session already exists for a template on a given date (for idempotent generation). */
    Optional<ClassSession> findByTemplateIdAndSessionDateAndDeletedAtIsNull(UUID templateId, LocalDate sessionDate);

    /** Upcoming sessions across all templates from a given date. */
    @Query("""
            SELECT s FROM ClassSession s
            WHERE s.sessionDate >= :from
              AND s.deletedAt IS NULL
            ORDER BY s.sessionDate, s.startTime
            """)
    List<ClassSession> findUpcoming(@Param("from") LocalDate from);

    /**
     * Sessions assigned to a specific teacher within a date range (both bounds required).
     * The controller always supplies non-null bounds — default to today / today+1yr if omitted.
     */
    @Query("""
            SELECT s FROM ClassSession s
            WHERE s.template.teacher.id = :teacherId
              AND s.deletedAt IS NULL
              AND s.sessionDate >= :from
              AND s.sessionDate <= :to
            ORDER BY s.sessionDate, s.startTime
            """)
    List<ClassSession> findByTeacherIdAndDateRange(@Param("teacherId") UUID teacherId,
                                                   @Param("from")      LocalDate from,
                                                   @Param("to")        LocalDate to);
}
