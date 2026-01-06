package com.oregonmarkets.domain.admin.service;

import com.oregonmarkets.domain.admin.dto.request.CreatePermissionRequest;
import com.oregonmarkets.domain.admin.dto.request.UpdatePermissionRequest;
import com.oregonmarkets.domain.admin.dto.response.PermissionResponse;
import com.oregonmarkets.domain.admin.exception.AdminPermissionNotFoundException;
import com.oregonmarkets.domain.admin.model.AdminPermission;
import com.oregonmarkets.domain.admin.repository.AdminPermissionRepository;
import com.oregonmarkets.integration.keycloak.KeycloakAdminClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class PermissionService {

    private final KeycloakAdminClient keycloakAdminClient;
    private final AdminPermissionRepository permissionRepository;

    @Value("${keycloak.admin.client-id:oregon-admin-app}")
    private String adminClientId;

    /**
     * Get all permissions (client roles) from Keycloak
     */
    public Flux<PermissionResponse> getAllPermissions() {
        return keycloakAdminClient.getClientByClientId(adminClientId)
                .flatMapMany(client -> {
                    String clientUuid = (String) client.get("id");
                    return keycloakAdminClient.getClientRoles(clientUuid)
                            .flatMapMany(Flux::fromIterable);
                })
                .map(this::mapToPermissionResponse)
                .onErrorResume(error -> {
                    log.error("Failed to get permissions: {}", error.getMessage());
                    return Flux.empty();
                });
    }

    /**
     * Get paginated permissions (client roles) from Keycloak
     * @param page Page number (0-indexed)
     * @param size Page size
     * @return Mono containing paginated response
     */
    public Mono<com.oregonmarkets.common.response.PagedResponse<PermissionResponse>> getAllPermissionsPaged(int page, int size) {
        return getAllPermissions()
                .collectList()
                .map(allPermissions -> {
                    long totalElements = allPermissions.size();
                    int fromIndex = page * size;
                    int toIndex = Math.min(fromIndex + size, allPermissions.size());

                    // Handle out of bounds
                    if (fromIndex >= allPermissions.size()) {
                        return com.oregonmarkets.common.response.PagedResponse.of(
                                java.util.Collections.emptyList(),
                                page,
                                size,
                                totalElements
                        );
                    }

                    java.util.List<PermissionResponse> pageContent = allPermissions.subList(fromIndex, toIndex);

                    return com.oregonmarkets.common.response.PagedResponse.of(
                            pageContent,
                            page,
                            size,
                            totalElements
                    );
                });
    }

    /**
     * Get permissions by module
     */
    public Flux<PermissionResponse> getPermissionsByModule(String module) {
        return getAllPermissions()
                .filter(permission -> module.equals(permission.getModule()));
    }

    /**
     * Create a new permission in both Keycloak and database
     */
    public Mono<PermissionResponse> createPermission(CreatePermissionRequest request) {
        String permissionName = buildPermissionName(request);
        log.info("Creating permission: {}", permissionName);

        return checkPermissionExistence(permissionName, request)
                .flatMap(tuple -> executePermissionCreation(permissionName, request, tuple.getT1(), tuple.getT2()));
    }

    private Mono<Tuple2<Optional<Map<String, Object>>, Optional<AdminPermission>>> checkPermissionExistence(
            String permissionName, CreatePermissionRequest request) {
        
        // Check Keycloak
        Mono<Optional<Map<String, Object>>> keycloakPermission = keycloakAdminClient.getClientByClientId(adminClientId)
                .flatMap(client -> {
                    String clientUuid = (String) client.get("id");
                    return keycloakAdminClient.getClientRoles(clientUuid)
                            .flatMapMany(Flux::fromIterable)
                            .filter(role -> permissionName.equals(role.get("name")))
                            .next()
                            .map(Optional::of)
                            .defaultIfEmpty(Optional.empty());
                })
                .onErrorReturn(Optional.empty());

        // Check database
        Mono<Optional<AdminPermission>> dbPermission = permissionRepository.findByName(permissionName)
                .map(Optional::of)
                .defaultIfEmpty(Optional.empty());

        return Mono.zip(keycloakPermission, dbPermission);
    }

    private Mono<PermissionResponse> executePermissionCreation(
            String permissionName, 
            CreatePermissionRequest request,
            Optional<Map<String, Object>> kcPermission, 
            Optional<AdminPermission> dbPermission) {

        // If exists in both, return error
        if (kcPermission.isPresent() && dbPermission.isPresent()) {
            return Mono.error(new com.oregonmarkets.common.exception.BusinessException(
                    com.oregonmarkets.common.response.ResponseCode.DUPLICATE_PERMISSION,
                    "Permission '" + permissionName + "' already exists"
            ));
        }

        // Create in Keycloak if missing
        Mono<Map<String, Object>> createInKeycloak = kcPermission.isPresent() 
                ? Mono.just(kcPermission.get())
                : createPermissionInKeycloak(permissionName, request);

        // Create in database if missing  
        Mono<AdminPermission> createInDb = dbPermission.isPresent()
                ? Mono.just(dbPermission.get())
                : createInKeycloak.flatMap(kcPerm -> createPermissionInDatabase(permissionName, request, kcPerm));

        return createInDb.map(this::mapToPermissionResponse);
    }

    private Mono<Map<String, Object>> createPermissionInKeycloak(String permissionName, CreatePermissionRequest request) {
        log.info("Creating permission '{}' in Keycloak", permissionName);
        Map<String, Object> roleRepresentation = buildPermissionRepresentation(permissionName, request);

        return keycloakAdminClient.getClientByClientId(adminClientId)
                .flatMap(client -> {
                    String clientUuid = (String) client.get("id");
                    return keycloakAdminClient.createClientRole(clientUuid, roleRepresentation)
                            .then(keycloakAdminClient.getClientRoles(clientUuid))
                            .flatMapMany(Flux::fromIterable)
                            .filter(role -> permissionName.equals(role.get("name")))
                            .next();
                })
                .doOnSuccess(v -> log.info("Successfully created permission '{}' in Keycloak", permissionName))
                .doOnError(error -> log.error("Failed to create permission '{}' in Keycloak: {}", permissionName, error.getMessage()));
    }

    private Mono<AdminPermission> createPermissionInDatabase(String permissionName, CreatePermissionRequest request, Map<String, Object> kcPermission) {
        log.info("Creating permission '{}' in database", permissionName);
        
        AdminPermission permission = AdminPermission.builder()
                .keycloakRoleId((String) kcPermission.get("id"))
                .name(permissionName)
                .description(request.getDescription())
                .module(request.getModule())
                .action(request.getAction())
                .isActive(true)
                .lastSyncedAt(Instant.now())
                .build();

        return permissionRepository.save(permission)
                .doOnSuccess(v -> log.info("Successfully created permission '{}' in database", permissionName))
                .doOnError(error -> log.error("Failed to create permission '{}' in database: {}", permissionName, error.getMessage()));
    }

    /**
     * Build permission name from module and action
     */
    private String buildPermissionName(CreatePermissionRequest request) {
        return request.getModule() + ":" + request.getAction();
    }

    /**
     * Check if permission already exists
     */
    private Mono<Void> checkPermissionDuplication(String permissionName) {
        return getAllPermissions()
                .filter(permission -> permissionName.equals(permission.getName()))
                .next()
                .flatMap(existingPermission -> {
                    log.warn("Attempted to create duplicate permission: {}", permissionName);
                    return Mono.<Void>error(new com.oregonmarkets.common.exception.BusinessException(
                            com.oregonmarkets.common.response.ResponseCode.DUPLICATE_PERMISSION,
                            "Permission '" + permissionName + "' already exists"
                    ));
                })
                .then();
    }

    /**
     * Perform the actual permission creation
     */
    private Mono<PermissionResponse> performPermissionCreation(String permissionName, CreatePermissionRequest request) {
        Map<String, Object> roleRepresentation = buildPermissionRepresentation(permissionName, request);

        return keycloakAdminClient.getClientByClientId(adminClientId)
                .flatMap(client -> createAndRetrievePermission(client, permissionName, roleRepresentation))
                .flatMap(permissionResponse -> syncPermissionToDatabase(permissionResponse).thenReturn(permissionResponse));
    }

    /**
     * Build permission representation for creation
     */
    private Map<String, Object> buildPermissionRepresentation(String permissionName, CreatePermissionRequest request) {
        Map<String, Object> roleRepresentation = new HashMap<>();
        roleRepresentation.put("name", permissionName);
        roleRepresentation.put("description", request.getDescription());
        roleRepresentation.put("clientRole", true);
        return roleRepresentation;
    }

    /**
     * Create permission and retrieve it
     */
    private Mono<PermissionResponse> createAndRetrievePermission(
            Map<String, Object> client,
            String permissionName,
            Map<String, Object> roleRepresentation) {

        String clientUuid = (String) client.get("id");

        return keycloakAdminClient.createClientRole(clientUuid, roleRepresentation)
                .then(keycloakAdminClient.getClientRoles(clientUuid))
                .flatMapMany(Flux::fromIterable)
                .filter(role -> permissionName.equals(role.get("name")))
                .next()
                .map(this::mapToPermissionResponse);
    }

    /**
     * Update an existing permission (client role) in Keycloak
     */
    public Mono<PermissionResponse> updatePermission(String permissionName, UpdatePermissionRequest request) {
        return keycloakAdminClient.getClientByClientId(adminClientId)
                .flatMap(client -> {
                    String clientUuid = (String) client.get("id");

                    // Verify permission exists
                    return keycloakAdminClient.getClientRoleByName(clientUuid, permissionName)
                            .switchIfEmpty(Mono.error(new AdminPermissionNotFoundException(permissionName)))
                            .flatMap(roleMap -> {
                                // Build update representation
                                Map<String, Object> roleRepresentation = new HashMap<>();
                                roleRepresentation.put("name", permissionName);

                                // Update description if provided
                                if (request.getDescription() != null) {
                                    roleRepresentation.put("description", request.getDescription());
                                } else {
                                    roleRepresentation.put("description", roleMap.get("description"));
                                }

                                roleRepresentation.put("clientRole", true);

                                // Update the client role in Keycloak
                                return keycloakAdminClient.updateClientRole(clientUuid, permissionName, roleRepresentation)
                                        .then(keycloakAdminClient.getClientRoleByName(clientUuid, permissionName));
                            })
                            .map(this::mapToPermissionResponse)
                            .flatMap(permissionResponse -> {
                                // Sync the updated permission to database
                                return syncPermissionToDatabase(permissionResponse)
                                        .thenReturn(permissionResponse);
                            });
                })
                .doOnSuccess(permissionResponse -> log.info("Successfully updated permission: {}", permissionName))
                .doOnError(error -> log.error("Failed to update permission {}: {}", permissionName, error.getMessage()));
    }

    /**
     * Delete a permission (client role) from Keycloak
     */
    public Mono<Void> deletePermission(String permissionName) {
        return keycloakAdminClient.getClientByClientId(adminClientId)
                .flatMap(client -> {
                    String clientUuid = (String) client.get("id");

                    // Verify permission exists
                    return keycloakAdminClient.getClientRoleByName(clientUuid, permissionName)
                            .switchIfEmpty(Mono.error(new AdminPermissionNotFoundException(permissionName)))
                            .flatMap(roleMap -> {
                                String keycloakRoleId = (String) roleMap.get("id");

                                // Delete from Keycloak
                                return keycloakAdminClient.deleteClientRole(clientUuid, permissionName)
                                        .then(Mono.defer(() -> {
                                            // Mark as inactive in database (soft delete)
                                            return permissionRepository.findByKeycloakRoleId(keycloakRoleId)
                                                    .flatMap(adminPermission -> {
                                                        adminPermission.setIsActive(false);
                                                        adminPermission.setLastSyncedAt(Instant.now());
                                                        return permissionRepository.save(adminPermission);
                                                    })
                                                    .then();
                                        }));
                            });
                })
                .doOnSuccess(v -> log.info("Successfully deleted permission: {}", permissionName))
                .doOnError(error -> log.error("Failed to delete permission {}: {}", permissionName, error.getMessage()));
    }

    /**
     * Sync permission from Keycloak to local database for caching
     */
    private Mono<AdminPermission> syncPermissionToDatabase(PermissionResponse permissionResponse) {
        return permissionRepository.findByKeycloakRoleId(permissionResponse.getId())
                .switchIfEmpty(
                        Mono.defer(() -> {
                            AdminPermission newPermission = AdminPermission.builder()
                                    .keycloakRoleId(permissionResponse.getId())
                                    .name(permissionResponse.getName())
                                    .description(permissionResponse.getDescription())
                                    .module(permissionResponse.getModule())
                                    .action(permissionResponse.getAction())
                                    .isActive(true)
                                    .lastSyncedAt(Instant.now())
                                    .build();

                            return permissionRepository.save(newPermission);
                        })
                )
                .flatMap(existingPermission -> {
                    // Update existing permission
                    existingPermission.setName(permissionResponse.getName());
                    existingPermission.setDescription(permissionResponse.getDescription());
                    existingPermission.setModule(permissionResponse.getModule());
                    existingPermission.setAction(permissionResponse.getAction());
                    existingPermission.setLastSyncedAt(Instant.now());

                    return permissionRepository.save(existingPermission);
                });
    }

    /**
     * Map Keycloak client role to PermissionResponse
     */
    private PermissionResponse mapToPermissionResponse(Map<String, Object> roleMap) {
        String roleName = (String) roleMap.get("name");
        String description = (String) roleMap.get("description");

        // Parse permission name (format: module:action)
        String[] parts = roleName.contains(":") ? roleName.split(":", 2) : new String[]{roleName, ""};
        String module = parts[0];
        String action = parts.length > 1 ? parts[1] : "";

        return PermissionResponse.builder()
                .id((String) roleMap.get("id"))
                .name(roleName)
                .description(description != null ? description : "")
                .module(module)
                .action(action)
                .build();
    }

    /**
     * Map AdminPermission to PermissionResponse
     */
    private PermissionResponse mapToPermissionResponse(AdminPermission permission) {
        return PermissionResponse.builder()
                .id(permission.getKeycloakRoleId())
                .name(permission.getName())
                .description(permission.getDescription() != null ? permission.getDescription() : "")
                .module(permission.getModule())
                .action(permission.getAction())
                .build();
    }
}
