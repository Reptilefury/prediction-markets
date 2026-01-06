package com.oregonmarkets.domain.admin.controller;

import com.oregonmarkets.common.response.ApiResponse;
import com.oregonmarkets.common.response.ResponseCode;
import com.oregonmarkets.domain.admin.dto.request.CreateRoleRequest;
import com.oregonmarkets.domain.admin.dto.request.UpdateRoleRequest;
import com.oregonmarkets.domain.admin.service.RoleService;
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
public class RoleRouterConfig {

    private final RoleService roleService;

    @Bean
    public RouterFunction<ServerResponse> roleRoutes() {
        return RouterFunctions.route()
                .GET("/api/admin/roles", this::getAllRoles)
                .GET("/api/admin/roles/{roleName}", this::getRoleByName)
                .POST("/api/admin/roles", this::createRole)
                .PUT("/api/admin/roles/{roleName}", this::updateRole)
                .DELETE("/api/admin/roles/{roleName}", this::deleteRole)
                .build();
    }

    private Mono<ServerResponse> getAllRoles(ServerRequest request) {
        log.info("Fetching all roles");
        return roleService.getAllRoles()
                .collectList()
                .map(roles -> ApiResponse.success(ResponseCode.SUCCESS, roles))
                .flatMap(apiResponse ->
                        ServerResponse.ok()
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(apiResponse))
                .onErrorResume(error -> {
                    log.error("Error fetching roles: {}", error.getMessage());
                    return ServerResponse.status(500)
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(ApiResponse.error(ResponseCode.INTERNAL_SERVER_ERROR, error.getMessage()));
                });
    }

    private Mono<ServerResponse> getRoleByName(ServerRequest request) {
        String roleName = request.pathVariable("roleName");
        log.info("Fetching role by name: {}", roleName);

        return roleService.getRoleByName(roleName)
                .map(role -> ApiResponse.success(ResponseCode.SUCCESS, role))
                .flatMap(apiResponse ->
                        ServerResponse.ok()
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(apiResponse))
                .onErrorResume(error -> {
                    log.error("Error fetching role {}: {}", roleName, error.getMessage());
                    return ServerResponse.status(404)
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(ApiResponse.error(ResponseCode.NOT_FOUND, "Role not found"));
                });
    }

    private Mono<ServerResponse> createRole(ServerRequest request) {
        log.info("Creating new role");

        return request.bodyToMono(CreateRoleRequest.class)
                .flatMap(roleService::createRole)
                .map(role -> ApiResponse.success(ResponseCode.CREATED, role))
                .flatMap(apiResponse ->
                        ServerResponse.ok()
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(apiResponse))
                .onErrorResume(error -> {
                    log.error("Error creating role: {}", error.getMessage());
                    return ServerResponse.status(500)
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(ApiResponse.error(ResponseCode.INTERNAL_SERVER_ERROR, error.getMessage()));
                });
    }

    private Mono<ServerResponse> updateRole(ServerRequest request) {
        String roleName = request.pathVariable("roleName");
        log.info("Updating role: {}", roleName);

        return request.bodyToMono(UpdateRoleRequest.class)
                .flatMap(updateRequest -> roleService.updateRole(roleName, updateRequest))
                .map(role -> ApiResponse.success(ResponseCode.UPDATED, role))
                .flatMap(apiResponse ->
                        ServerResponse.ok()
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(apiResponse))
                .onErrorResume(error -> {
                    log.error("Error updating role {}: {}", roleName, error.getMessage());
                    return ServerResponse.status(500)
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(ApiResponse.error(ResponseCode.INTERNAL_SERVER_ERROR, error.getMessage()));
                });
    }

    private Mono<ServerResponse> deleteRole(ServerRequest request) {
        String roleName = request.pathVariable("roleName");
        log.info("Deleting role: {}", roleName);

        return roleService.deleteRole(roleName)
                .then(Mono.fromCallable(() -> ApiResponse.<Void>success(ResponseCode.DELETED, null)))
                .flatMap(apiResponse ->
                        ServerResponse.ok()
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(apiResponse))
                .onErrorResume(error -> {
                    log.error("Error deleting role {}: {}", roleName, error.getMessage());
                    return ServerResponse.status(500)
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(ApiResponse.error(ResponseCode.INTERNAL_SERVER_ERROR, error.getMessage()));
                });
    }
}
