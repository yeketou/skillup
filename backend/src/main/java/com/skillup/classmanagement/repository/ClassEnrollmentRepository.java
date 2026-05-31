package com.skillup.classmanagement.repository;

import com.skillup.classmanagement.domain.ClassEnrollment;
import com.skillup.classmanagement.domain.EnrollmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClassEnrollmentRepository extends JpaRepository<ClassEnrollment, UUID> {

    @Query("SELECT e FROM ClassEnrollment e WHERE e.id = :id AND e.deletedAt IS NULL")
    Optional<ClassEnrollment> findActiveById(@Param("id") UUID id);

    /** All enrollments for a class template (for roster view). */
    @Query("SELECT e FROM ClassEnrollment e WHERE e.template.id = :templateId AND e.deletedAt IS NULL ORDER BY e.student.fullName")
    List<ClassEnrollment> findByTemplateId(@Param("templateId") UUID templateId);

    /** All enrollments for a student (their timetable). */
    @Query("SELECT e FROM ClassEnrollment e WHERE e.student.id = :studentId AND e.deletedAt IS NULL ORDER BY e.template.dayOfWeek, e.template.startTime")
    List<ClassEnrollment> findByStudentId(@Param("studentId") UUID studentId);

    /** Find a specific student-template enrollment (unique constraint). */
    Optional<ClassEnrollment> findByStudentIdAndTemplateIdAndDeletedAtIsNull(UUID studentId, UUID templateId);

    /** Count ACTIVE enrolments for capacity enforcement. */
    long countByTemplateIdAndStatusAndDeletedAtIsNull(UUID templateId, EnrollmentStatus status);

    /**
     * All ACTIVE enrolments across the entire centre, optionally filtered by branch.
     * Used by the fee-generation job to create monthly fee records.
     */
    @Query("""
            SELECT e FROM ClassEnrollment e
            WHERE e.status = :status
              AND e.deletedAt IS NULL
              AND (:branchId IS NULL OR e.template.branch.id = :branchId)
            ORDER BY e.template.branch.name, e.student.fullName
            """)
    List<ClassEnrollment> findActiveEnrollments(@Param("status") EnrollmentStatus status,
                                                @Param("branchId") UUID branchId);
}
