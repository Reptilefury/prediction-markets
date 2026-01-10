package com.oregonmarkets.domain.admin.service;

import com.oregonmarkets.domain.admin.dto.request.CreateRoleRequest;
import com.oregonmarkets.domain.admin.dto.request.UpdateRoleRequest;
import com.oregonmarkets.domain.admin.dto.response.PermissionResponse;
import com.oregonmarkets.domain.admin.dto.response.RoleResponse;
import com.oregonmarkets.domain.admin.exception.AdminRoleNotFoundException;
import com.oregonmarkets.domain.admin.exception.AdminRoleInUseException;
import com.oregonmarkets.domain.admin.model.AdminRole;
import com.oregonmarkets.domain.admin.repository.AdminRoleRepository;
import com.oregonmarkets.integration.keycloak.KeycloakAdminClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import org.springframework.beans.factory.annotation.Value;

import java.time.Instant;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class RoleService {

    private final KeycloakAdminClient keycloakAdminClient;
    private final AdminRoleRepository roleRepository;
    private final PermissionService permissionService;

    @Value("${keycloak.admin.client-id:oregon-admin-app}")
    private String adminClientId;

    /**
     * Get all roles from Keycloak with their permissions
     */
    public Flux<RoleResponse> getAllRoles() {
        return keycloakAdminClient.getRealmRoles()
                .flatMapMany(Flux::fromIterable)
                .flatMap(roleMap -> {
                    String roleName = (String) roleMap.get("name");
                    // Get permissions (composites) for this role
                    return keycloakAdminClient.getRoleComposites(roleName)
                            .defaultIfEmpty(Collections.emptyList())
                            .flatMap(composites -> {
                                // Get database role to use its ID
                                return roleRepository.findByName(roleName)
                                        .filter(AdminRole::getIsActive)
                                        .map(dbRole -> {
                                            RoleResponse roleResponse = mapToRoleResponse(roleMap);
                                            // Use database ID instead of Keycloak ID
                                            roleResponse.setId(dbRole.getId().toString());
                                            // Map composites to permissions
                                            List<PermissionResponse> permissions = composites.stream()
                                                    .filter(composite -> Boolean.TRUE.equals(composite.get("clientRole")))
                                                    .map(this::mapToPermissionResponse)
                                                    .toList();
                                            roleResponse.setPermissions(permissions);
                                            return roleResponse;
                                        });
                            });
                });
    }

    /**
     * Get role by name with permissions
     */
    public Mono<RoleResponse> getRoleByName(String roleName) {
        return keycloakAdminClient.getRoleByName(roleName)
                .flatMap(roleMap -> {
                    // Get permissions (composites) for this role
                    return keycloakAdminClient.getRoleComposites(roleName)
                            .defaultIfEmpty(Collections.emptyList())
                            .map(composites -> {
                                RoleResponse roleResponse = mapToRoleResponse(roleMap);
                                // Map composites to permissions
                                List<PermissionResponse> permissions = composites.stream()
                                        .filter(composite -> Boolean.TRUE.equals(composite.get("clientRole")))
                                        .map(this::mapToPermissionResponse)
                                        .toList();
                                roleResponse.setPermissions(permissions);
                                return roleResponse;
                            });
                });
    }

    /**
     * Create a new role in Keycloak with permissions
     */
    public Mono<RoleResponse> createRole(CreateRoleRequest request) {
        return validateCreateRequest(request)
                .then(checkRoleDuplication(request.getName()))
                .then(Mono.defer(() -> performRoleCreation(request)));
    }

    /**
     * Validate create role request
     */
    private Mono<Void> validateCreateRequest(CreateRoleRequest request) {
        if (request.getPermissionIds() == null || request.getPermissionIds().isEmpty()) {
            return Mono.error(new com.oregonmarkets.common.exception.BusinessException(
                    com.oregonmarkets.common.response.ResponseCode.ROLE_REQUIRES_PERMISSIONS,
                    "At least one permission is required to create a role"
            ));
        }
        return Mono.empty();
    }

    /**
     * Check if role already exists in Keycloak or database
     */
    private Mono<Void> checkRoleDuplication(String roleName) {
        return Mono.zip(
                checkKeycloakRoleDuplication(roleName),
                checkDatabaseRoleDuplication(roleName)
        ).then();
    }

    /**
     * Check if role exists in Keycloak
     */
    private Mono<Void> checkKeycloakRoleDuplication(String roleName) {
        return keycloakAdminClient.getRoleByName(roleName)
                .flatMap(existingRole -> {
                    log.warn("Attempted to create duplicate role in Keycloak: {}", roleName);
                    return Mono.<Void>error(new com.oregonmarkets.common.exception.BusinessException(
                            com.oregonmarkets.common.response.ResponseCode.DUPLICATE_ROLE,
                            "Role '" + roleName + "' already exists in Keycloak"
                    ));
                })
                .onErrorResume(throwable -> {
                    // Handle 404 - role doesn't exist, which is what we want for creation
                    if (throwable instanceof org.springframework.web.reactive.function.client.WebClientResponseException &&
                        ((org.springframework.web.reactive.function.client.WebClientResponseException) throwable).getStatusCode().value() == 404) {
                        return Mono.empty();
                    }
                    return Mono.error(throwable);
                })
                .then();
    }

    /**
     * Check if role exists in database
     */
    private Mono<Void> checkDatabaseRoleDuplication(String roleName) {
        return roleRepository.findByName(roleName)
                .filter(AdminRole::getIsActive)
                .flatMap(existingRole -> {
                    log.warn("Attempted to create duplicate role in database: {}", roleName);
                    return Mono.<Void>error(new com.oregonmarkets.common.exception.BusinessException(
                            com.oregonmarkets.common.response.ResponseCode.DUPLICATE_ROLE,
                            "Role '" + roleName + "' already exists in database"
                    ));
                })
                .then();
    }

    /**
     * Perform the actual role creation
     */
    private Mono<RoleResponse> performRoleCreation(CreateRoleRequest request) {
        Map<String, Object> roleRepresentation = buildCreateRoleRepresentation(request);

        return keycloakAdminClient.createRealmRole(roleRepresentation)
                .then(keycloakAdminClient.getRoleByName(request.getName()))
                .flatMap(roleMap -> assignPermissionsToNewRole(request, roleMap))
                .flatMap(this::fetchUpdatedRoleWithPermissions)
                .flatMap(roleResponse -> syncRoleToDatabase(roleResponse.getName()).thenReturn(roleResponse));
    }

    /**
     * Build role representation for creation
     */
    private Map<String, Object> buildCreateRoleRepresentation(CreateRoleRequest request) {
        Map<String, Object> roleRepresentation = new HashMap<>();
        roleRepresentation.put("name", request.getName());
        roleRepresentation.put("description", request.getDescription());
        roleRepresentation.put("composite", true);
        roleRepresentation.put("clientRole", false);
        return roleRepresentation;
    }

    /**
     * Assign permissions to newly created role
     */
    private Mono<String> assignPermissionsToNewRole(CreateRoleRequest request, Map<String, Object> roleMap) {
        String roleName = (String) roleMap.get("name");
        log.info("Assigning {} permissions to role: {}", request.getPermissionIds().size(), roleName);

        return permissionService.getAllPermissions()
                .filter(permission -> request.getPermissionIds().contains(permission.getId()))
                .collectList()
                .flatMap(matchedPermissions -> validateAndAddPermissions(roleName, matchedPermissions));
    }

    /**
     * Update an existing role in Keycloak
     */
    public Mono<RoleResponse> updateRole(String roleName, UpdateRoleRequest request) {
        return keycloakAdminClient.getRoleByName(roleName)
                .switchIfEmpty(Mono.error(new AdminRoleNotFoundException(roleName)))
                .flatMap(roleMap -> updateRoleDescription(roleName, request, roleMap))
                .flatMap(rn -> updateRolePermissionsIfProvided(rn, request))
                .flatMap(this::fetchUpdatedRoleWithPermissions)
                .flatMap(roleResponse -> syncRoleToDatabase(roleResponse.getName()).thenReturn(roleResponse))
                .doOnSuccess(roleResponse -> log.info("Successfully updated role: {}", roleName))
                .doOnError(error -> log.error("Failed to update role {}: {}", roleName, error.getMessage()));
    }

    /**
     * Update role description in Keycloak
     */
    private Mono<String> updateRoleDescription(String roleName, UpdateRoleRequest request, Map<String, Object> roleMap) {
        Map<String, Object> roleRepresentation = buildRoleRepresentation(roleName, request, roleMap);
        return keycloakAdminClient.updateRealmRole(roleName, roleRepresentation)
                .thenReturn(roleName);
    }

    /**
     * Build role representation for update
     */
    private Map<String, Object> buildRoleRepresentation(String roleName, UpdateRoleRequest request, Map<String, Object> roleMap) {
        Map<String, Object> roleRepresentation = new HashMap<>();
        roleRepresentation.put("name", roleName);
        roleRepresentation.put("description",
                request.getDescription() != null ? request.getDescription() : roleMap.get("description"));
        roleRepresentation.put("composite", true);
        roleRepresentation.put("clientRole", false);
        return roleRepresentation;
    }

    /**
     * Update role permissions if provided in request
     */
    private Mono<String> updateRolePermissionsIfProvided(String roleName, UpdateRoleRequest request) {
        if (request.getPermissionIds() == null || request.getPermissionIds().isEmpty()) {
            return Mono.just(roleName);
        }

        log.info("Updating permissions for role: {}", roleName);
        return removeCurrentPermissions(roleName)
                .flatMap(rn -> addNewPermissions(rn, request.getPermissionIds()));
    }

    /**
     * Remove current permissions from role
     */
    private Mono<String> removeCurrentPermissions(String roleName) {
        return keycloakAdminClient.getRoleComposites(roleName)
                .defaultIfEmpty(Collections.emptyList())
                .flatMap(currentComposites -> {
                    List<Map<String, Object>> clientRoleComposites = currentComposites.stream()
                            .filter(composite -> Boolean.TRUE.equals(composite.get("clientRole")))
                            .toList();

                    if (clientRoleComposites.isEmpty()) {
                        return Mono.just(roleName);
                    }

                    return keycloakAdminClient.removeRoleComposites(roleName, clientRoleComposites)
                            .thenReturn(roleName);
                });
    }

    /**
     * Add new permissions to role
     */
    private Mono<String> addNewPermissions(String roleName, List<String> permissionIds) {
        return permissionService.getAllPermissions()
                .filter(permission -> permissionIds.contains(permission.getId()))
                .collectList()
                .flatMap(matchedPermissions -> validateAndAddPermissions(roleName, matchedPermissions));
    }

    /**
     * Validate matched permissions and add them to role
     */
    private Mono<String> validateAndAddPermissions(String roleName, List<PermissionResponse> matchedPermissions) {
        if (matchedPermissions.isEmpty()) {
            return Mono.error(new com.oregonmarkets.common.exception.BusinessException(
                    com.oregonmarkets.common.response.ResponseCode.ROLE_REQUIRES_PERMISSIONS,
                    "No valid permissions found"
            ));
        }

        return fetchPermissionsAsComposites(matchedPermissions)
                .flatMap(compositeRoles -> addCompositesToRole(roleName, compositeRoles));
    }

    /**
     * Fetch permissions from Keycloak as composite roles
     */
    private Mono<List<Map<String, Object>>> fetchPermissionsAsComposites(List<PermissionResponse> permissions) {
        return keycloakAdminClient.getClientByClientId(adminClientId)
                .flatMap(client -> {
                    String clientUuid = (String) client.get("id");
                    return Flux.fromIterable(permissions)
                            .flatMap(permission -> keycloakAdminClient.getClientRoleByName(clientUuid, permission.getName())
                                    .onErrorResume(e -> {
                                        log.warn("Failed to fetch permission {}: {}", permission.getName(), e.getMessage());
                                        return Mono.empty();
                                    }))
                            .collectList();
                });
    }

    /**
     * Add composite roles to the role
     */
    private Mono<String> addCompositesToRole(String roleName, List<Map<String, Object>> compositeRoles) {
        if (compositeRoles.isEmpty()) {
            return Mono.error(new com.oregonmarkets.common.exception.BusinessException(
                    com.oregonmarkets.common.response.ResponseCode.ROLE_REQUIRES_PERMISSIONS,
                    "Failed to fetch permissions from Keycloak"
            ));
        }

        return keycloakAdminClient.addRoleComposites(roleName, compositeRoles)
                .thenReturn(roleName);
    }

    /**
     * Fetch updated role with all permissions
     */
    private Mono<RoleResponse> fetchUpdatedRoleWithPermissions(String roleName) {
        return keycloakAdminClient.getRoleByName(roleName)
                .flatMap(roleMap -> keycloakAdminClient.getRoleComposites(roleName)
                        .defaultIfEmpty(Collections.emptyList())
                        .map(composites -> buildRoleResponseWithPermissions(roleMap, composites)));
    }

    /**
     * Build RoleResponse with permissions
     */
    private RoleResponse buildRoleResponseWithPermissions(Map<String, Object> roleMap, List<Map<String, Object>> composites) {
        RoleResponse roleResponse = mapToRoleResponse(roleMap);
        List<PermissionResponse> permissions = composites.stream()
                .filter(composite -> Boolean.TRUE.equals(composite.get("clientRole")))
                .map(this::mapToPermissionResponse)
                .toList();
        roleResponse.setPermissions(permissions);
        return roleResponse;
    }

    /**
     * Delete a role from both Keycloak and database
     */
    public Mono<Void> deleteRole(String roleName) {
        log.info("Attempting to delete role: {}", roleName);
        
        return checkRoleExistence(roleName)
                .flatMap(tuple -> executeRoleDeletion(roleName, tuple.getT1(), tuple.getT2()))
                .doOnSuccess(v -> log.info("Successfully completed deletion process for role: {}", roleName))
                .doOnError(error -> log.error("Failed to delete role {}: {}", roleName, error.getMessage()));
    }

    private Mono<Tuple2<Optional<Map<String, Object>>, Optional<AdminRole>>> checkRoleExistence(String roleName) {
        Mono<Optional<Map<String, Object>>> keycloakRole = keycloakAdminClient.getRoleByName(roleName)
                .map(Optional::of)
                .onErrorReturn(Optional.empty());
        
        Mono<Optional<AdminRole>> dbRole = roleRepository.findByName(roleName)
                .map(Optional::of)
                .defaultIfEmpty(Optional.empty());
        
        return Mono.zip(keycloakRole, dbRole);
    }

    private Mono<Void> executeRoleDeletion(String roleName, Optional<Map<String, Object>> kcRole, Optional<AdminRole> adminRole) {
        if (kcRole.isEmpty() && adminRole.isEmpty()) {
            return Mono.error(new AdminRoleNotFoundException(roleName + " not found in Keycloak or database"));
        }
        
        Mono<Void> deleteFromKeycloak = deleteFromKeycloak(roleName, kcRole);
        Mono<Void> deleteFromDb = deleteFromDatabase(roleName, adminRole);
        
        return Mono.when(deleteFromKeycloak, deleteFromDb);
    }

    private Mono<Void> deleteFromKeycloak(String roleName, Optional<Map<String, Object>> kcRole) {
        if (kcRole.isEmpty()) {
            log.warn("Role '{}' not found in Keycloak, skipping Keycloak deletion", roleName);
            return Mono.empty();
        }
        
        log.info("Deleting role '{}' from Keycloak", roleName);
        return keycloakAdminClient.deleteRealmRole(roleName)
                .doOnSuccess(v -> log.info("Successfully deleted role '{}' from Keycloak", roleName))
                .onErrorResume(error -> {
                    log.error("Keycloak deletion failed for role '{}': {}", roleName, error.getMessage());
                    return Mono.empty();
                });
    }

    private Mono<Void> deleteFromDatabase(String roleName, Optional<AdminRole> adminRole) {
        if (adminRole.isEmpty()) {
            log.warn("Role '{}' not found in database, skipping database deletion", roleName);
            return Mono.empty();
        }
        
        log.info("Deleting role '{}' from database", roleName);
        return roleRepository.delete(adminRole.get())
                .doOnSuccess(v -> log.info("Successfully deleted role '{}' from database", roleName))
                .onErrorResume(error -> {
                    log.error("Database deletion failed for role '{}': {}", roleName, error.getMessage());
                    return Mono.empty();
                });
    }

    /**
     * Sync role from Keycloak to local database for caching
     */
    private Mono<AdminRole> syncRoleToDatabase(String roleName) {
        return keycloakAdminClient.getRoleByName(roleName)
                .flatMap(roleMap -> {
                    String keycloakRoleId = (String) roleMap.get("id");

                    return roleRepository.findByKeycloakRoleId(keycloakRoleId)
                            .switchIfEmpty(
                                    Mono.defer(() -> {
                                        AdminRole newRole = AdminRole.builder()
                                                .keycloakRoleId(keycloakRoleId)
                                                .name(roleName)
                                                .description((String) roleMap.get("description"))
                                                .isComposite((Boolean) roleMap.getOrDefault("composite", false))
                                                .isActive(true)
                                                .lastSyncedAt(Instant.now())
                                                .build();

                                        return roleRepository.save(newRole);
                                    })
                            )
                            .flatMap(existingRole -> {
                                // Update existing role
                                existingRole.setName(roleName);
                                existingRole.setDescription((String) roleMap.get("description"));
                                existingRole.setIsComposite((Boolean) roleMap.getOrDefault("composite", false));
                                existingRole.setLastSyncedAt(Instant.now());

                                return roleRepository.save(existingRole);
                            });
                });
    }

    /**
     * Map Keycloak role to RoleResponse
     */
    private RoleResponse mapToRoleResponse(Map<String, Object> roleMap) {
        return RoleResponse.builder()
                .id((String) roleMap.get("id"))
                .name((String) roleMap.get("name"))
                .description((String) roleMap.get("description"))
                .composite((Boolean) roleMap.getOrDefault("composite", false))
                .clientRole((Boolean) roleMap.getOrDefault("clientRole", false))
                .permissions(Collections.emptyList())
                .build();
    }

    /**
     * Map Keycloak composite role to PermissionResponse
     */
    private PermissionResponse mapToPermissionResponse(Map<String, Object> compositeMap) {
        String roleName = (String) compositeMap.get("name");
        String description = (String) compositeMap.get("description");

        // Parse permission name (format: module:action)
        String[] parts = roleName.contains(":") ? roleName.split(":", 2) : new String[]{roleName, ""};
        String module = parts[0];
        String action = parts.length > 1 ? parts[1] : "";

        return PermissionResponse.builder()
                .id((String) compositeMap.get("id"))
                .name(roleName)
                .description(description != null ? description : "")
                .module(module)
                .action(action)
                .build();
    }
}
