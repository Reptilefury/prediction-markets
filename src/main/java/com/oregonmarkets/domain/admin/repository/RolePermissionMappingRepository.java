package com.oregonmarkets.domain.admin.repository;

import com.oregonmarkets.domain.admin.model.RolePermissionMapping;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface RolePermissionMappingRepository extends R2dbcRepository<RolePermissionMapping, UUID> {

    Flux<RolePermissionMapping> findByRoleId(UUID roleId);

    Flux<RolePermissionMapping> findByPermissionId(UUID permissionId);

    Mono<RolePermissionMapping> findByRoleIdAndPermissionId(UUID roleId, UUID permissionId);

    @Query("DELETE FROM role_permission_mappings WHERE role_id = :roleId")
    Mono<Void> deleteByRoleId(UUID roleId);

    @Query("SELECT DISTINCT p.* FROM admin_permissions p " +
           "JOIN role_permission_mappings rpm ON p.id = rpm.permission_id " +
           "WHERE rpm.role_id = :roleId AND p.is_active = true")
    Flux<UUID> findPermissionIdsByRoleId(UUID roleId);
}
