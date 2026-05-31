package com.skillup.communication.domain;

import com.skillup.common.BaseEntity;
import com.skillup.student.domain.Student;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

/**
 * Immutable audit record of every outbound WhatsApp message attempt.
 * Created before dispatch and updated with the provider's response.
 */
@Entity
@Table(name = "communication_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommunicationLog extends BaseEntity {

    /** Linked student — null for ad-hoc admin messages. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id")
    private Student student;

    /** Template used to render this message — null for ad-hoc plain-text sends. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id")
    private MessageTemplate template;

    /** WhatsApp number with country code, no '+': e.g. "60123456789". */
    @Column(name = "recipient_phone", nullable = false, length = 30)
    private String recipientPhone;

    @Column(name = "recipient_name", length = 200)
    private String recipientName;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_category", nullable = false, length = 50)
    private MessageCategory messageCategory;

    /** The fully rendered message that was sent. */
    @Column(name = "rendered_content", nullable = false, columnDefinition = "TEXT")
    private String renderedContent;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private MessageStatus status = MessageStatus.PENDING;

    @Column(name = "sent_at")
    private OffsetDateTime sentAt;

    /** "wamid.XXX" returned by Meta — used to match delivery webhooks later. */
    @Column(name = "provider_message_id", length = 200)
    private String providerMessageId;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
}
