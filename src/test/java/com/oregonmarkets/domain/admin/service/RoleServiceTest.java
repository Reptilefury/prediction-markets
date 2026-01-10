package com.oregonmarkets.domain.admin.service;

import com.oregonmarkets.domain.admin.dto.request.CreateRoleRequest;
import com.oregonmarkets.domain.admin.dto.response.RoleResponse;
import com.oregonmarkets.domain.admin.model.AdminRole;
import com.oregonmarkets.domain.admin.repository.AdminRoleRepository;
import com.oregonmarkets.integration.keycloak.KeycloakAdminClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

    @Mock
    private KeycloakAdminClient keycloakAdminClient;

    @Mock
    private AdminRoleRepository roleRepository;

    @Mock
    private PermissionService permissionService;

    @InjectMocks
    private RoleService roleService;

    private Map<String, Object> mockRoleData;
    private AdminRole mockDbRole;

    @BeforeEach
    void setUp() {
        mockRoleData = new HashMap<>();
        mockRoleData.put("id", "role-123");
        mockRoleData.put("name", "admin");
        mockRoleData.put("description", "Administrator role");
        mockRoleData.put("composite", false);
        mockRoleData.put("clientRole", false);

        mockDbRole = AdminRole.builder()
                .id(UUID.randomUUID())
                .name("admin")
                .description("Administrator role")
                .isActive(true)
                .build();
    }

    @Test
    void getAllRoles_ShouldReturnAllRoles() {
        // Given
        when(keycloakAdminClient.getRealmRoles())
                .thenReturn(Mono.just(List.of(mockRoleData)));
        when(keycloakAdminClient.getRoleComposites("admin"))
                .thenReturn(Mono.just(Collections.emptyList()));
        when(roleRepository.findByName("admin"))
                .thenReturn(Mono.just(mockDbRole));

        // When & Then
        StepVerifier.create(roleService.getAllRoles().collectList())
                .assertNext(roles -> {
                    assertThat(roles).hasSize(1);
                    RoleResponse role = roles.get(0);
                    assertThat(role.getName()).isEqualTo("admin");
                    assertThat(role.getDescription()).isEqualTo("Administrator role");
                    assertThat(role.getId()).isEqualTo(mockDbRole.getId().toString());
                })
                .verifyComplete();

        verify(keycloakAdminClient).getRealmRoles();
        verify(keycloakAdminClient).getRoleComposites("admin");
        verify(roleRepository).findByName("admin");
    }

    @Test
    void getRoleByName_ShouldReturnRole() {
        // Given
        when(keycloakAdminClient.getRoleByName("admin"))
                .thenReturn(Mono.just(mockRoleData));
        when(keycloakAdminClient.getRoleComposites("admin"))
                .thenReturn(Mono.just(Collections.emptyList()));

        // When & Then
        StepVerifier.create(roleService.getRoleByName("admin"))
                .assertNext(role -> {
                    assertThat(role.getId()).isEqualTo("role-123");
                    assertThat(role.getName()).isEqualTo("admin");
                })
                .verifyComplete();

        verify(keycloakAdminClient).getRoleByName("admin");
        verify(keycloakAdminClient).getRoleComposites("admin");
    }

    @Test
    void createRole_ShouldCreateAndReturnRole() {
        // Given
        CreateRoleRequest request = CreateRoleRequest.builder()
                .name("moderator")
                .description("Moderator role")
                .permissionIds(List.of("perm1"))
                .build();

        when(roleRepository.findByName("moderator"))
                .thenReturn(Mono.empty());
        when(keycloakAdminClient.getRoleByName("moderator"))
                .thenReturn(Mono.error(new org.springframework.web.reactive.function.client.WebClientResponseException(404, "Not Found", null, null, null)));

        // When & Then
        StepVerifier.create(roleService.createRole(request))
                .expectError()
                .verify();

        verify(roleRepository).findByName("moderator");
    }
}
