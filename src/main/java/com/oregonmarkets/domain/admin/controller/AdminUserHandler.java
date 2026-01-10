package com.oregonmarkets.domain.admin.controller;

import com.oregonmarkets.common.response.ApiResponse;
import com.oregonmarkets.common.response.ResponseCode;
import com.oregonmarkets.domain.admin.dto.request.CreateAdminUserRequest;
import com.oregonmarkets.domain.admin.dto.request.UpdateAdminUserRequest;
import com.oregonmarkets.domain.admin.exception.AdminRoleNotFoundException;
import com.oregonmarkets.domain.admin.exception.AdminUserAlreadyExistsException;
import com.oregonmarkets.domain.admin.exception.AdminUserNotFoundException;
import com.oregonmarkets.domain.admin.model.AdminUser;
import com.oregonmarkets.domain.admin.service.AdminUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminUserHandler {

    private final AdminUserService adminUserService;

    public Mono<ServerResponse> createAdminUser(ServerRequest request) {
        return request.bodyToMono(CreateAdminUserRequest.class)
            .flatMap(adminUserService::createAdminUser)
            .flatMap(user -> ServerResponse.ok().bodyValue(ApiResponse.success(ResponseCode.ADMIN_USER_CREATED,ResponseCode.ADMIN_USER_CREATED.getMessage() ,user)))
            .onErrorResume(e -> {
                log.error("Error creating admin user", e);
                return ServerResponse.badRequest().bodyValue(ApiResponse.error(ResponseCode.VALIDATION_ERROR, e.getMessage(),null));
            });
    }

    public Mono<ServerResponse> getAdminUser(ServerRequest request) {
        UUID id = UUID.fromString(request.pathVariable("id"));
        return adminUserService.getAdminUser(id)
            .flatMap(user -> ServerResponse.ok().bodyValue(ApiResponse.success(ResponseCode.ADMIN_USER_RETRIEVED,ResponseCode.ADMIN_USER_RETRIEVED.getMessage(), user)))
            .onErrorResume(e -> {
                log.error("Error retrieving admin user", e);
                return ServerResponse.badRequest().bodyValue(ApiResponse.error(ResponseCode.ADMIN_USER_NOT_FOUND, e.getMessage(),null));
            });
    }

    public Mono<ServerResponse> getAllAdminUsers(ServerRequest request) {
        String search = request.queryParam("search").orElse(null);
        
        return (search != null && !search.trim().isEmpty()
            ? adminUserService.searchAdminUsers(search.trim()).collectList()
            : adminUserService.getAllAdminUsers().collectList())
            .flatMap(users -> ServerResponse.ok().bodyValue(ApiResponse.success(ResponseCode.ADMIN_USERS_LISTED, ResponseCode.ADMIN_USERS_LISTED.getMessage(),users)));
    }

    public Mono<ServerResponse> updateAdminUser(ServerRequest request) {
        UUID id = UUID.fromString(request.pathVariable("id"));
        return request.bodyToMono(UpdateAdminUserRequest.class)
            .flatMap(updateRequest -> adminUserService.updateAdminUser(id, updateRequest))
            .flatMap(user -> ServerResponse.ok().bodyValue(ApiResponse.success(ResponseCode.ADMIN_USER_UPDATED,ResponseCode.ADMIN_USER_UPDATED.getMessage(), user)))
            .onErrorResume(AdminUserNotFoundException.class, e -> {
                log.error("Admin user not found: {}", e.getMessage());
                return ServerResponse.badRequest().bodyValue(ApiResponse.error(ResponseCode.ADMIN_USER_NOT_FOUND, e.getMessage(), null));
            })
            .onErrorResume(AdminUserAlreadyExistsException.class, e -> {
                log.error("Admin user email already exists: {}", e.getMessage());
                return ServerResponse.badRequest().bodyValue(ApiResponse.error(ResponseCode.ADMIN_USER_EMAIL_EXISTS, e.getMessage(), null));
            })
            .onErrorResume(AdminRoleNotFoundException.class, e -> {
                log.error("Admin role not found: {}", e.getMessage());
                return ServerResponse.badRequest().bodyValue(ApiResponse.error(ResponseCode.ADMIN_ROLE_NOT_FOUND, e.getMessage(), null));
            })
            .onErrorResume(IllegalArgumentException.class, e -> {
                log.error("Validation error: {}", e.getMessage());
                return ServerResponse.badRequest().bodyValue(ApiResponse.error(ResponseCode.VALIDATION_ERROR, e.getMessage(), null));
            })
            .onErrorResume(e -> {
                log.error("Error updating admin user", e);
                return ServerResponse.badRequest().bodyValue(ApiResponse.error(ResponseCode.VALIDATION_ERROR, e.getMessage(),null));
            });
    }

    public Mono<ServerResponse> deleteAdminUser(ServerRequest request) {
        UUID id = UUID.fromString(request.pathVariable("id"));
        return adminUserService.deleteAdminUser(id)
            .then(ServerResponse.ok().bodyValue(ApiResponse.success(ResponseCode.ADMIN_USER_DELETED, ResponseCode.ADMIN_USER_DELETED.getMessage(),null)))
            .onErrorResume(e -> {
                log.error("Error deleting admin user", e);
                return ServerResponse.badRequest().bodyValue(ApiResponse.error(ResponseCode.ADMIN_USER_NOT_FOUND, e.getMessage(),null));
            });
    }

    public Mono<ServerResponse> updateLastLogin(ServerRequest request) {
        UUID id = UUID.fromString(request.pathVariable("id"));
        return adminUserService.updateLastLogin(id)
            .then(ServerResponse.ok().bodyValue(ApiResponse.success(ResponseCode.ADMIN_LOGIN_UPDATED, ResponseCode.ADMIN_LOGIN_UPDATED.getMessage(), null)))
            .onErrorResume(e -> {
                log.error("Error updating last login", e);
                return ServerResponse.badRequest().bodyValue(ApiResponse.error(ResponseCode.ADMIN_USER_NOT_FOUND, e.getMessage(),null));
            });
    }

    public Mono<ServerResponse> getAdminUserStatistics(ServerRequest request) {
        return adminUserService.getAdminUserStatistics()
            .flatMap(stats -> ServerResponse.ok().bodyValue(ApiResponse.success(ResponseCode.ADMIN_USER_STATS_RETRIEVED, ResponseCode.ADMIN_USER_STATS_RETRIEVED.getMessage(), stats)))
            .onErrorResume(e -> {
                log.error("Error retrieving admin user statistics", e);
                return ServerResponse.badRequest().bodyValue(ApiResponse.error(ResponseCode.VALIDATION_ERROR, e.getMessage(), null));
            });
    }

    public Mono<ServerResponse> updateUserStatus(ServerRequest request) {
        UUID id = UUID.fromString(request.pathVariable("id"));
        String statusParam = request.pathVariable("status");
        
        try {
            AdminUser.AdminUserStatus newStatus = AdminUser.AdminUserStatus.valueOf(statusParam.toUpperCase());
            
            return adminUserService.updateUserStatus(id, newStatus)
                .flatMap(user -> ServerResponse.ok().bodyValue(ApiResponse.success(ResponseCode.ADMIN_USER_UPDATED, "User status updated successfully", user)))
                .onErrorResume(AdminUserNotFoundException.class, e -> {
                    log.error("Admin user not found: {}", e.getMessage());
                    return ServerResponse.badRequest().bodyValue(ApiResponse.error(ResponseCode.ADMIN_USER_NOT_FOUND, e.getMessage(), null));
                })
                .onErrorResume(e -> {
                    log.error("Error updating user status", e);
                    return ServerResponse.badRequest().bodyValue(ApiResponse.error(ResponseCode.VALIDATION_ERROR, e.getMessage(), null));
                });
        } catch (IllegalArgumentException e) {
            return ServerResponse.badRequest().bodyValue(ApiResponse.error(ResponseCode.VALIDATION_ERROR, "Invalid status: " + statusParam, null));
        }
    }
}
