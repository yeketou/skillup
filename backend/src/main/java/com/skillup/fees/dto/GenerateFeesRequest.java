package com.skillup.fees.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class GenerateFeesRequest {

    /**
     * The billing month. Any day in the month works — the service normalises it to the 1st.
     * E.g. "2026-06-01" or "2026-06-15" both generate fees for June 2026.
     */
    @NotNull(message = "Billing month is required")
    private LocalDate billingMonth;

    /**
     * Payment deadline. Defaults to the 7th of the billing month if not provided.
     */
    private LocalDate dueDate;

    /**
     * Optional branch filter — generate only for a specific branch.
     * Null = generate for all branches.
     */
    private UUID branchId;
}
