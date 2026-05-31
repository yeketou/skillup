package com.skillup.student.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Lightweight student record for list views, search results, and dropdowns.
 * Does NOT include parent details or notes.
 */
@Data
@Builder
public class StudentSummaryResponse {
    private UUID id;
    private String studentRef;
    private String fullName;
    private String preferredName;
    private String gender;
    private String currentGrade;
    private String status;
    private UUID branchId;
    private String branchName;
    private LocalDate enrolledDate;
    private String photoUrl;
    private String primaryParentName;
    private String primaryParentPhone;
}
