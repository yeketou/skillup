package com.skillup.classmanagement.repository;

import com.skillup.classmanagement.domain.ClassTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClassTemplateRepository extends JpaRepository<ClassTemplate, UUID> {

    @Query("SELECT t FROM ClassTemplate t WHERE t.id = :id AND t.deletedAt IS NULL")
    Optional<ClassTemplate> findActiveById(@Param("id") UUID id);

    /**
     * Multi-filter search across templates.
     * All parameters are optional — pass null to skip a filter.
     * Uses CAST for nullable parameters to prevent Hibernate 6 bytea inference.
     */
    @Query("""
            SELECT t FROM ClassTemplate t
            WHERE t.deletedAt IS NULL
              AND (:branchId  IS NULL OR t.branch.id = :branchId)
              AND (:subjectId IS NULL OR t.subject.id = :subjectId)
              AND (:dayOfWeek IS NULL OR t.dayOfWeek = :dayOfWeek)
              AND (:active    IS NULL OR t.active = :active)
            ORDER BY t.dayOfWeek, t.startTime, t.name
            """)
    List<ClassTemplate> search(@Param("branchId")  UUID branchId,
                               @Param("subjectId") UUID subjectId,
                               @Param("dayOfWeek") DayOfWeek dayOfWeek,
                               @Param("active")    Boolean active);

    /** Templates where this staff member is the assigned teacher. */
    @Query("""
            SELECT t FROM ClassTemplate t
            WHERE t.teacher.id = :teacherId
              AND t.deletedAt IS NULL
            ORDER BY t.dayOfWeek, t.startTime
            """)
    List<ClassTemplate> findByTeacherId(@Param("teacherId") UUID teacherId);
}
