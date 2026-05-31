package com.skillup.attendance.repository;

import com.skillup.attendance.domain.AttendanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AttendanceRepository extends JpaRepository<AttendanceRecord, UUID> {

    /** All records for a session (used to build the attendance sheet). */
    @Query("SELECT a FROM AttendanceRecord a WHERE a.session.id = :sessionId AND a.deletedAt IS NULL")
    List<AttendanceRecord> findBySessionId(@Param("sessionId") UUID sessionId);

    /** Upsert lookup: find an existing record for a specific student-session pair. */
    Optional<AttendanceRecord> findBySessionIdAndStudentIdAndDeletedAtIsNull(UUID sessionId, UUID studentId);

    /** Full attendance history for a student, most-recent-first. */
    @Query("""
            SELECT a FROM AttendanceRecord a
            WHERE a.student.id = :studentId
              AND a.deletedAt IS NULL
            ORDER BY a.session.sessionDate DESC
            """)
    List<AttendanceRecord> findByStudentId(@Param("studentId") UUID studentId);

    /** Student attendance history filtered by date range. */
    @Query("""
            SELECT a FROM AttendanceRecord a
            WHERE a.student.id = :studentId
              AND a.session.sessionDate BETWEEN :from AND :to
              AND a.deletedAt IS NULL
            ORDER BY a.session.sessionDate DESC
            """)
    List<AttendanceRecord> findByStudentIdAndDateRange(@Param("studentId") UUID studentId,
                                                       @Param("from") LocalDate from,
                                                       @Param("to") LocalDate to);

    /** Student attendance for a specific class template (for per-class summary). */
    @Query("""
            SELECT a FROM AttendanceRecord a
            WHERE a.student.id  = :studentId
              AND a.session.template.id = :templateId
              AND a.deletedAt IS NULL
            ORDER BY a.session.sessionDate DESC
            """)
    List<AttendanceRecord> findByStudentIdAndTemplateId(@Param("studentId") UUID studentId,
                                                        @Param("templateId") UUID templateId);

    // ── Report queries ────────────────────────────────────────────────────────

    /**
     * All attendance records within a date range, optionally filtered by branch.
     * Used by the attendance report and dashboard.
     */
    @Query("""
            SELECT a FROM AttendanceRecord a
            WHERE a.deletedAt IS NULL
              AND a.session.sessionDate BETWEEN :from AND :to
              AND (:branchId IS NULL OR a.session.template.branch.id = :branchId)
            """)
    List<AttendanceRecord> findByDateRange(@Param("from")     LocalDate from,
                                           @Param("to")       LocalDate to,
                                           @Param("branchId") UUID branchId);

    /**
     * All attendance records for a specific class template in a date range.
     * Used to calculate per-class attendance rates.
     */
    @Query("""
            SELECT a FROM AttendanceRecord a
            WHERE a.deletedAt IS NULL
              AND a.session.template.id = :templateId
              AND a.session.sessionDate BETWEEN :from AND :to
            """)
    List<AttendanceRecord> findByTemplateAndDateRange(@Param("templateId") UUID templateId,
                                                      @Param("from")       LocalDate from,
                                                      @Param("to")         LocalDate to);
}
