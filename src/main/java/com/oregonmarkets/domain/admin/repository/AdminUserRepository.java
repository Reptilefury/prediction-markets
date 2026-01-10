package com.oregonmarkets.domain.admin.repository;

import com.oregonmarkets.domain.admin.model.AdminUser;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface AdminUserRepository extends ReactiveCrudRepository<AdminUser, UUID> {

    Mono<AdminUser> findByEmail(String email);

    Mono<AdminUser> findByUsername(String username);

    Mono<AdminUser> findByKeycloakUserId(String keycloakUserId);

    Flux<AdminUser> findByStatus(AdminUser.AdminUserStatus status);

    Flux<AdminUser> findByRoleId(UUID roleId);

    @Query("SELECT * FROM admin_users WHERE name ILIKE :searchTerm OR email ILIKE :searchTerm")
    Flux<AdminUser> findByNameOrEmailContainingIgnoreCase(String searchTerm);

    Mono<Boolean> existsByEmail(String email);

    Mono<Boolean> existsByUsername(String username);

    Mono<Boolean> existsByKeycloakUserId(String keycloakUserId);
}
