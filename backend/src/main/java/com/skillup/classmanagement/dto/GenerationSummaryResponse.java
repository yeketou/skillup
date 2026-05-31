package com.skillup.classmanagement.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * Summary returned by the session auto-generation endpoint and logged by the scheduler.
 */
@Data
@Builder
public class GenerationSummaryResponse {

    /** Date range that was processed. */
    private LocalDate generatedFrom;
    private LocalDate generatedTo;

    /** Number of active templates considered. */
    private int templatesProcessed;

    /** Sessions actually inserted (excludes already-existing sessions). */
    private int sessionsCreated;

    /** Number of template-day combinations skipped because the date was a public holiday. */
    private int holidayDatesSkipped;

    /** Names of the holidays encountered in the date range (deduplicated). */
    private List<String> skippedHolidays;

    /** Per-template error messages — generation continues even if one template fails. */
    private List<String> errors;
}
