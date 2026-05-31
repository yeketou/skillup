package com.skillup.communication.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Send a named template to a recipient, with placeholder values resolved at send time. */
@Data
public class SendTemplatedMessageRequest {

    /** Internal template name, e.g. "FEE_REMINDER". */
    @NotBlank(message = "Template name is required")
    private String templateName;

    /** Optional link to a student for message history. */
    private UUID studentId;

    @NotBlank(message = "Recipient phone is required")
    @Pattern(regexp = "\\d{10,15}", message = "Phone must be 10-15 digits, country code included, no '+'")
    private String recipientPhone;

    private String recipientName;

    /**
     * Placeholder values, e.g. {@code {"studentName": "Ahmad", "amount": "RM 150.00"}}.
     * Keys correspond to {@code {{token}}} names in the template content.
     */
    private Map<String, String> variables = new HashMap<>();
}
