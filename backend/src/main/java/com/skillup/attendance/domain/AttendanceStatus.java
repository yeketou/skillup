package com.skillup.attendance.domain;

public enum AttendanceStatus {
    PRESENT,   // attended on time
    ABSENT,    // did not attend
    LATE,      // arrived late (still counts toward attendance rate)
    EXCUSED    // absent with a valid reason (medical, family, etc.)
}
