package com.oregonmarkets.domain.admin.controller;

import com.oregonmarkets.common.response.ApiResponse;
import com.oregonmarkets.common.response.ResponseCode;
import com.oregonmarkets.common.response.PagedResponse;
import com.oregonmarkets.domain.admin.dto.request.CreatePermissionRequest;
import com.oregonmarkets.domain.admin.dto.request.UpdatePermissionRequest;
import com.oregonmarkets.domain.admin.dto.response.PermissionResponse;
import com.oregonmarkets.domain.admin.service.PermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.*;
import reactor.core.publisher.Mono;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class PermissionRouterConfig {

    private final PermissionService permissionService;

    @Bean
    public RouterFunction<ServerResponse> permissionRoutes() {
        return RouterFunctions.route()
                .GET("/api/admin/permissions", this::getAllPermissions)
                .GET("/api/admin/permissions/module/{module}", this::getPermissionsByModule)
                .POST("/api/admin/permissions", this::createPermission)
                .PUT("/api/admin/permissions/{permissionName}", this::updatePermission)
                .DELETE("/api/admin/permissions/{permissionName}", this::deletePermission)
                .build();
    }

    private Mono<ServerResponse> getAllPermissions(ServerRequest request) {
        // Extract pagination parameters with defaults
        int page = request.queryParam("page")
                .map(Integer::parseInt)
                .filter(p -> p >= 0)
                .orElse(0); // Default to page 0

        int size = request.queryParam("size")
                .map(Integer::parseInt)
                .filter(s -> s > 0 && s <= 100) // Max size of 100
                .orElse(20); // Default to size 20

        log.info("Fetching permissions with pagination - page: {}, size: {}", page, size);

        return permissionService.getAllPermissionsPaged(page, size)
                .map(pagedResponse -> ApiResponse.success(ResponseCode.SUCCESS, pagedResponse))
                .flatMap(apiResponse ->
                        ServerResponse.ok()
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(apiResponse))
                .onErrorResume(error -> {
                    log.error("Error fetching permissions: {}", error.getMessage());
                    return ServerResponse.status(500)
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(ApiResponse.error(ResponseCode.INTERNAL_SERVER_ERROR, error.getMessage()));
                });
    }

    private Mono<ServerResponse> getPermissionsByModule(ServerRequest request) {
        String module = request.pathVariable("module");
        log.info("Fetching permissions by module: {}", module);

        return permissionService.getPermissionsByModule(module)
                .collectList()
                .map(permissions -> ApiResponse.success(ResponseCode.SUCCESS, permissions))
                .flatMap(apiResponse ->
                        ServerResponse.ok()
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(apiResponse))
                .onErrorResume(error -> {
                    log.error("Error fetching permissions for module {}: {}", module, error.getMessage());
                    return ServerResponse.status(500)
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(ApiResponse.error(ResponseCode.INTERNAL_SERVER_ERROR, error.getMessage()));
                });
    }

    private Mono<ServerResponse> createPermission(ServerRequest request) {
        log.info("Creating new permission");

        return request.bodyToMono(CreatePermissionRequest.class)
                .flatMap(permissionService::createPermission)
                .map(permission -> ApiResponse.success(ResponseCode.CREATED, permission))
                .flatMap(apiResponse ->
                        ServerResponse.ok()
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(apiResponse))
                .onErrorResume(error -> {
                    log.error("Error creating permission: {}", error.getMessage());
                    return ServerResponse.status(500)
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(ApiResponse.error(ResponseCode.INTERNAL_SERVER_ERROR, error.getMessage()));
                });
    }

    private Mono<ServerResponse> updatePermission(ServerRequest request) {
        String permissionName = request.pathVariable("permissionName");
        log.info("Updating permission: {}", permissionName);

        return request.bodyToMono(UpdatePermissionRequest.class)
                .flatMap(updateRequest -> permissionService.updatePermission(permissionName, updateRequest))
                .map(permission -> ApiResponse.success(ResponseCode.UPDATED, permission))
                .flatMap(apiResponse ->
                        ServerResponse.ok()
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(apiResponse))
                .onErrorResume(error -> {
                    log.error("Error updating permission {}: {}", permissionName, error.getMessage());
                    return ServerResponse.status(500)
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(ApiResponse.error(ResponseCode.INTERNAL_SERVER_ERROR, error.getMessage()));
                });
    }

    private Mono<ServerResponse> deletePermission(ServerRequest request) {
        String permissionName = request.pathVariable("permissionName");
        log.info("Deleting permission: {}", permissionName);

        return permissionService.deletePermission(permissionName)
                .then(Mono.fromCallable(() -> ApiResponse.<Void>success(ResponseCode.DELETED, null)))
                .flatMap(apiResponse ->
                        ServerResponse.ok()
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(apiResponse))
                .onErrorResume(error -> {
                    log.error("Error deleting permission {}: {}", permissionName, error.getMessage());
                    return ServerResponse.status(500)
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(ApiResponse.error(ResponseCode.INTERNAL_SERVER_ERROR, error.getMessage()));
                });
    }
}
