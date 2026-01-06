package com.oregonmarkets.domain.admin.model;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Admin User model - represents admin users in the system
 * Integrates with Keycloak for authentication and role management
 */
@Table("admin_users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminUser {

    @Id
    private UUID id;

    /**
     * Keycloak user ID - maps to the user in Keycloak
     */
    @Column("keycloak_user_id")
    private String keycloakUserId;

    @Column("first_name")
    private String firstName;

    @Column("last_name")
    private String lastName;

    @Column("username")
    private String username;

    @Column("email")
    private String email;

    @Column("phone")
    private String phone;

    /**
     * Admin role ID - references admin_roles table
     */
    @Column("role_id")
    private UUID roleId;

    @Column("status")
    @Builder.Default
    private AdminUserStatus status = AdminUserStatus.ACTIVE;

    @Column("two_factor_enabled")
    @Builder.Default
    private Boolean twoFactorEnabled = false;

    @Column("last_login_at")
    private Instant lastLoginAt;

    @Column("login_attempts")
    @Builder.Default
    private Integer loginAttempts = 0;

    @Column("locked_until")
    private Instant lockedUntil;

    @CreatedDate
    @Column("created_at")
    private Instant createdAt;

    @LastModifiedDate
    @Column("updated_at")
    private Instant updatedAt;

    /**
     * Role information - loaded separately
     */
    @Transient
    private AdminRole role;

    /**
     * User permissions - derived from role
     */
    @Transient
    @Builder.Default
    private List<AdminPermission> permissions = new ArrayList<>();

    public enum AdminUserStatus {
        ACTIVE,
        INACTIVE,
        SUSPENDED
    }
}
