package com.skillup.communication.sender;

/**
 * Strategy interface for outbound WhatsApp message delivery.
 *
 * <ul>
 *   <li>{@link StubWhatsAppSender} — logs to console; used in local/dev ({@code skillup.whatsapp.enabled=false})</li>
 *   <li>{@link MetaWhatsAppSender} — calls Meta Cloud API; used in prod ({@code skillup.whatsapp.enabled=true})</li>
 * </ul>
 */
public interface WhatsAppSender {

    /**
     * Send a plain-text message to a WhatsApp number.
     *
     * @param recipientPhone phone with country code, no '+' — e.g. "60123456789"
     * @param message        rendered message body (max ~4096 chars for WhatsApp)
     * @return result containing success flag, provider message ID, and any error detail
     */
    SendResult send(String recipientPhone, String message);

    /** Immutable result of a send attempt. */
    record SendResult(boolean success, String messageId, String errorMessage) {
        public static SendResult ok(String messageId) {
            return new SendResult(true, messageId, null);
        }
        public static SendResult fail(String errorMessage) {
            return new SendResult(false, null, errorMessage);
        }
    }
}
