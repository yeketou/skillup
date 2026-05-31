package com.skillup.fees.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class PaymentResponse {
    private UUID id;
    private UUID feeRecordId;
    private BigDecimal amountPaid;
    private LocalDate paymentDate;
    private String paymentMethod;    // CASH | BANK_TRANSFER | ONLINE | CARD
    private String referenceNumber;
    private String notes;
    private String recordedBy;
    private OffsetDateTime createdAt;
}
