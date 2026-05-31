package com.skillup.attendance.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class AttendanceRecordResponse {
    private UUID id;               // null if student has not been marked yet

    private UUID sessionId;
    private LocalDate sessionDate;
    private LocalTime startTime;

    private UUID studentId;
    private String studentRef;
    private String studentName;

    /** PRESENT | ABSENT | LATE | EXCUSED | null = not yet marked */
    private String status;
    private String notes;

    /** When this record was last saved — null if not yet marked. */
    private OffsetDateTime markedAt;
}
