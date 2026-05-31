package com.skillup.classmanagement.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class PublicHolidayResponse {
    private UUID      id;
    private LocalDate holidayDate;
    private String    name;
    private int       year;
}
