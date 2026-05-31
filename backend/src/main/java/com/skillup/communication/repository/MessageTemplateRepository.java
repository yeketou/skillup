package com.skillup.communication.repository;

import com.skillup.communication.domain.MessageCategory;
import com.skillup.communication.domain.MessageTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MessageTemplateRepository extends JpaRepository<MessageTemplate, UUID> {

    @Query("SELECT t FROM MessageTemplate t WHERE t.id = :id AND t.deletedAt IS NULL")
    Optional<MessageTemplate> findActiveById(@Param("id") UUID id);

    @Query("SELECT t FROM MessageTemplate t WHERE t.name = :name AND t.deletedAt IS NULL")
    Optional<MessageTemplate> findByName(@Param("name") String name);

    @Query("SELECT t FROM MessageTemplate t WHERE t.deletedAt IS NULL ORDER BY t.category, t.name")
    List<MessageTemplate> findAllActive();

    @Query("SELECT t FROM MessageTemplate t WHERE t.category = :category AND t.active = TRUE AND t.deletedAt IS NULL ORDER BY t.name")
    List<MessageTemplate> findActiveByCategory(@Param("category") MessageCategory category);

    boolean existsByNameAndDeletedAtIsNull(String name);
}
