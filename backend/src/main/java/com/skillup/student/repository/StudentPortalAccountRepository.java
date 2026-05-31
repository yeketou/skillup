package com.skillup.student.repository;

import com.skillup.student.domain.StudentPortalAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface StudentPortalAccountRepository extends JpaRepository<StudentPortalAccount, UUID> {

    @Query("SELECT a FROM StudentPortalAccount a WHERE a.email = :email AND a.deletedAt IS NULL")
    Optional<StudentPortalAccount> findActiveByEmail(@Param("email") String email);

    boolean existsByEmailAndDeletedAtIsNull(String email);
}
