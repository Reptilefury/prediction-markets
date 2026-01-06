package com.oregonmarkets.domain.admin.dto.request;

import com.oregonmarkets.domain.admin.model.AdminUser;
import lombok.Data;

import jakarta.validation.constraints.Email;
import java.util.UUID;

@Data
public class UpdateAdminUserRequest {

    private String firstName;
    private String lastName;

    @Email(message = "Email must be valid")
    private String email;

    private String phone;

    private UUID roleId;

    private Boolean twoFactorEnabled;

    private AdminUser.AdminUserStatus status;
}
