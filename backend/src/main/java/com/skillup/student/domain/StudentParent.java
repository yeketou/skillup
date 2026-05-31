package com.skillup.student.domain;

import com.skillup.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * Parent or guardian contact linked to a student.
 *
 * The primary parent's email becomes the Student Portal login username.
 * WhatsApp fee reminders are sent to the primary parent's whatsapp_number.
 */
@Entity
@Table(name = "student_parents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentParent extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Enumerated(EnumType.STRING)
    @Column(name = "relationship", nullable = false, length = 30)
    @Builder.Default
    private ParentRelationship relationship = ParentRelationship.PARENT;

    /**
     * TRUE = this is the primary billing contact.
     * Fee receipts, WhatsApp reminders, and portal login are tied to this person.
     */
    @Column(name = "is_primary", nullable = false)
    @Builder.Default
    private boolean primary = true;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(name = "ic_number", length = 20)
    private String icNumber;

    @Column(name = "phone", nullable = false, length = 20)
    private String phone;

    @Column(name = "email", length = 150)
    private String email;

    /**
     * WhatsApp number — defaults to phone if not specified.
     * Include country code without '+': e.g. "60123456789"
     */
    @Column(name = "whatsapp_number", length = 20)
    private String whatsappNumber;

    @Column(name = "preferred_language", nullable = false, length = 10)
    @Builder.Default
    private String preferredLanguage = "MS";    // MS = Bahasa Malaysia | EN = English

    // ── Helpers ───────────────────────────────────────────────────────────────

    public String effectiveWhatsappNumber() {
        return (whatsappNumber != null && !whatsappNumber.isBlank())
                ? whatsappNumber
                : phone;
    }
}
