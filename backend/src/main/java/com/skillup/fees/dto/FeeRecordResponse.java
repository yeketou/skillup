package com.skillup.fees.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class FeeRecordResponse {
    private UUID id;

    private UUID studentId;
    private String studentRef;
    private String studentName;

    private UUID templateId;
    private String templateName;
    private String subjectName;

    private LocalDate billingMonth;
    private BigDecimal amount;           // gross fee
    private BigDecimal discountAmount;
    private BigDecimal netAmount;        // amount - discount
    private BigDecimal paidAmount;       // total received so far
    private BigDecimal balance;          // netAmount - paidAmount

    private LocalDate dueDate;
    private String status;               // PENDING | PARTIAL | PAID | OVERDUE | WAIVED

    private String notes;

    private List<PaymentResponse> payments;

    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
