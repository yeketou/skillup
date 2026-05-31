package com.skillup.classmanagement.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Data
public class ClassTemplateRequest {

    @NotBlank(message = "Class name is required")
    @Size(max = 150)
    private String name;

    @NotNull(message = "Subject is required")
    private UUID subjectId;

    @NotNull(message = "Branch is required")
    private UUID branchId;

    @Size(max = 150)
    private String teacherName;

    /** MONDAY | TUESDAY | WEDNESDAY | THURSDAY | FRIDAY | SATURDAY | SUNDAY */
    @NotBlank(message = "Day of week is required")
    private String dayOfWeek;

    @NotNull(message = "Start time is required")
    private LocalTime startTime;

    @NotNull(message = "End time is required")
    private LocalTime endTime;

    @Min(value = 1, message = "Max capacity must be at least 1")
    private Integer maxCapacity;

    @DecimalMin(value = "0.0", message = "Fee amount cannot be negative")
    private BigDecimal feeAmount;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    private LocalDate endDate;

    private Boolean active;
}
