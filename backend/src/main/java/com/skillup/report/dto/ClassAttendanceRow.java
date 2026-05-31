package com.skillup.report.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

/** Attendance statistics for one class template. */
@Data
@Builder
public class ClassAttendanceRow {

    private UUID   templateId;
    private String className;
    private String subjectName;
    private String branchName;
    private String dayOfWeek;
    private String timeSlot;          // e.g. "09:00–10:30"

    private int    totalMarked;
    private int    present;
    private int    late;
    private int    absent;
    private int    excused;

    private double attendanceRate;    // (present + late) / totalMarked × 100
}
