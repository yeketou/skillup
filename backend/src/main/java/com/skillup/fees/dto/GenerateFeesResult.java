package com.skillup.fees.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class GenerateFeesResult {
    private LocalDate billingMonth;
    private LocalDate dueDate;
    private int created;
    private int skipped;   // already existed or fee = RM 0
    private String message;
}
