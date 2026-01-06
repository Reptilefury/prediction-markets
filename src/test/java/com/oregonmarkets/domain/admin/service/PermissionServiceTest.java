package com.oregonmarkets.domain.admin.service;

import com.oregonmarkets.domain.admin.dto.request.CreatePermissionRequest;
import com.oregonmarkets.domain.admin.dto.response.PermissionResponse;
import com.oregonmarkets.domain.admin.repository.AdminPermissionRepository;
import com.oregonmarkets.integration.keycloak.KeycloakAdminClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PermissionServiceTest {

    @Mock
    private KeycloakAdminClient keycloakAdminClient;

    @Mock
    private AdminPermissionRepository permissionRepository;

    @InjectMocks
    private PermissionService permissionService;

    private Map<String, Object> mockClientData;
    private Map<String, Object> mockPermissionData;

    @BeforeEach
    void setUp() {
        // Set the @Value field using reflection
        ReflectionTestUtils.setField(permissionService, "adminClientId", "oregon-admin-app");

        mockClientData = new HashMap<>();
        mockClientData.put("id", "client-123");
        mockClientData.put("clientId", "oregon-admin-app");

        mockPermissionData = new HashMap<>();
        mockPermissionData.put("id", "permission-123");
        mockPermissionData.put("name", "markets:create");
        mockPermissionData.put("description", "Create markets");
        mockPermissionData.put("clientRole", true);
    }

    @Test
    void getAllPermissions_ShouldReturnAllPermissions() {
        // Given
        when(keycloakAdminClient.getClientByClientId("oregon-admin-app"))
                .thenReturn(Mono.just(mockClientData));
        when(keycloakAdminClient.getClientRoles("client-123"))
                .thenReturn(Mono.just(List.of(mockPermissionData)));

        // When & Then
        StepVerifier.create(permissionService.getAllPermissions().collectList())
                .assertNext(permissions -> {
                    assertThat(permissions).hasSize(1);
                    PermissionResponse permission = permissions.get(0);
                    assertThat(permission.getName()).isEqualTo("markets:create");
                    assertThat(permission.getModule()).isEqualTo("markets");
                    assertThat(permission.getAction()).isEqualTo("create");
                })
                .verifyComplete();

        verify(keycloakAdminClient).getClientByClientId("oregon-admin-app");
        verify(keycloakAdminClient).getClientRoles("client-123");
    }

    @Test
    void getPermissionsByModule_ShouldReturnFilteredPermissions() {
        // Given
        when(keycloakAdminClient.getClientByClientId("oregon-admin-app"))
                .thenReturn(Mono.just(mockClientData));

        Map<String, Object> permission1 = new HashMap<>();
        permission1.put("id", "perm-1");
        permission1.put("name", "markets:create");
        permission1.put("description", "Create markets");

        Map<String, Object> permission2 = new HashMap<>();
        permission2.put("id", "perm-2");
        permission2.put("name", "users:create");
        permission2.put("description", "Create users");

        when(keycloakAdminClient.getClientRoles("client-123"))
                .thenReturn(Mono.just(List.of(permission1, permission2)));

        // When & Then
        StepVerifier.create(permissionService.getPermissionsByModule("markets").collectList())
                .assertNext(permissions -> {
                    assertThat(permissions).hasSize(1);
                    assertThat(permissions.get(0).getModule()).isEqualTo("markets");
                })
                .verifyComplete();
    }

    @Test
    void createPermission_ShouldCreateAndReturnPermission() {
        // Given
        CreatePermissionRequest request = CreatePermissionRequest.builder()
                .name("CreateMarket")
                .description("Permission to create markets")
                .module("markets")
                .action("create")
                .build();

        when(keycloakAdminClient.getClientByClientId("oregon-admin-app"))
                .thenReturn(Mono.just(mockClientData));
        when(keycloakAdminClient.createClientRole(eq("client-123"), any()))
                .thenReturn(Mono.empty());
        when(keycloakAdminClient.getClientRoles("client-123"))
                .thenReturn(Mono.just(List.of(mockPermissionData)));
        when(permissionRepository.findByKeycloakRoleId(anyString()))
                .thenReturn(Mono.empty());
        when(permissionRepository.save(any()))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        // When & Then
        StepVerifier.create(permissionService.createPermission(request))
                .assertNext(permission -> {
                    assertThat(permission.getModule()).isEqualTo("markets");
                    assertThat(permission.getAction()).isEqualTo("create");
                })
                .verifyComplete();

        verify(keycloakAdminClient).createClientRole(eq("client-123"), any());
    }
}
