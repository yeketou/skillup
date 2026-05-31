package com.skillup.student.domain;

import com.skillup.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Portal login account — one per primary parent email.
 *
 * Both the parent AND the student share these credentials.
 * A single portal account can link to multiple students (siblings at the same centre).
 *
 * keycloak_id is null until the admin "activates" the portal account
 * (triggers Keycloak user creation and sends a welcome email with a one-time password).
 */
@Entity
@Table(name = "student_portal_accounts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentPortalAccount extends BaseEntity {

    @Column(name = "keycloak_id")
    private UUID keycloakId;

    @Column(name = "email", nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "last_login_at")
    private OffsetDateTime lastLoginAt;

    /**
     * Students linked to this portal account.
     * Loaded lazily — use only when you need the full sibling list.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "portal_account_students",
        joinColumns = @JoinColumn(name = "portal_account_id"),
        inverseJoinColumns = @JoinColumn(name = "student_id")
    )
    @Builder.Default
    private List<Student> students = new ArrayList<>();

    public boolean isKeycloakProvisioned() {
        return keycloakId != null;
    }
}
