package com.skillup.student.dto;

import lombok.Data;

import java.util.UUID;

/**
 * Query parameters for GET /students
 * All fields are optional filters.
 */
@Data
public class StudentSearchRequest {

    private String name;            // partial match on full_name or preferred_name
    private String studentRef;      // exact match STU-YYYY-NNNN
    private String status;          // ACTIVE | INACTIVE | SUSPENDED | GRADUATED
    private UUID   branchId;
    private String currentGrade;
    private Integer page;           // 0-based, default 0
    private Integer size;           // default 20, max 100
    private String sortBy;          // full_name | student_ref | enrolled_date | status
    private String sortDir;         // ASC | DESC
}
