package com.skillup.assignment.repository;

import com.skillup.assignment.domain.AssignmentSubmission;
import com.skillup.assignment.domain.SubmissionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AssignmentSubmissionRepository extends JpaRepository<AssignmentSubmission, UUID> {

    @Query("SELECT s FROM AssignmentSubmission s WHERE s.id = :id AND s.deletedAt IS NULL")
    Optional<AssignmentSubmission> findActiveById(@Param("id") UUID id);

    /** Teacher view — all submissions for an assignment. */
    @Query("""
            SELECT s FROM AssignmentSubmission s
            WHERE s.assignment.id = :assignmentId
              AND s.deletedAt IS NULL
            ORDER BY s.student.fullName
            """)
    List<AssignmentSubmission> findByAssignment(@Param("assignmentId") UUID assignmentId);

    /** Student view — a specific student's submission for one assignment. */
    @Query("""
            SELECT s FROM AssignmentSubmission s
            WHERE s.assignment.id = :assignmentId
              AND s.student.id = :studentId
              AND s.deletedAt IS NULL
            """)
    Optional<AssignmentSubmission> findByAssignmentAndStudent(@Param("assignmentId") UUID assignmentId,
                                                              @Param("studentId") UUID studentId);

    /** All submissions by a student (across all assignments), newest first. */
    @Query("""
            SELECT s FROM AssignmentSubmission s
            WHERE s.student.id = :studentId
              AND s.deletedAt IS NULL
            ORDER BY s.assignment.dueDate DESC
            """)
    List<AssignmentSubmission> findByStudent(@Param("studentId") UUID studentId);

    /**
     * Idempotency check — has a PENDING submission already been created for this
     * student + assignment pair?
     */
    boolean existsByAssignmentIdAndStudentIdAndDeletedAtIsNull(UUID assignmentId, UUID studentId);

    /** Count of submissions in a given status for an assignment (used for summary stats). */
    long countByAssignmentIdAndStatusAndDeletedAtIsNull(UUID assignmentId, SubmissionStatus status);

    /**
     * Centre-wide count of submissions in a given status, optionally scoped to one branch.
     * Branch is resolved via assignment → class template → branch.
     * Used by the dashboard to show pending-submission count.
     */
    @Query("""
            SELECT COUNT(s) FROM AssignmentSubmission s
            WHERE s.status = :status
              AND s.deletedAt IS NULL
              AND (:branchId IS NULL OR s.assignment.template.branch.id = :branchId)
            """)
    long countByStatusAndBranchId(@Param("status")   SubmissionStatus status,
                                  @Param("branchId") UUID branchId);
}
