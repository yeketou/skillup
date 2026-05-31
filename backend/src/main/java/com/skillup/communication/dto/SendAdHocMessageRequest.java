package com.skillup.communication.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.UUID;

/** Ad-hoc plain-text WhatsApp message — bypasses templates. */
@Data
public class SendAdHocMessageRequest {

    /** Optional: link this message to a student for history tracking. */
    private UUID studentId;

    /**
     * WhatsApp number including country code, no '+'.
     * Malaysian format: 601X-XXXXXXX → "601XXXXXXXX"
     */
    @NotBlank(message = "Recipient phone is required")
    @Pattern(regexp = "\\d{10,15}", message = "Phone must be 10-15 digits, country code included, no '+' or spaces")
    private String recipientPhone;

    private String recipientName;

    @NotBlank(message = "Message content is required")
    private String message;
}
