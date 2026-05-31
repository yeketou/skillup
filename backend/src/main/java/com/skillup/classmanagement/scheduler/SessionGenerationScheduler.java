package com.skillup.classmanagement.scheduler;

import com.skillup.classmanagement.dto.GenerationSummaryResponse;
import com.skillup.classmanagement.service.SessionGenerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Weekly job that auto-generates class sessions for all active templates.
 *
 * <p>Runs every Monday at 06:00 MYT (configurable via
 * {@code skillup.classes.generation-cron}). The lookahead window is controlled by
 * {@code skillup.classes.lookahead-weeks} (default 4 weeks).
 *
 * <p>Generation is idempotent — running the job multiple times for the same date
 * range is safe and will not create duplicate sessions.
 *
 * <p>Dates registered in the {@code public_holidays} table are skipped automatically.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SessionGenerationScheduler {

    private final SessionGenerationService sessionGenerationService;

    @Scheduled(cron = "${skillup.classes.generation-cron:0 0 6 * * MON}",
               zone  = "Asia/Kuala_Lumpur")
    public void generateUpcomingSessions() {
        log.info("[SessionScheduler] Starting weekly session generation sweep");
        try {
            GenerationSummaryResponse result = sessionGenerationService.generateDefault();

            log.info("[SessionScheduler] Complete — {} session(s) created across {} template(s) ({} to {})",
                    result.getSessionsCreated(),
                    result.getTemplatesProcessed(),
                    result.getGeneratedFrom(),
                    result.getGeneratedTo());

            if (result.getHolidayDatesSkipped() > 0) {
                log.info("[SessionScheduler] Skipped {} holiday occurrence(s): {}",
                        result.getHolidayDatesSkipped(), result.getSkippedHolidays());
            }

            if (!result.getErrors().isEmpty()) {
                log.warn("[SessionScheduler] {} template(s) had errors: {}",
                        result.getErrors().size(), result.getErrors());
            }
        } catch (Exception e) {
            log.error("[SessionScheduler] Fatal error during session generation sweep: {}", e.getMessage(), e);
        }
    }
}
