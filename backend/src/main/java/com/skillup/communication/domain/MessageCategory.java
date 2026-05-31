package com.skillup.communication.domain;

public enum MessageCategory {
    FEE_REMINDER,           // fee due / overdue alerts
    ATTENDANCE_ALERT,       // absence notifications to parents
    ASSIGNMENT_REMINDER,    // upcoming deadline alerts
    WELCOME,                // new student registration
    GENERAL                 // ad-hoc messages from staff
}
