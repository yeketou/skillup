package com.skillup.fees.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class RecordPaymentRequest {

    @NotNull(message = "Payment amount is required")
    @DecimalMin(value = "0.01", message = "Payment amount must be greater than zero")
    private BigDecimal amountPaid;

    /** Defaults to today if not provided. */
    private LocalDate paymentDate;

    /** CASH | BANK_TRANSFER | ONLINE | CARD — defaults to CASH. */
    private String paymentMethod;

    /** Bank reference number, DuitNow transaction ID, receipt number. */
    private String referenceNumber;

    private String notes;
}
