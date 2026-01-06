package com.oregonmarkets.domain.admin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleResponse {

    private String id;
    private String name;
    private String description;
    private boolean composite;
    private boolean clientRole;
    private List<PermissionResponse> permissions;
}
