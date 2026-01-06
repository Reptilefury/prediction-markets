package com.oregonmarkets.domain.admin.repository;

import com.oregonmarkets.domain.admin.model.AdminRole;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface AdminRoleRepository extends R2dbcRepository<AdminRole, UUID> {

    Mono<AdminRole> findByKeycloakRoleId(String keycloakRoleId);

    Mono<AdminRole> findByName(String name);

    Flux<AdminRole> findByIsActive(Boolean isActive);

    @Query("SELECT * FROM admin_roles WHERE is_active = true ORDER BY priority ASC")
    Flux<AdminRole> findActiveRolesOrderedByPriority();
}
