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

    /** Resolves the portal account from the Keycloak user UUID (JWT sub claim). */
    @Query("SELECT a FROM StudentPortalAccount a WHERE a.keycloakId = :keycloakId AND a.deletedAt IS NULL")
    Optional<StudentPortalAccount> findActiveByKeycloakId(@Param("keycloakId") UUID keycloakId);

    /**
     * Fetch by ID with students eagerly loaded (JOIN FETCH avoids LazyInitializationException
     * when the portal controller accesses the students collection outside a JPA session).
     */
    @Query("SELECT a FROM StudentPortalAccount a LEFT JOIN FETCH a.students WHERE a.id = :id AND a.deletedAt IS NULL")
    Optional<StudentPortalAccount> findByIdWithStudents(@Param("id") UUID id);

    /** Same as {@link #findActiveByKeycloakId} but with students eagerly fetched. */
    @Query("SELECT a FROM StudentPortalAccount a LEFT JOIN FETCH a.students WHERE a.keycloakId = :keycloakId AND a.deletedAt IS NULL")
    Optional<StudentPortalAccount> findByKeycloakIdWithStudents(@Param("keycloakId") UUID keycloakId);

    boolean existsByEmailAndDeletedAtIsNull(String email);
}
