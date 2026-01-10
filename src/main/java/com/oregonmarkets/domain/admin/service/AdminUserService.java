package com.oregonmarkets.domain.admin.service;

import com.oregonmarkets.domain.admin.dto.request.CreateAdminUserRequest;
import com.oregonmarkets.domain.admin.dto.request.UpdateAdminUserRequest;
import com.oregonmarkets.domain.admin.dto.response.AdminUserResponse;
import com.oregonmarkets.domain.admin.dto.response.AdminUserStatsResponse;
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
import java.util.Collections;
import java.util.List;

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
        log.info("Updating admin user with ID: {}", id);
        
        return validateUpdateRequest(id, request)
            .flatMap(adminUser -> applyUpdates(adminUser, request))
            .flatMap(adminUserRepository::save)
            .flatMap(this::mapToResponse);
    }

    private Mono<AdminUser> validateUpdateRequest(UUID id, UpdateAdminUserRequest request) {
        return adminUserRepository.findById(id)
            .switchIfEmpty(Mono.error(new AdminUserNotFoundException(id.toString())))
            .flatMap(adminUser -> validateEmailUniqueness(adminUser, request)
                .then(validateRoleExists(request))
                .then(validateBusinessRules(adminUser, request))
                .thenReturn(adminUser));
    }

    private Mono<Void> validateEmailUniqueness(AdminUser currentUser, UpdateAdminUserRequest request) {
        if (request.getEmail() == null || request.getEmail().equals(currentUser.getEmail())) {
            return Mono.empty();
        }
        
        return adminUserRepository.existsByEmail(request.getEmail())
            .flatMap(exists -> {
                if (exists) {
                    return Mono.error(new AdminUserAlreadyExistsException(request.getEmail()));
                }
                return Mono.empty();
            });
    }

    private Mono<Void> validateRoleExists(UpdateAdminUserRequest request) {
        if (request.getRoleId() == null) {
            return Mono.empty();
        }
        
        return adminRoleRepository.existsById(request.getRoleId())
            .flatMap(exists -> {
                if (!exists) {
                    return Mono.error(new AdminRoleNotFoundException(request.getRoleId().toString()));
                }
                return Mono.empty();
            });
    }

    private Mono<Void> validateBusinessRules(AdminUser currentUser, UpdateAdminUserRequest request) {
        // Prevent self-deactivation/suspension
        // Note: This would require getting current user context, for now we'll skip this validation
        
        // Validate phone format if provided
        if (request.getPhone() != null && !request.getPhone().trim().isEmpty()) {
            if (!isValidPhoneFormat(request.getPhone())) {
                return Mono.error(new IllegalArgumentException("Invalid phone number format"));
            }
        }
        
        // Validate name fields are not empty if provided
        if (request.getFirstName() != null && request.getFirstName().trim().isEmpty()) {
            return Mono.error(new IllegalArgumentException("First name cannot be empty"));
        }
        
        if (request.getLastName() != null && request.getLastName().trim().isEmpty()) {
            return Mono.error(new IllegalArgumentException("Last name cannot be empty"));
        }
        
        return Mono.empty();
    }

    private boolean isValidPhoneFormat(String phone) {
        // Basic phone validation - starts with + and contains only digits, spaces, hyphens, parentheses
        return phone.matches("^\\+?[0-9\\s\\-\\(\\)]{7,15}$");
    }

    private Mono<AdminUser> applyUpdates(AdminUser adminUser, UpdateAdminUserRequest request) {
        if (request.getFirstName() != null) {
            adminUser.setFirstName(request.getFirstName().trim());
        }
        if (request.getLastName() != null) {
            adminUser.setLastName(request.getLastName().trim());
        }
        
        // Regenerate username if first or last name changed
        if (request.getFirstName() != null || request.getLastName() != null) {
            String username = generateUsername(adminUser.getFirstName(), adminUser.getLastName());
            // Check if username would be unique
            return validateUsernameUniqueness(adminUser.getId(), username)
                .then(Mono.fromCallable(() -> {
                    adminUser.setUsername(username);
                    return adminUser;
                }))
                .flatMap(user -> applyRemainingUpdates(user, request));
        }
        
        return applyRemainingUpdates(adminUser, request);
    }

    private Mono<Void> validateUsernameUniqueness(UUID currentUserId, String username) {
        return adminUserRepository.findByUsername(username)
            .flatMap(existingUser -> {
                if (!existingUser.getId().equals(currentUserId)) {
                    return Mono.error(new IllegalArgumentException("Generated username already exists"));
                }
                return Mono.empty();
            })
            .switchIfEmpty(Mono.empty()).then();
    }

    private Mono<AdminUser> applyRemainingUpdates(AdminUser adminUser, UpdateAdminUserRequest request) {
        boolean statusChanged = false;
        AdminUser.AdminUserStatus oldStatus = adminUser.getStatus();
        
        if (request.getEmail() != null) {
            adminUser.setEmail(request.getEmail().trim().toLowerCase());
        }
        if (request.getPhone() != null) {
            adminUser.setPhone(request.getPhone().trim());
        }
        if (request.getRoleId() != null) {
            adminUser.setRoleId(request.getRoleId());
        }
        if (request.getTwoFactorEnabled() != null) {
            adminUser.setTwoFactorEnabled(request.getTwoFactorEnabled());
        }
        if (request.getStatus() != null) {
            statusChanged = !request.getStatus().equals(oldStatus);
            adminUser.setStatus(request.getStatus());
        }
        
        adminUser.setUpdatedAt(Instant.now());
        
        // Sync with Keycloak if status changed and user has Keycloak ID
        if (statusChanged && adminUser.getKeycloakUserId() != null) {
            return syncUserStatusWithKeycloak(adminUser)
                .thenReturn(adminUser);
        }
        
        return Mono.just(adminUser);
    }

    private Mono<Void> syncUserStatusWithKeycloak(AdminUser adminUser) {
        log.info("Syncing user status with Keycloak for user: {}", adminUser.getUsername());
        
        Map<String, Object> userUpdate = new HashMap<>();
        userUpdate.put("enabled", adminUser.getStatus() == AdminUser.AdminUserStatus.ACTIVE);
        
        return keycloakAdminClient.updateUser(adminUser.getKeycloakUserId(), userUpdate)
            .doOnSuccess(v -> log.info("Successfully synced status for user {} in Keycloak", adminUser.getUsername()))
            .doOnError(e -> log.error("Failed to sync status for user {} in Keycloak: {}", adminUser.getUsername(), e.getMessage()));
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
                // Get permissions from Keycloak composites instead of local DB
                keycloakAdminClient.getRoleComposites(role.getName())
                    .defaultIfEmpty(Collections.emptyList())
                    .map(composites -> {
                        List<AdminUserResponse.AdminPermissionResponse> permissions = composites.stream()
                                .filter(composite -> Boolean.TRUE.equals(composite.get("clientRole")))
                                .map(composite -> AdminUserResponse.AdminPermissionResponse.builder()
                                    .id(UUID.fromString((String) composite.get("id")))
                                    .name((String) composite.get("name"))
                                    .description((String) composite.get("description"))
                                    .build())
                                .toList();
                        
                        return AdminUserResponse.builder()
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
                            .build();
                    })
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

    public Mono<AdminUserStatsResponse> getAdminUserStatistics() {
        log.info("Retrieving admin user statistics");
        
        return adminUserRepository.findAll()
            .collectList()
            .map(users -> {
                long total = users.size();
                long active = users.stream().filter(u -> u.getStatus() == AdminUser.AdminUserStatus.ACTIVE).count();
                long inactive = users.stream().filter(u -> u.getStatus() == AdminUser.AdminUserStatus.INACTIVE).count();
                long suspended = users.stream().filter(u -> u.getStatus() == AdminUser.AdminUserStatus.SUSPENDED).count();
                long twoFactorEnabled = users.stream().filter(AdminUser::getTwoFactorEnabled).count();
                
                return AdminUserStatsResponse.builder()
                    .total(total)
                    .active(active)
                    .inactive(inactive)
                    .suspended(suspended)
                    .twoFactorEnabled(twoFactorEnabled)
                    .build();
            });
    }

    @Transactional
    public Mono<AdminUserResponse> updateUserStatus(UUID id, AdminUser.AdminUserStatus newStatus) {
        log.info("Updating user status for ID: {} to {}", id, newStatus);
        
        return adminUserRepository.findById(id)
            .switchIfEmpty(Mono.error(new AdminUserNotFoundException(id.toString())))
            .flatMap(adminUser -> {
                adminUser.setStatus(newStatus);
                adminUser.setUpdatedAt(Instant.now());
                
                // Sync with Keycloak if user has Keycloak ID
                Mono<Void> keycloakSync = adminUser.getKeycloakUserId() != null 
                    ? syncUserStatusWithKeycloak(adminUser)
                    : Mono.empty();
                
                return keycloakSync
                    .then(adminUserRepository.save(adminUser))
                    .flatMap(this::mapToResponse);
            });
    }
}
