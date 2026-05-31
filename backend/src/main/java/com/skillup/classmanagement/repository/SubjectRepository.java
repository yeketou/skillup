package com.skillup.classmanagement.repository;

import com.skillup.classmanagement.domain.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, UUID> {

    @Query("SELECT s FROM Subject s WHERE s.id = :id AND s.deletedAt IS NULL")
    Optional<Subject> findActiveById(@Param("id") UUID id);

    @Query("SELECT s FROM Subject s WHERE s.deletedAt IS NULL ORDER BY s.name")
    List<Subject> findAllActive();

    @Query("SELECT s FROM Subject s WHERE s.deletedAt IS NULL AND s.active = true ORDER BY s.name")
    List<Subject> findAllActiveAndEnabled();

    boolean existsByCodeAndDeletedAtIsNull(String code);

    @Query("SELECT s FROM Subject s WHERE s.code = :code AND s.deletedAt IS NULL")
    Optional<Subject> findByCodeActive(@Param("code") String code);
}
