package com.oregonmarkets.domain.admin.service;

import com.oregonmarkets.domain.admin.dto.request.CreateAdminUserRequest;
import com.oregonmarkets.domain.admin.dto.request.UpdateAdminUserRequest;
import com.oregonmarkets.domain.admin.dto.response.AdminUserResponse;
import com.oregonmarkets.domain.admin.exception.AdminUserAlreadyExistsException;
import com.oregonmarkets.domain.admin.exception.AdminUserNotFoundException;
import com.oregonmarkets.domain.admin.model.AdminUser;
import com.oregonmarkets.domain.admin.repository.AdminUserRepository;
import com.oregonmarkets.domain.admin.repository.AdminRoleRepository;
import com.oregonmarkets.domain.admin.repository.AdminPermissionRepository;
import com.oregonmarkets.integration.keycloak.KeycloakAdminClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock
    private AdminUserRepository adminUserRepository;
    
    @Mock
    private AdminRoleRepository adminRoleRepository;
    
    @Mock
    private AdminPermissionRepository adminPermissionRepository;

    @Mock
    private KeycloakAdminClient keycloakAdminClient;

    @InjectMocks
    private AdminUserService adminUserService;

    private UUID testUserId;
    private UUID testRoleId;
    private AdminUser testAdminUser;
    private CreateAdminUserRequest createRequest;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testRoleId = UUID.randomUUID();
        
        testAdminUser = AdminUser.builder()
            .id(testUserId)
            .firstName("John")
            .lastName("Doe")
            .username("jdoe")
            .email("john.doe@example.com")
            .roleId(testRoleId)
            .status(AdminUser.AdminUserStatus.ACTIVE)
            .twoFactorEnabled(false)
            .build();

        createRequest = new CreateAdminUserRequest();
        createRequest.setFirstName("John");
        createRequest.setLastName("Doe");
        createRequest.setEmail("john.doe@example.com");
        createRequest.setRoleId(testRoleId);
        createRequest.setTwoFactorEnabled(false);
    }

    @Test
    void createAdminUser_ShouldCreateSuccessfully() {
        // Given
        when(adminUserRepository.existsByEmail(anyString())).thenReturn(Mono.just(false));
        when(adminRoleRepository.findById(testRoleId)).thenReturn(Mono.just(new com.oregonmarkets.domain.admin.model.AdminRole()));
        when(keycloakAdminClient.createAdminUser(any())).thenReturn(Mono.just("keycloak-user-id"));
        when(keycloakAdminClient.getRoleByName(anyString())).thenReturn(Mono.just(java.util.Map.of("id", "role-id")));
        when(keycloakAdminClient.assignRealmRolesToUser(anyString(), anyList())).thenReturn(Mono.empty());
        when(adminUserRepository.save(any(AdminUser.class))).thenReturn(Mono.just(testAdminUser));
        when(adminRoleRepository.findById(testRoleId)).thenReturn(Mono.just(new com.oregonmarkets.domain.admin.model.AdminRole()));
        when(adminPermissionRepository.findByRoleId(testRoleId)).thenReturn(Flux.empty());

        // When & Then
        StepVerifier.create(adminUserService.createAdminUser(createRequest))
            .assertNext(response -> {
                assertThat(response.getFirstName()).isEqualTo("John");
                assertThat(response.getLastName()).isEqualTo("Doe");
                assertThat(response.getUsername()).isEqualTo("jdoe");
            })
            .verifyComplete();
    }

    @Test
    void createAdminUser_ShouldThrowExceptionWhenEmailExists() {
        // Given
        when(adminUserRepository.existsByEmail(anyString())).thenReturn(Mono.just(true));

        // When & Then
        StepVerifier.create(adminUserService.createAdminUser(createRequest))
            .expectError(AdminUserAlreadyExistsException.class)
            .verify();
    }

    @Test
    void getAdminUser_ShouldReturnUser() {
        // Given
        when(adminUserRepository.findById(testUserId)).thenReturn(Mono.just(testAdminUser));
        when(adminRoleRepository.findById(testRoleId)).thenReturn(Mono.just(new com.oregonmarkets.domain.admin.model.AdminRole()));
        when(adminPermissionRepository.findByRoleId(testRoleId)).thenReturn(Flux.empty());

        // When & Then
        StepVerifier.create(adminUserService.getAdminUser(testUserId))
            .assertNext(response -> {
                assertThat(response.getId()).isEqualTo(testUserId);
                assertThat(response.getFirstName()).isEqualTo("John");
            })
            .verifyComplete();
    }

    @Test
    void getAdminUser_ShouldThrowExceptionWhenNotFound() {
        // Given
        when(adminUserRepository.findById(testUserId)).thenReturn(Mono.empty());

        // When & Then
        StepVerifier.create(adminUserService.getAdminUser(testUserId))
            .expectError(AdminUserNotFoundException.class)
            .verify();
    }

    @Test
    void deleteAdminUser_ShouldDeleteSuccessfully() {
        // Given
        testAdminUser.setKeycloakUserId("keycloak-user-id");
        when(adminUserRepository.findById(testUserId)).thenReturn(Mono.just(testAdminUser));
        when(keycloakAdminClient.deleteUser("keycloak-user-id")).thenReturn(Mono.empty());
        when(adminUserRepository.delete(testAdminUser)).thenReturn(Mono.empty());

        // When & Then
        StepVerifier.create(adminUserService.deleteAdminUser(testUserId))
            .verifyComplete();

        verify(keycloakAdminClient).deleteUser("keycloak-user-id");
        verify(adminUserRepository).delete(testAdminUser);
    }
}
