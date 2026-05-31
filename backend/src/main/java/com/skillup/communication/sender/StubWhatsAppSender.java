package com.skillup.communication.sender;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Development stub — logs the message instead of calling the real API.
 * Active when {@code skillup.whatsapp.enabled=false} (the default for local dev).
 *
 * Every call returns a fake success so callers can proceed normally and
 * logs are still written to the communication_logs table.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "skillup.whatsapp.enabled", havingValue = "false", matchIfMissing = true)
public class StubWhatsAppSender implements WhatsAppSender {

    @Override
    public SendResult send(String recipientPhone, String message) {
        String fakeId = "STUB-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        log.info("""
                ╔══ [WhatsApp STUB] ══════════════════════════════════
                ║ To      : {}
                ║ Msg ID  : {}
                ╠═════════════════════════════════════════════════════
                {}
                ╚═════════════════════════════════════════════════════""",
                recipientPhone, fakeId, message);
        return SendResult.ok(fakeId);
    }
}
