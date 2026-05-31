package com.skillup.staff.domain;

import com.skillup.branch.domain.Branch;
import com.skillup.classmanagement.domain.Subject;
import com.skillup.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * A staff member at the tuition centre — teacher, branch admin, or centre admin.
 *
 * <p>Staff members can be linked to a Keycloak account (via {@link #keycloakId})
 * to log in to the teacher portal. Call {@code activatePortalAccess()} to provision the
 * Keycloak user; until then {@link #keycloakProvisioned} is {@code false}.
 *
 * <p>Subjects they are qualified to teach are stored in the join table
 * {@code staff_subjects} and exposed via {@link #subjects}.
 */
@Entity
@Table(name = "staff")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Staff extends BaseEntity {

    /** Human-readable reference, e.g. {@code STF-2026-0001}. */
    @Column(name = "staff_ref", nullable = false, unique = true, length = 20)
    private String staffRef;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(name = "ic_number", length = 20)
    private String icNumber;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "email", length = 150)
    private String email;

    @Column(name = "whatsapp_number", length = 20)
    private String whatsappNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    @Builder.Default
    private StaffRole role = StaffRole.TEACHER;

    /** Primary branch (nullable — centre-wide admins may not belong to one branch). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // ── Keycloak portal ──────────────────────────────────────────────────────

    /** Keycloak user UUID (JWT {@code sub} claim). Set on portal activation. */
    @Column(name = "keycloak_id", unique = true)
    private UUID keycloakId;

    @Column(name = "keycloak_provisioned", nullable = false)
    @Builder.Default
    private boolean keycloakProvisioned = false;

    @Column(name = "last_login_at")
    private OffsetDateTime lastLoginAt;

    // ── Subjects ─────────────────────────────────────────────────────────────

    /** Subjects this staff member is qualified/assigned to teach. */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "staff_subjects",
            joinColumns        = @JoinColumn(name = "staff_id"),
            inverseJoinColumns = @JoinColumn(name = "subject_id")
    )
    @Builder.Default
    private Set<Subject> subjects = new HashSet<>();
}
