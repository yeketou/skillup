package com.skillup.assignment.domain;

public enum AssignmentStatus {
    DRAFT,       // visible only to teacher, still being edited
    PUBLISHED,   // visible to students, accepting submissions
    CLOSED       // deadline passed or manually closed — no new submissions
}
