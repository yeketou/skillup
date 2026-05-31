package com.skillup.fees.scheduler;

import com.skillup.fees.config.FeeProperties;
import com.skillup.fees.dto.GenerateFeesRequest;
import com.skillup.fees.dto.GenerateFeesResult;
import com.skillup.fees.service.FeeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Runs on the 1st of each month at 07:00 MYT and auto-generates fee records
 * for every actively enrolled student in every active class.
 *
 * <p>Uses {@link FeeService#generateMonthlyFees} which is idempotent — existing
 * records for the same student + billing month are skipped, so it is safe to
 * re-run manually via {@code POST /fees/generate} without creating duplicates.
 *
 * <p>Configure via {@code application.yml}:
 * <pre>
 * skillup:
 *   fees:
 *     generation-cron: "0 0 7 1 * *"   # 1st of month, 07:00 MYT (default)
 *     due-day-of-month: 15              # fees due on the 15th (default)
 * </pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FeeGenerationScheduler {

    private final FeeService    feeService;
    private final FeeProperties feeProperties;

    @Scheduled(cron = "${skillup.fees.generation-cron:0 0 7 1 * *}",
               zone  = "Asia/Kuala_Lumpur")
    public void generateMonthlyFees() {
        LocalDate today        = LocalDate.now();
        LocalDate billingMonth = today.withDayOfMonth(1);
        int       dueDom       = feeProperties.getDueDayOfMonth();
        LocalDate dueDate      = today.withDayOfMonth(
                Math.min(dueDom, today.lengthOfMonth())); // clamp for Feb / short months

        log.info("[FeeGenScheduler] Auto-generating fees: billingMonth={} dueDate={}", billingMonth, dueDate);
        try {
            GenerateFeesRequest req = new GenerateFeesRequest();
            req.setBillingMonth(billingMonth);
            req.setDueDate(dueDate);
            // branchId = null  →  generate for ALL branches

            GenerateFeesResult result = feeService.generateMonthlyFees(req);
            log.info("[FeeGenScheduler] Complete — created={}, skipped={}",
                    result.getCreated(), result.getSkipped());
        } catch (Exception e) {
            log.error("[FeeGenScheduler] Monthly fee generation failed: {}", e.getMessage(), e);
        }
    }
}
