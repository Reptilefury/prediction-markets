package com.oregonmarkets.domain.admin.dto.response;

import com.oregonmarkets.domain.admin.model.AdminUser;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class AdminUserResponse {

    private UUID id;
    private String firstName;
    private String lastName;
    private String username;
    private String email;
    private String phone;
    private UUID roleId;
    private String roleName;
    private AdminUser.AdminUserStatus status;
    private Boolean twoFactorEnabled;
    private Instant lastLoginAt;
    private Integer loginAttempts;
    private Instant createdAt;
    private Instant updatedAt;
    private List<AdminPermissionResponse> permissions;

    @Data
    @Builder
    public static class AdminPermissionResponse {
        private UUID id;
        private String name;
        private String description;
        private String module;
        private String action;
    }
}
