package com.skillup.fees.scheduler;

import com.skillup.communication.service.NotificationService;
import com.skillup.fees.config.FeeProperties;
import com.skillup.fees.domain.FeeRecord;
import com.skillup.fees.repository.FeeRecordRepository;
import com.skillup.fees.service.FeeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Daily scheduled job that:
 * 1. Marks PENDING/PARTIAL fees past their due date as OVERDUE.
 * 2. Sends WhatsApp reminders (via NotificationService) for fees that hit
 *    a configured overdue threshold (default: 3, 5, 15 days after due date).
 *    Configure thresholds via {@code skillup.fees.reminder-days} in application.yml.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FeeReminderScheduler {

    private final FeeService           feeService;
    private final FeeRecordRepository  feeRecordRepository;
    private final FeeProperties        feeProperties;
    private final NotificationService  notificationService;

    /**
     * Runs every day at 08:00 MYT (UTC+8 = 00:00 UTC).
     */
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Kuala_Lumpur")
    public void processDailyFees() {
        LocalDate today = LocalDate.now();
        log.info("[FeeScheduler] Running daily fee sweep for {}", today);

        // 1. Mark overdue
        int marked = feeService.markOverdueFees();
        if (marked > 0) {
            log.info("[FeeScheduler] Marked {} fee record(s) as OVERDUE", marked);
        }

        // 2. Send WhatsApp reminders for fees that hit a reminder threshold
        List<FeeRecord> overdueFees = feeRecordRepository.findAllOverdue();
        for (FeeRecord fee : overdueFees) {
            long daysOverdue = ChronoUnit.DAYS.between(fee.getDueDate(), today);
            if (feeProperties.getReminderDays().contains((int) daysOverdue)) {
                sendReminder(fee, daysOverdue);
            }
        }
    }

    private void sendReminder(FeeRecord fee, long daysOverdue) {
        try {
            notificationService.sendFeeReminder(fee, daysOverdue);
        } catch (Exception ex) {
            // Never let a notification failure abort the scheduler sweep
            log.error("[FeeScheduler] Failed to send reminder for fee {} (student={}): {}",
                    fee.getId(), fee.getStudent().getStudentRef(), ex.getMessage());
        }
    }
}
