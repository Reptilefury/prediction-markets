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
 * Admin Role model - represents roles in Keycloak
 * This can be used for caching or storing additional metadata
 *
 * The source of truth is Keycloak, but we can cache here for performance
 */
@Table("admin_roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminRole {

    @Id
    private UUID id;

    /**
     * Keycloak role ID - maps to the role in Keycloak
     */
    @Column("keycloak_role_id")
    private String keycloakRoleId;

    @Column("name")
    private String name;

    @Column("description")
    private String description;

    /**
     * Whether this is a composite role (contains other roles)
     */
    @Column("is_composite")
    @Builder.Default
    private Boolean isComposite = false;

    /**
     * Priority/hierarchy level (e.g., super_admin = 1, admin = 2, etc.)
     */
    @Column("priority")
    private Integer priority;

    /**
     * Whether this role is active and can be assigned
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
     * Permissions associated with this role
     * This is a many-to-many relationship via role_permission_mappings table
     * @Transient means this field is not persisted directly in admin_roles table
     * It must be loaded separately via RolePermissionMappingRepository
     */
    @Transient
    @Builder.Default
    private List<AdminPermission> permissions = new ArrayList<>();
}
