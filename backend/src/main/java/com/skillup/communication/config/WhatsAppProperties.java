package com.skillup.communication.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Binds {@code skillup.whatsapp.*} from application.yml.
 *
 * <pre>
 * skillup:
 *   whatsapp:
 *     enabled: false          # true in prod (set WHATSAPP_ENABLED=true)
 *     api-url: https://graph.facebook.com/v19.0
 *     phone-number-id: ""     # from Meta Business Manager
 *     access-token: ""        # permanent / long-lived system user token
 *     from-name: "SkillUp Tuition Centre"
 * </pre>
 */
@Configuration
@ConfigurationProperties(prefix = "skillup.whatsapp")
@Getter
@Setter
public class WhatsAppProperties {

    /** When false the StubWhatsAppSender is used — no real messages sent. */
    private boolean enabled = false;

    private String apiUrl     = "https://graph.facebook.com/v19.0";
    private String phoneNumberId = "";
    private String accessToken   = "";
    private String fromName      = "SkillUp Tuition Centre";
}
