package com.skillup.student.domain;

/**
 * Lifecycle state of a student's enrolment.
 *
 * ACTIVE      — currently attending classes
 * INACTIVE    — temporarily not attending (e.g., school holiday break)
 * SUSPENDED   — fees overdue; access restricted on Student Portal
 * GRADUATED   — completed programme; historical record retained
 */
public enum StudentStatus {
    ACTIVE,
    INACTIVE,
    SUSPENDED,
    GRADUATED
}
