package com.skillup.communication.domain;

import com.skillup.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * A reusable WhatsApp message template with {@code {{placeholder}}} tokens.
 *
 * <p>Tokens are resolved at send time by the {@link com.skillup.communication.service.NotificationService}.
 * Common tokens: {@code {{parentName}}}, {@code {{studentName}}}, {@code {{studentRef}}},
 * {@code {{className}}}, {@code {{amount}}}, {@code {{balance}}}, {@code {{dueDate}}}.
 *
 * <p>For Meta WhatsApp Business API production use, {@code waTemplateName} must match
 * a pre-approved template in the Meta Business Manager.
 */
@Entity
@Table(name = "message_templates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageTemplate extends BaseEntity {

    /** Internal code used in service calls — e.g. {@code "FEE_OVERDUE"}. */
    @Column(name = "name", nullable = false, length = 100, unique = true)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 50)
    private MessageCategory category;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /** Message body with {@code {{token}}} placeholders. Supports Unicode / emoji. */
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    /**
     * Meta WhatsApp Business Manager pre-approved template name.
     * Required for business-initiated conversations in production.
     * Null = send as free-form text (session message).
     */
    @Column(name = "wa_template_name", length = 100)
    private String waTemplateName;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Renders the template by replacing all {@code {{key}}} tokens with values
     * from the provided map. Unknown tokens are left unchanged.
     */
    public String render(java.util.Map<String, String> variables) {
        String rendered = this.content;
        for (var entry : variables.entrySet()) {
            if (entry.getValue() != null) {
                rendered = rendered.replace("{{" + entry.getKey() + "}}", entry.getValue());
            }
        }
        return rendered;
    }
}
