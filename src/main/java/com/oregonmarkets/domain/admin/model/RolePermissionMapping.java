package com.oregonmarkets.domain.admin.model;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Role-Permission mapping model
 * Represents the many-to-many relationship between roles and permissions
 *
 * This is used for caching the Keycloak role-permission mappings
 * The source of truth is still Keycloak
 */
@Table("role_permission_mappings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RolePermissionMapping {

    @Id
    private UUID id;

    @Column("role_id")
    private UUID roleId;

    @Column("permission_id")
    private UUID permissionId;

    @CreatedDate
    @Column("created_at")
    private Instant createdAt;

    /**
     * Timestamp of last sync with Keycloak
     */
    @Column("last_synced_at")
    private Instant lastSyncedAt;
}
