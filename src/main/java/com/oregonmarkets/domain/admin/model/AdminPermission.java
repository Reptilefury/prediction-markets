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
 * Admin Permission model - represents permissions/client roles in Keycloak
 * This can be used for caching or storing additional metadata
 *
 * The source of truth is Keycloak (stored as client roles), but we can cache here for performance
 */
@Table("admin_permissions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminPermission {

    @Id
    private UUID id;

    /**
     * Keycloak client role ID - maps to the permission in Keycloak
     */
    @Column("keycloak_role_id")
    private String keycloakRoleId;

    @Column("name")
    private String name;

    @Column("description")
    private String description;

    /**
     * Module/feature this permission belongs to
     * Examples: markets, users, transactions, compliance, settings
     */
    @Column("module")
    private String module;

    /**
     * Action/operation this permission allows
     * Examples: view, create, edit, delete, approve
     */
    @Column("action")
    private String action;

    /**
     * Whether this permission is active and can be assigned
     */
    @Column("is_active")
    @Builder.Default
    private Boolean isActive = true;

    @CreatedDate
    @Column("created_at")
    private Instant createdAt;

    @LastModifiedDate
    @Column("updated_at")
    private Instant updatedAt;

    /**
     * Timestamp of last sync with Keycloak
     */
    @Column("last_synced_at")
    private Instant lastSyncedAt;

    /**
     * Roles that have this permission
     * This is a many-to-many relationship via role_permission_mappings table
     * @Transient means this field is not persisted directly in admin_permissions table
     * It must be loaded separately via RolePermissionMappingRepository
     */
    @Transient
    @Builder.Default
    private List<AdminRole> roles = new ArrayList<>();
}
