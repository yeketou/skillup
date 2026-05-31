package com.skillup.communication.repository;

import com.skillup.communication.domain.CommunicationLog;
import com.skillup.communication.domain.MessageCategory;
import com.skillup.communication.domain.MessageStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CommunicationLogRepository extends JpaRepository<CommunicationLog, UUID> {

    /** All messages for a specific student, newest first. */
    @Query("""
            SELECT l FROM CommunicationLog l
            WHERE l.student.id = :studentId
              AND l.deletedAt IS NULL
            ORDER BY l.createdAt DESC
            """)
    List<CommunicationLog> findByStudent(@Param("studentId") UUID studentId);

    /**
     * Admin search — all filters optional.
     */
    @Query("""
            SELECT l FROM CommunicationLog l
            WHERE l.deletedAt IS NULL
              AND (:studentId IS NULL OR l.student.id = :studentId)
              AND (:status    IS NULL OR l.status = :status)
              AND (:category  IS NULL OR l.messageCategory = :category)
            ORDER BY l.createdAt DESC
            """)
    List<CommunicationLog> search(@Param("studentId") UUID studentId,
                                  @Param("status")    MessageStatus status,
                                  @Param("category")  MessageCategory category);
}
