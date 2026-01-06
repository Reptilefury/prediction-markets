package com.oregonmarkets.domain.admin.repository;

import com.oregonmarkets.domain.admin.model.AdminPermission;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface AdminPermissionRepository extends R2dbcRepository<AdminPermission, UUID> {

    Mono<AdminPermission> findByKeycloakRoleId(String keycloakRoleId);

    Mono<AdminPermission> findByName(String name);

    Flux<AdminPermission> findByModule(String module);

    Flux<AdminPermission> findByAction(String action);

    Flux<AdminPermission> findByModuleAndAction(String module, String action);

    Flux<AdminPermission> findByIsActive(Boolean isActive);

    @Query("SELECT * FROM admin_permissions WHERE is_active = true ORDER BY module, action")
    Flux<AdminPermission> findActivePermissionsOrdered();

    @Query("SELECT ap.* FROM admin_permissions ap " +
           "JOIN role_permission_mappings rpm ON ap.id = rpm.permission_id " +
           "WHERE rpm.role_id = :roleId AND ap.is_active = true " +
           "ORDER BY ap.module, ap.action")
    Flux<AdminPermission> findByRoleId(UUID roleId);
}   
