package com.skillup.classmanagement.service;

import com.skillup.classmanagement.config.ClassProperties;
import com.skillup.classmanagement.domain.*;
import com.skillup.classmanagement.dto.GenerationSummaryResponse;
import com.skillup.classmanagement.repository.ClassSessionRepository;
import com.skillup.classmanagement.repository.ClassTemplateRepository;
import com.skillup.classmanagement.repository.PublicHolidayRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Handles bulk, holiday-aware session generation across all active class templates.
 *
 * <p>This service is the engine behind both the weekly scheduler and the admin
 * manual-trigger endpoint. The per-template generate API in
 * {@link ClassManagementService} remains separate and holiday-unaware (admins
 * control dates manually there).
 *
 * <p><b>Idempotent:</b> existing sessions are never duplicated — safe to call
 * multiple times for the same date range.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionGenerationService {

    private final ClassTemplateRepository templateRepository;
    private final ClassSessionRepository  sessionRepository;
    private final PublicHolidayRepository holidayRepository;
    private final ClassProperties         classProperties;

    // ══════════════════════════════════════════════════════════════════════════
    // Public API
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Generate sessions for all active, non-expired templates from {@code from}
     * through {@code to} (inclusive), skipping any registered public holidays.
     */
    @Transactional
    public GenerationSummaryResponse generateForAllActiveTemplates(LocalDate from, LocalDate to) {
        // Load public holidays once for the whole date range
        List<PublicHoliday> holidayList = holidayRepository.findByDateRange(from, to);
        Set<LocalDate> holidayDates = holidayList.stream()
                .map(PublicHoliday::getHolidayDate)
                .collect(Collectors.toSet());
        List<String> holidayNames = holidayList.stream()
                .map(PublicHoliday::getName)
                .distinct()
                .collect(Collectors.toList());

        // All active templates that overlap the requested range
        List<ClassTemplate> templates = templateRepository.search(null, null, null, true)
                .stream()
                .filter(t -> !t.getStartDate().isAfter(to))                       // started before window ends
                .filter(t -> t.getEndDate() == null || !t.getEndDate().isBefore(from)) // hasn't ended before window starts
                .collect(Collectors.toList());

        int totalCreated  = 0;
        int totalHolidaySkips = 0;
        List<String> errors = new ArrayList<>();

        for (ClassTemplate template : templates) {
            try {
                int[] counts = generateForTemplate(template, from, to, holidayDates);
                totalCreated      += counts[0];
                totalHolidaySkips += counts[1];
            } catch (Exception e) {
                String msg = template.getName() + ": " + e.getMessage();
                errors.add(msg);
                log.error("[SessionGenerator] Failed for template {} ({}): {}",
                        template.getName(), template.getId(), e.getMessage());
            }
        }

        return GenerationSummaryResponse.builder()
                .generatedFrom(from)
                .generatedTo(to)
                .templatesProcessed(templates.size())
                .sessionsCreated(totalCreated)
                .holidayDatesSkipped(totalHolidaySkips)
                .skippedHolidays(totalHolidaySkips > 0 ? holidayNames : List.of())
                .errors(errors)
                .build();
    }

    /**
     * Convenience: generate using the configured lookahead window from today.
     * Called by the scheduler and by the admin endpoint when no explicit range is provided.
     */
    @Transactional
    public GenerationSummaryResponse generateDefault() {
        LocalDate from = LocalDate.now();
        LocalDate to   = from.plusWeeks(classProperties.getLookaheadWeeks());
        return generateForAllActiveTemplates(from, to);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Internal helpers
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Walk day-by-day through the effective date range for {@code template}:
     * <ul>
     *   <li>Skip days that don't match the template's dayOfWeek.</li>
     *   <li>Skip dates that are public holidays.</li>
     *   <li>Skip dates where a session already exists (idempotent).</li>
     *   <li>Otherwise, create a new SCHEDULED session.</li>
     * </ul>
     *
     * @return int[]{sessionsCreated, holidayDatesSkipped}
     */
    int[] generateForTemplate(ClassTemplate template, LocalDate from, LocalDate to,
                              Set<LocalDate> holidays) {
        // Clamp range to the template's own start/end
        LocalDate effectiveFrom = from.isBefore(template.getStartDate()) ? template.getStartDate() : from;
        LocalDate effectiveTo   = (template.getEndDate() != null && template.getEndDate().isBefore(to))
                ? template.getEndDate() : to;

        if (effectiveFrom.isAfter(effectiveTo)) return new int[]{0, 0};

        int created      = 0;
        int holidaySkips = 0;
        LocalDate cursor = effectiveFrom;

        while (!cursor.isAfter(effectiveTo)) {
            if (cursor.getDayOfWeek() == template.getDayOfWeek()) {
                if (holidays.contains(cursor)) {
                    holidaySkips++;
                    log.debug("[SessionGenerator] Holiday skip: template={} date={}", template.getName(), cursor);
                } else {
                    boolean exists = sessionRepository
                            .findByTemplateIdAndSessionDateAndDeletedAtIsNull(template.getId(), cursor)
                            .isPresent();
                    if (!exists) {
                        sessionRepository.save(ClassSession.builder()
                                .template(template)
                                .sessionDate(cursor)
                                .startTime(template.getStartTime())
                                .endTime(template.getEndTime())
                                .teacherName(template.getTeacherName())
                                .status(ClassSessionStatus.SCHEDULED)
                                .build());
                        created++;
                    }
                }
            }
            cursor = cursor.plusDays(1);
        }

        if (created > 0) {
            log.debug("[SessionGenerator] Template '{}': {} session(s) created", template.getName(), created);
        }
        return new int[]{created, holidaySkips};
    }
}
