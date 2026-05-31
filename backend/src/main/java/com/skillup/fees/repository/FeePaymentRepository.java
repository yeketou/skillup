package com.skillup.fees.repository;

import com.skillup.fees.domain.FeePayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FeePaymentRepository extends JpaRepository<FeePayment, UUID> {

    @Query("SELECT p FROM FeePayment p WHERE p.feeRecord.id = :feeRecordId AND p.deletedAt IS NULL ORDER BY p.paymentDate DESC")
    List<FeePayment> findByFeeRecordId(@Param("feeRecordId") UUID feeRecordId);
}
