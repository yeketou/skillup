package com.skillup.fees.repository;

import com.skillup.fees.domain.FeeRecord;
import com.skillup.fees.domain.FeeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FeeRecordRepository extends JpaRepository<FeeRecord, UUID> {

    @Query("SELECT f FROM FeeRecord f WHERE f.id = :id AND f.deletedAt IS NULL")
    Optional<FeeRecord> findActiveById(@Param("id") UUID id);

    /**
     * Multi-filter search. All parameters are optional (pass null to skip).
     */
    @Query("""
            SELECT f FROM FeeRecord f
            WHERE f.deletedAt IS NULL
              AND (:status       IS NULL OR f.status = :status)
              AND (:studentId    IS NULL OR f.student.id = :studentId)
              AND (:branchId     IS NULL OR f.template.branch.id = :branchId)
              AND (:billingMonth IS NULL OR f.billingMonth = :billingMonth)
            ORDER BY f.dueDate DESC, f.student.fullName
            """)
    List<FeeRecord> search(@Param("status")       FeeStatus status,
                           @Param("studentId")    UUID studentId,
                           @Param("branchId")     UUID branchId,
                           @Param("billingMonth") LocalDate billingMonth);

    /** All fee records for a student (for student fee history). */
    @Query("""
            SELECT f FROM FeeRecord f
            WHERE f.student.id = :studentId
              AND f.deletedAt IS NULL
            ORDER BY f.billingMonth DESC, f.template.name
            """)
    List<FeeRecord> findByStudentId(@Param("studentId") UUID studentId);

    /** Unpaid fees for a student (PENDING, PARTIAL, OVERDUE). */
    @Query("""
            SELECT f FROM FeeRecord f
            WHERE f.student.id = :studentId
              AND f.status IN ('PENDING','PARTIAL','OVERDUE')
              AND f.deletedAt IS NULL
            ORDER BY f.dueDate ASC
            """)
    List<FeeRecord> findUnpaidByStudentId(@Param("studentId") UUID studentId);

    /** Idempotency check for fee generation. */
    boolean existsByStudentIdAndTemplateIdAndBillingMonthAndDeletedAtIsNull(
            UUID studentId, UUID templateId, LocalDate billingMonth);

    /** Fees that are still PENDING or PARTIAL but have passed their due date. */
    @Query("""
            SELECT f FROM FeeRecord f
            WHERE f.status IN ('PENDING','PARTIAL')
              AND f.dueDate < :today
              AND f.deletedAt IS NULL
            """)
    List<FeeRecord> findPendingPastDue(@Param("today") LocalDate today);

    /** All OVERDUE fees — used by the reminder scheduler. */
    @Query("""
            SELECT f FROM FeeRecord f
            WHERE f.status = 'OVERDUE'
              AND f.deletedAt IS NULL
            """)
    List<FeeRecord> findAllOverdue();
}
