package com.skillup.attendance.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

/** Attendance stats for a student within a single class template. */
@Data
@Builder
public class ClassAttendanceSummary {
    private UUID templateId;
    private String templateName;
    private String subjectName;
    private String dayOfWeek;

    private int totalSessions;
    private int present;
    private int absent;
    private int late;
    private int excused;

    /** (present + late) / totalSessions × 100, rounded to 1 decimal. */
    private double attendanceRate;
}
