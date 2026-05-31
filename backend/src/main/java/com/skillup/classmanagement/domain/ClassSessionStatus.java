package com.skillup.classmanagement.domain;

public enum ClassSessionStatus {
    SCHEDULED,   // upcoming / not yet run
    COMPLETED,   // class ran as planned
    CANCELLED    // class was cancelled (public holiday, teacher sick, etc.)
}
