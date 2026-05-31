package com.skillup.assignment.domain;

public enum SubmissionStatus {
    PENDING,     // auto-created at publish time — student hasn't submitted yet
    SUBMITTED,   // submitted on time
    LATE,        // submitted after the due date (only if allowLate = true)
    GRADED,      // teacher has scored and given feedback
    EXCUSED      // student exempted from this assignment
}
