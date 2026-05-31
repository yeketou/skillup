package com.skillup.student.repository;

import com.skillup.student.domain.StudentParent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StudentParentRepository extends JpaRepository<StudentParent, UUID> {

    @Query("SELECT p FROM StudentParent p WHERE p.student.id = :studentId AND p.deletedAt IS NULL")
    List<StudentParent> findActiveByStudentId(@Param("studentId") UUID studentId);

    @Query("SELECT p FROM StudentParent p WHERE p.student.id = :studentId AND p.primary = TRUE AND p.deletedAt IS NULL")
    Optional<StudentParent> findPrimaryByStudentId(@Param("studentId") UUID studentId);

    @Query("SELECT COUNT(p) > 0 FROM StudentParent p WHERE p.email = :email AND p.deletedAt IS NULL")
    boolean existsByEmail(@Param("email") String email);
}
