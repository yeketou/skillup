package com.skillup.student.repository;

import com.skillup.student.domain.Student;
import com.skillup.student.domain.StudentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface StudentRepository extends JpaRepository<Student, UUID> {

    @Query("SELECT s FROM Student s WHERE s.id = :id AND s.deletedAt IS NULL")
    Optional<Student> findActiveById(@Param("id") UUID id);

    @Query("SELECT s FROM Student s WHERE s.studentRef = :ref AND s.deletedAt IS NULL")
    Optional<Student> findByStudentRef(@Param("ref") String ref);

    /**
     * Multi-filter search with optional name, status, branch, grade.
     * name matches against fullName or preferredName (case-insensitive, partial).
     */
    /**
     * CAST(:param AS string) is required for Hibernate 6 + PostgreSQL.
     * Without it, null parameters are inferred as bytea and lower(bytea) fails.
     */
    @Query("""
            SELECT s FROM Student s
            WHERE s.deletedAt IS NULL
              AND (:name     IS NULL OR LOWER(s.fullName)     LIKE LOWER(CONCAT('%', CAST(:name  AS string), '%'))
                                    OR LOWER(s.preferredName) LIKE LOWER(CONCAT('%', CAST(:name  AS string), '%')))
              AND (:status   IS NULL OR s.status = :status)
              AND (:branchId IS NULL OR s.branch.id = :branchId)
              AND (:grade    IS NULL OR LOWER(s.currentGrade) LIKE LOWER(CONCAT('%', CAST(:grade AS string), '%')))
            """)
    Page<Student> search(@Param("name")     String name,
                         @Param("status")   StudentStatus status,
                         @Param("branchId") UUID branchId,
                         @Param("grade")    String grade,
                         Pageable pageable);

    boolean existsByStudentRefAndDeletedAtIsNull(String studentRef);

    @Query("SELECT COUNT(s) FROM Student s WHERE s.branch.id = :branchId AND s.status = 'ACTIVE' AND s.deletedAt IS NULL")
    long countActiveByBranch(@Param("branchId") UUID branchId);
}
