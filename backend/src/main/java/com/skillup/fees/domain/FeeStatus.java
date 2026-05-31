package com.skillup.fees.domain;

public enum FeeStatus {
    PENDING,    // unpaid, within due date
    PARTIAL,    // partially paid
    PAID,       // fully paid — net amount covered
    OVERDUE,    // unpaid or partially paid, past the due date
    WAIVED      // written off by admin (owner/manager)
}
