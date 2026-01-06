package com.oregonmarkets.domain.admin.service;

import com.oregonmarkets.domain.admin.dto.request.CreateRoleRequest;
import com.oregonmarkets.domain.admin.dto.response.RoleResponse;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    @InjectMocks
    private RoleService roleService;

    private Map<String, Object> mockRoleData;

    @BeforeEach
    void setUp() {
        mockRoleData = new HashMap<>();
        mockRoleData.put("id", "role-123");
        mockRoleData.put("name", "admin");
        mockRoleData.put("description", "Administrator role");
        mockRoleData.put("composite", false);
        mockRoleData.put("clientRole", false);
    }

    @Test
    void getAllRoles_ShouldReturnAllRoles() {
        // Given
        when(keycloakAdminClient.getRealmRoles())
                .thenReturn(Mono.just(List.of(mockRoleData)));

        // When & Then
        StepVerifier.create(roleService.getAllRoles().collectList())
                .assertNext(roles -> {
                    assertThat(roles).hasSize(1);
                    RoleResponse role = roles.get(0);
                    assertThat(role.getName()).isEqualTo("admin");
                    assertThat(role.getDescription()).isEqualTo("Administrator role");
                })
                .verifyComplete();

        verify(keycloakAdminClient).getRealmRoles();
    }

    @Test
    void getRoleByName_ShouldReturnRole() {
        // Given
        when(keycloakAdminClient.getRoleByName("admin"))
                .thenReturn(Mono.just(mockRoleData));

        // When & Then
        StepVerifier.create(roleService.getRoleByName("admin"))
                .assertNext(role -> {
                    assertThat(role.getId()).isEqualTo("role-123");
                    assertThat(role.getName()).isEqualTo("admin");
                })
                .verifyComplete();

        verify(keycloakAdminClient).getRoleByName("admin");
    }

    @Test
    void createRole_ShouldCreateAndReturnRole() {
        // Given
        CreateRoleRequest request = CreateRoleRequest.builder()
                .name("moderator")
                .description("Moderator role")
                .build();

        Map<String, Object> newRoleData = new HashMap<>();
        newRoleData.put("id", "role-456");
        newRoleData.put("name", "moderator");
        newRoleData.put("description", "Moderator role");
        newRoleData.put("composite", false);
        newRoleData.put("clientRole", false);

        when(keycloakAdminClient.createRealmRole(any()))
                .thenReturn(Mono.empty());
        when(keycloakAdminClient.getRoleByName("moderator"))
                .thenReturn(Mono.just(newRoleData));
        when(roleRepository.findByKeycloakRoleId(anyString()))
                .thenReturn(Mono.empty());
        when(roleRepository.save(any()))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        // When & Then
        StepVerifier.create(roleService.createRole(request))
                .assertNext(role -> {
                    assertThat(role.getName()).isEqualTo("moderator");
                    assertThat(role.getDescription()).isEqualTo("Moderator role");
                })
                .verifyComplete();

        verify(keycloakAdminClient).createRealmRole(any());
        verify(keycloakAdminClient, times(2)).getRoleByName("moderator");
    }
}
