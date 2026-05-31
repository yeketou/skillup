package com.skillup.staff.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/** Returned by GET /teacher/me — profile + assigned class templates. */
@Data
@Builder
public class TeacherMeResponse {

    private UUID   staffId;
    private String staffRef;
    private String fullName;
    private String role;
    private String branchName;

    /** Class templates currently assigned to this teacher. */
    private List<AssignedClass> assignedClasses;

    @Data
    @Builder
    public static class AssignedClass {
        private UUID      templateId;
        private String    templateName;
        private String    subjectName;
        private String    dayOfWeek;
        private LocalTime startTime;
        private LocalTime endTime;
        private int       enrolledCount;
        private boolean   active;
        private LocalDate startDate;
        private LocalDate endDate;
    }
}
