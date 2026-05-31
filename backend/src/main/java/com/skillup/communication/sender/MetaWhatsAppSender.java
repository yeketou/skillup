package com.skillup.communication.sender;

import com.skillup.communication.config.WhatsAppProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

/**
 * Sends messages via the Meta WhatsApp Business Cloud API.
 * Active when {@code skillup.whatsapp.enabled=true}.
 *
 * Sends free-form text messages (session messages).
 * For business-initiated template messages, upgrade to the template API
 * once Meta pre-approval is granted — wire via {@link com.skillup.communication.domain.MessageTemplate#getWaTemplateName()}.
 *
 * Ref: https://developers.facebook.com/docs/whatsapp/cloud-api/messages/text-messages
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "skillup.whatsapp.enabled", havingValue = "true")
public class MetaWhatsAppSender implements WhatsAppSender {

    private final WhatsAppProperties props;
    private final RestClient          restClient;

    public MetaWhatsAppSender(WhatsAppProperties props) {
        this.props = props;
        this.restClient = RestClient.builder()
                .baseUrl(props.getApiUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + props.getAccessToken())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public SendResult send(String recipientPhone, String message) {
        if (props.getPhoneNumberId() == null || props.getPhoneNumberId().isBlank()) {
            return SendResult.fail("WhatsApp phone-number-id is not configured");
        }

        // Meta Cloud API request body (text message)
        Map<String, Object> body = Map.of(
                "messaging_product", "whatsapp",
                "recipient_type",    "individual",
                "to",                recipientPhone,
                "type",              "text",
                "text",              Map.of("preview_url", false, "body", message)
        );

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.post()
                    .uri("/{phoneNumberId}/messages", props.getPhoneNumberId())
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            // Extract "wamid.XXX" from response.messages[0].id
            String messageId = extractMessageId(response);
            log.info("[WhatsApp] Sent to {} — messageId={}", recipientPhone, messageId);
            return SendResult.ok(messageId);

        } catch (RestClientException ex) {
            String error = ex.getMessage();
            log.error("[WhatsApp] Failed to send to {}: {}", recipientPhone, error);
            return SendResult.fail(error);
        }
    }

    @SuppressWarnings("unchecked")
    private String extractMessageId(Map<String, Object> response) {
        if (response == null) return null;
        Object messages = response.get("messages");
        if (messages instanceof List<?> list && !list.isEmpty()) {
            Object first = list.get(0);
            if (first instanceof Map<?, ?> m) {
                Object id = m.get("id");
                return id != null ? id.toString() : null;
            }
        }
        return null;
    }
}
