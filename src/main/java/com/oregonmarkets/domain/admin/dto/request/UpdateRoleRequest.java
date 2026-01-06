package com.oregonmarkets.domain.admin.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateRoleRequest {

    private String description;

    /**
     * List of permission IDs to assign to this role.
     * This replaces all existing permissions.
     */
    private List<String> permissionIds;
}
