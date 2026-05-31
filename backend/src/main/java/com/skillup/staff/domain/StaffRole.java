package com.skillup.staff.domain;

public enum StaffRole {
    /** Classroom teacher — can view their assigned classes, mark attendance, grade assignments. */
    TEACHER,
    /** Manages a single branch — same access as TEACHER plus branch-level admin. */
    BRANCH_ADMIN,
    /** Full centre-wide administrative access. */
    CENTRE_ADMIN
}
