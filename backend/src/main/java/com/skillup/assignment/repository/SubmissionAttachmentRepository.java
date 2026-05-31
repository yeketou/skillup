package com.skillup.assignment.repository;

import com.skillup.assignment.domain.SubmissionAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SubmissionAttachmentRepository extends JpaRepository<SubmissionAttachment, UUID> {

    @Query("""
            SELECT a FROM SubmissionAttachment a
            WHERE a.submission.id = :submissionId
              AND a.deletedAt IS NULL
            ORDER BY a.createdAt
            """)
    List<SubmissionAttachment> findBySubmission(@Param("submissionId") UUID submissionId);
}
