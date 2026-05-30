package com.skillup.branch.domain;

import com.skillup.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * Represents a physical tuition centre location.
 * A SkillUp installation can have one or more branches.
 */
@Entity
@Table(name = "branches")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Branch extends BaseEntity {

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    /**
     * Short uppercase code, e.g. "HQ", "PJ01".
     * Used in API filtering and display labels.
     */
    @Column(name = "code", nullable = false, length = 20, unique = true)
    private String code;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "email", length = 150)
    private String email;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;
}
