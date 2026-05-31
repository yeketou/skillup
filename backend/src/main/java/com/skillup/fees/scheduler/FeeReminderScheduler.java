package com.skillup.fees.scheduler;

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
 * 2. Logs WhatsApp reminder prompts for fees that are 3, 5, or 15 days overdue
 *    (configurable via skillup.fees.reminder-days in application.yml).
 *
 * Phase 10 will wire the actual WhatsApp API call in place of the log statement.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FeeReminderScheduler {

    private final FeeService           feeService;
    private final FeeRecordRepository  feeRecordRepository;
    private final FeeProperties        feeProperties;

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

    /**
     * Placeholder for WhatsApp reminder dispatch.
     * Phase 10: replace body with actual WhatsApp API call via
     * the communication module's WhatsAppService.
     */
    private void sendReminder(FeeRecord fee, long daysOverdue) {
        String studentRef = fee.getStudent().getStudentRef();
        String className  = fee.getTemplate().getName();
        String balance    = String.format("RM %.2f", fee.getBalance());
        String dueDate    = fee.getDueDate().toString();

        log.info("[WhatsApp PENDING] student={} class='{}' balance={} dueDate={} daysOverdue={}",
                studentRef, className, balance, dueDate, daysOverdue);

        // TODO Phase 10:
        // whatsAppService.sendFeeReminder(
        //     fee.getStudent().getPrimaryParent().getWhatsappNumber(),
        //     studentRef, className, balance, dueDate, daysOverdue
        // );
    }
}
