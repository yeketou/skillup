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
}
