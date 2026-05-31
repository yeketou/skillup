package com.skillup.assignment.repository;

import com.skillup.assignment.domain.Assignment;
import com.skillup.assignment.domain.AssignmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, UUID> {

    /** Fetch a single non-deleted assignment. */
    @Query("SELECT a FROM Assignment a WHERE a.id = :id AND a.deletedAt IS NULL")
    Optional<Assignment> findActiveById(@Param("id") UUID id);

    /**
     * All assignments for a class template, optionally filtered by status.
     * Ordered newest first.
     */
    @Query("""
            SELECT a FROM Assignment a
            WHERE a.template.id = :templateId
              AND a.deletedAt IS NULL
              AND (:status IS NULL OR a.status = :status)
            ORDER BY a.dueDate DESC
            """)
    List<Assignment> findByTemplate(@Param("templateId") UUID templateId,
                                    @Param("status") AssignmentStatus status);

    /**
     * All assignments linked to a specific class session.
     */
    @Query("""
            SELECT a FROM Assignment a
            WHERE a.session.id = :sessionId
              AND a.deletedAt IS NULL
            ORDER BY a.dueDate DESC
            """)
    List<Assignment> findBySession(@Param("sessionId") UUID sessionId);
}
