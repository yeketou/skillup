package com.skillup.fees.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Fee-related configuration from application.yml.
 *
 * <pre>
 * skillup:
 *   fees:
 *     reminder-days: [3, 5, 15]   # days after due date to send WhatsApp reminders
 * </pre>
 */
@Configuration
@ConfigurationProperties(prefix = "skillup.fees")
@Getter
@Setter
public class FeeProperties {

    /** Days after the due date on which WhatsApp reminder messages are sent. */
    private List<Integer> reminderDays = List.of(3, 5, 15);

    /**
     * Day of the month on which auto-generated fee records are due.
     * E.g. 15 = fees generated on the 1st are due on the 15th of the same month.
     */
    private int dueDayOfMonth = 15;

    /** Cron expression for the monthly fee generation job (zone = Asia/Kuala_Lumpur). */
    private String generationCron = "0 0 7 1 * *";
}
