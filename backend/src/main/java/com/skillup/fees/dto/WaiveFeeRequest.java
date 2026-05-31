package com.skillup.fees.dto;

import lombok.Data;

@Data
public class WaiveFeeRequest {
    /** Reason for waiving (required for audit trail). */
    private String reason;
}
