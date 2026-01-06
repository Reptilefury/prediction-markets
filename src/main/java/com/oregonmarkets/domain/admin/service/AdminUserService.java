package com.oregonmarkets.domain.admin.service;

import com.oregonmarkets.domain.admin.dto.request.CreateAdminUserRequest;
import com.oregonmarkets.domain.admin.dto.request.UpdateAdminUserRequest;
import com.oregonmarkets.domain.admin.dto.response.AdminUserResponse;
import com.oregonmarkets.domain.admin.exception.AdminUserAlreadyExistsException;
import com.oregonmarkets.domain.admin.exception.AdminUserNotFoundException;
import com.oregonmarkets.domain.admin.exception.AdminRoleNotFoundException;
import com.oregonmarkets.domain.admin.model.AdminUser;
import com.oregonmarkets.domain.admin.repository.AdminUserRepository;
import com.oregonmarkets.domain.admin.repository.AdminRoleRepository;
import com.oregonmarkets.domain.admin.repository.AdminPermissionRepository;
import com.oregonmarkets.integration.keycloak.KeycloakAdminClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminUserService {

    private final AdminUserRepository adminUserRepository;
    private final AdminRoleRepository adminRoleRepository;
    private final AdminPermissionRepository adminPermissionRepository;
    private final KeycloakAdminClient keycloakAdminClient;

    @Transactional
    public Mono<AdminUserResponse> createAdminUser(CreateAdminUserRequest request) {
        log.info("Creating admin user with email: {}", request.getEmail());
        
        return adminUserRepository.existsByEmail(request.getEmail())
            .flatMap(exists -> {
                if (exists) {
                    return Mono.error(new AdminUserAlreadyExistsException(request.getEmail()));
                }
                
                // Verify role exists
                return adminRoleRepository.findById(request.getRoleId())
                    .switchIfEmpty(Mono.error(new AdminRoleNotFoundException(request.getRoleId().toString())))
                    .flatMap(role -> {
                        // Generate username: first letter of firstName + lastName (lowercase)
                        String username = generateUsername(request.getFirstName(), request.getLastName());
                        
                        AdminUser adminUser = AdminUser.builder()
                            .firstName(request.getFirstName())
                            .lastName(request.getLastName())
                            .username(username)
                            .email(request.getEmail())
                            .phone(request.getPhone())
                            .roleId(request.getRoleId())
                            .status(request.getStatus())
                            .twoFactorEnabled(request.getTwoFactorEnabled())
                            .build();
                        
                        // Create user in Keycloak first
                        return createKeycloakUser(adminUser, role.getName())
                            .flatMap(keycloakUserId -> {
                                adminUser.setKeycloakUserId(keycloakUserId);
                                return adminUserRepository.save(adminUser);
                            });
                    });
            })
            .flatMap(this::mapToResponse);
    }

    private Mono<String> createKeycloakUser(AdminUser adminUser, String roleName) {
        Map<String, Object> userRepresentation = new HashMap<>();
        userRepresentation.put("username", adminUser.getUsername());
        userRepresentation.put("firstName", adminUser.getFirstName());
        userRepresentation.put("lastName", adminUser.getLastName());
        userRepresentation.put("email", adminUser.getEmail());
        userRepresentation.put("emailVerified", true);
        userRepresentation.put("enabled", adminUser.getStatus() == AdminUser.AdminUserStatus.ACTIVE);
        
        return keycloakAdminClient.createAdminUser(userRepresentation)
            .flatMap(keycloakUserId -> {
                // Assign the role to the user in Keycloak
                return keycloakAdminClient.getRoleByName(roleName)
                    .flatMap(roleMap -> {
                        java.util.List<Map<String, Object>> roles = java.util.List.of(roleMap);
                        return keycloakAdminClient.assignRealmRolesToUser(keycloakUserId, roles)
                            .thenReturn(keycloakUserId);
                    });
            })
            .doOnSuccess(keycloakUserId -> log.info("Created Keycloak user with ID: {}", keycloakUserId))
            .doOnError(e -> log.error("Failed to create Keycloak user for {}: {}", adminUser.getUsername(), e.getMessage()));
    }

    private String generateUsername(String firstName, String lastName) {
        return (firstName.substring(0, 1) + lastName).toLowerCase().replaceAll("\\s+", "");
    }

    public Mono<AdminUserResponse> getAdminUser(UUID id) {
        return adminUserRepository.findById(id)
            .switchIfEmpty(Mono.error(new AdminUserNotFoundException(id.toString())))
            .flatMap(this::mapToResponse);
    }

    public Flux<AdminUserResponse> getAllAdminUsers() {
        return adminUserRepository.findAll()
            .flatMap(this::mapToResponse);
    }

    public Flux<AdminUserResponse> searchAdminUsers(String searchTerm) {
        return adminUserRepository.findByNameOrEmailContainingIgnoreCase("%" + searchTerm + "%")
            .flatMap(this::mapToResponse);
    }

    @Transactional
    public Mono<AdminUserResponse> updateAdminUser(UUID id, UpdateAdminUserRequest request) {
        return adminUserRepository.findById(id)
            .switchIfEmpty(Mono.error(new AdminUserNotFoundException(id.toString())))
            .flatMap(adminUser -> {
                if (request.getFirstName() != null) adminUser.setFirstName(request.getFirstName());
                if (request.getLastName() != null) adminUser.setLastName(request.getLastName());
                
                // Regenerate username if first or last name changed
                if (request.getFirstName() != null || request.getLastName() != null) {
                    String username = generateUsername(adminUser.getFirstName(), adminUser.getLastName());
                    adminUser.setUsername(username);
                }
                
                if (request.getEmail() != null) adminUser.setEmail(request.getEmail());
                if (request.getPhone() != null) adminUser.setPhone(request.getPhone());
                if (request.getRoleId() != null) adminUser.setRoleId(request.getRoleId());
                if (request.getTwoFactorEnabled() != null) adminUser.setTwoFactorEnabled(request.getTwoFactorEnabled());
                if (request.getStatus() != null) adminUser.setStatus(request.getStatus());
                
                return adminUserRepository.save(adminUser);
            })
            .flatMap(this::mapToResponse);
    }

    @Transactional
    public Mono<Void> deleteAdminUser(UUID id) {
        return adminUserRepository.findById(id)
            .switchIfEmpty(Mono.error(new AdminUserNotFoundException(id.toString())))
            .flatMap(adminUser -> {
                // Delete from Keycloak if keycloakUserId exists
                if (adminUser.getKeycloakUserId() != null) {
                    return keycloakAdminClient.deleteUser(adminUser.getKeycloakUserId())
                        .then(adminUserRepository.delete(adminUser));
                } else {
                    return adminUserRepository.delete(adminUser);
                }
            });
    }

    public Mono<Void> updateLastLogin(UUID id) {
        return adminUserRepository.findById(id)
            .switchIfEmpty(Mono.error(new AdminUserNotFoundException(id.toString())))
            .flatMap(adminUser -> {
                adminUser.setLastLoginAt(Instant.now());
                adminUser.setLoginAttempts(0);
                return adminUserRepository.save(adminUser);
            })
            .then();
    }

    private Mono<AdminUserResponse> mapToResponse(AdminUser adminUser) {
        return adminRoleRepository.findById(adminUser.getRoleId())
            .flatMap(role -> 
                adminPermissionRepository.findByRoleId(role.getId())
                    .map(permission -> AdminUserResponse.AdminPermissionResponse.builder()
                        .id(permission.getId())
                        .name(permission.getName())
                        .description(permission.getDescription())
                        .module(permission.getModule())
                        .action(permission.getAction())
                        .build())
                    .collectList()
                    .map(permissions -> AdminUserResponse.builder()
                        .id(adminUser.getId())
                        .firstName(adminUser.getFirstName())
                        .lastName(adminUser.getLastName())
                        .username(adminUser.getUsername())
                        .email(adminUser.getEmail())
                        .phone(adminUser.getPhone())
                        .roleId(adminUser.getRoleId())
                        .roleName(role.getName())
                        .status(adminUser.getStatus())
                        .twoFactorEnabled(adminUser.getTwoFactorEnabled())
                        .lastLoginAt(adminUser.getLastLoginAt())
                        .loginAttempts(adminUser.getLoginAttempts())
                        .createdAt(adminUser.getCreatedAt())
                        .updatedAt(adminUser.getUpdatedAt())
                        .permissions(permissions)
                        .build())
            )
            .switchIfEmpty(Mono.just(AdminUserResponse.builder()
                .id(adminUser.getId())
                .firstName(adminUser.getFirstName())
                .lastName(adminUser.getLastName())
                .username(adminUser.getUsername())
                .email(adminUser.getEmail())
                .phone(adminUser.getPhone())
                .roleId(adminUser.getRoleId())
                .status(adminUser.getStatus())
                .twoFactorEnabled(adminUser.getTwoFactorEnabled())
                .lastLoginAt(adminUser.getLastLoginAt())
                .loginAttempts(adminUser.getLoginAttempts())
                .createdAt(adminUser.getCreatedAt())
                .updatedAt(adminUser.getUpdatedAt())
                .build()));
    }
}
