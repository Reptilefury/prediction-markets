package com.oregonmarkets.domain.admin.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePermissionRequest {

    @NotBlank(message = "Permission name is required")
    private String name;

    private String description;

    @NotBlank(message = "Module is required")
    private String module;

    @NotBlank(message = "Action is required")
    private String action;
}
