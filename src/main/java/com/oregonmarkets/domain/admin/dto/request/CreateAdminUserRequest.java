package com.oregonmarkets.domain.admin.dto.request;

import com.oregonmarkets.domain.admin.model.AdminUser;
import lombok.Data;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@Data
public class CreateAdminUserRequest {

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    private String phone;

    @NotNull(message = "Role ID is required")
    private UUID roleId;

    private Boolean twoFactorEnabled = false;

    private AdminUser.AdminUserStatus status = AdminUser.AdminUserStatus.ACTIVE;
}
