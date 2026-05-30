package com.skillup.branch.repository;

import com.skillup.branch.domain.Branch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BranchRepository extends JpaRepository<Branch, UUID> {

    // Soft-delete aware: all queries exclude deleted records
    @Query("SELECT b FROM Branch b WHERE b.deletedAt IS NULL ORDER BY b.name")
    List<Branch> findAllActive();

    @Query("SELECT b FROM Branch b WHERE b.id = :id AND b.deletedAt IS NULL")
    Optional<Branch> findActiveById(UUID id);

    @Query("SELECT b FROM Branch b WHERE b.code = :code AND b.deletedAt IS NULL")
    Optional<Branch> findActiveByCode(String code);

    boolean existsByCodeAndDeletedAtIsNull(String code);
}
