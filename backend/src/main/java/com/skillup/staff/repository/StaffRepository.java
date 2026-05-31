package com.skillup.staff.repository;

import com.skillup.staff.domain.Staff;
import com.skillup.staff.domain.StaffRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StaffRepository extends JpaRepository<Staff, UUID> {

    @Query("SELECT s FROM Staff s WHERE s.id = :id AND s.deletedAt IS NULL")
    Optional<Staff> findActiveById(@Param("id") UUID id);

    @Query("SELECT s FROM Staff s WHERE s.keycloakId = :keycloakId AND s.deletedAt IS NULL")
    Optional<Staff> findByKeycloakId(@Param("keycloakId") UUID keycloakId);

    boolean existsByEmailAndDeletedAtIsNull(String email);

    /**
     * Multi-filter search. All parameters are optional — pass null to skip.
     */
    @Query("""
            SELECT s FROM Staff s
            WHERE s.deletedAt IS NULL
              AND (:branchId IS NULL OR s.branch.id = :branchId)
              AND (:role     IS NULL OR s.role = :role)
              AND (:active   IS NULL OR s.active = :active)
              AND (:name     IS NULL OR LOWER(s.fullName) LIKE LOWER(CONCAT('%', :name, '%')))
            ORDER BY s.fullName
            """)
    List<Staff> search(@Param("branchId") UUID branchId,
                       @Param("role")     StaffRole role,
                       @Param("active")   Boolean active,
                       @Param("name")     String name);

    /** All active teachers assigned to a specific branch. */
    @Query("""
            SELECT s FROM Staff s
            WHERE s.deletedAt IS NULL
              AND s.active = true
              AND s.role = 'TEACHER'
              AND s.branch.id = :branchId
            ORDER BY s.fullName
            """)
    List<Staff> findActiveTeachersByBranch(@Param("branchId") UUID branchId);
}
