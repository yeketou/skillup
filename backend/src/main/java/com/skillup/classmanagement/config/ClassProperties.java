package com.skillup.classmanagement.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Class-management configuration from application.yml.
 *
 * <pre>
 * skillup:
 *   classes:
 *     lookahead-weeks: 4               # how many weeks ahead the scheduler generates sessions
 *     generation-cron: "0 0 6 * * MON" # every Monday at 06:00 MYT
 * </pre>
 */
@Configuration
@ConfigurationProperties(prefix = "skillup.classes")
@Getter
@Setter
public class ClassProperties {

    /** How many weeks ahead the weekly scheduler should generate sessions. Default 4. */
    private int lookaheadWeeks = 4;

    /** Cron expression for the session generation job (Spring zone = Asia/Kuala_Lumpur). */
    private String generationCron = "0 0 6 * * MON";
}
