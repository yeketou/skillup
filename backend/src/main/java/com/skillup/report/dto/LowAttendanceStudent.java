package com.skillup.report.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

/** A student whose attendance rate is below the configured threshold. */
@Data
@Builder
public class LowAttendanceStudent {

    private UUID   studentId;
    private String studentRef;
    private String studentName;

    private UUID   templateId;
    private String className;

    private int    totalMarked;
    private int    present;
    private int    late;
    private int    absent;
    private int    excused;
    private double attendanceRate;
}
