package com.skillup.academic.repository;

import com.skillup.academic.domain.ExamResult;
import com.skillup.academic.domain.ExamType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExamResultRepository extends JpaRepository<ExamResult, UUID> {

    @Query("SELECT r FROM ExamResult r WHERE r.id = :id AND r.deletedAt IS NULL")
    Optional<ExamResult> findActiveById(@Param("id") UUID id);

    /**
     * Student's full result history, newest exam first.
     * All filter params are optional — pass null to skip.
     */
    @Query("""
            SELECT r FROM ExamResult r
            WHERE r.student.id = :studentId
              AND r.deletedAt IS NULL
              AND (:subjectId IS NULL OR r.subject.id = :subjectId)
              AND (:examType  IS NULL OR r.examType = :examType)
              AND (:from      IS NULL OR r.examDate >= :from)
              AND (:to        IS NULL OR r.examDate <= :to)
            ORDER BY r.examDate DESC, r.createdAt DESC
            """)
    List<ExamResult> findByStudent(@Param("studentId") UUID studentId,
                                   @Param("subjectId") UUID subjectId,
                                   @Param("examType")  ExamType examType,
                                   @Param("from")      LocalDate from,
                                   @Param("to")        LocalDate to);

    /**
     * All results for a class template, optionally filtered to a specific exam name.
     * Used for the class-level exam results view (teacher sees all students' scores).
     */
    @Query("""
            SELECT r FROM ExamResult r
            WHERE r.template.id = :templateId
              AND r.deletedAt IS NULL
              AND (:examName IS NULL OR LOWER(r.examName) LIKE LOWER(CONCAT('%', CAST(:examName AS string), '%')))
            ORDER BY r.examDate DESC, r.student.fullName
            """)
    List<ExamResult> findByTemplate(@Param("templateId") UUID templateId,
                                    @Param("examName")   String examName);

    /**
     * All results for a student grouped by subject, for the academic summary.
     * Returns newest first within each subject.
     */
    @Query("""
            SELECT r FROM ExamResult r
            WHERE r.student.id = :studentId
              AND r.deletedAt IS NULL
            ORDER BY r.subject.id NULLS LAST, r.subjectLabel NULLS LAST, r.examDate DESC
            """)
    List<ExamResult> findAllForSummary(@Param("studentId") UUID studentId);

    /**
     * All exam results within a date range, optionally scoped to one branch.
     * Branch is resolved via the direct branch FK on each ExamResult.
     * Used by the academic report to compute per-subject performance metrics.
     */
    @Query("""
            SELECT r FROM ExamResult r
            WHERE r.deletedAt IS NULL
              AND r.examDate BETWEEN :from AND :to
              AND (:branchId IS NULL OR r.branch.id = :branchId)
            ORDER BY r.examDate DESC, r.createdAt DESC
            """)
    List<ExamResult> findByDateRange(@Param("from")     LocalDate from,
                                    @Param("to")       LocalDate to,
                                    @Param("branchId") UUID branchId);
}
