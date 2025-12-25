package com.oregonmarkets.domain.user.dto.response;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oregonmarkets.domain.user.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UserProfileMapperTest {

    private UserProfileMapper mapper;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mapper = new UserProfileMapper(objectMapper);
    }

    @Test
    void shouldMapBasicUserFields() {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .email("test@example.com")
                .username("testuser")
                .magicWalletAddress("0x123")
                .build();

        UserRegistrationResponse response = mapper.toResponse(user);

        assertEquals(userId, response.getUserId());
        assertEquals("test@example.com", response.getEmail());
        assertEquals("testuser", response.getUsername());
        assertEquals("0x123", response.getMagicWalletAddress());
    }

    @Test
    void shouldHandleNullJsonString() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .enclaveDepositAddresses(null)
                .build();

        UserRegistrationResponse response = mapper.toResponse(user);

        assertNull(response.getEnclaveDepositAddresses());
    }

    @Test
    void shouldHandleEmptyJsonString() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .enclaveDepositAddresses("")
                .build();

        UserRegistrationResponse response = mapper.toResponse(user);

        assertNull(response.getEnclaveDepositAddresses());
    }

    @Test
    void shouldParseValidJsonString() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .enclaveDepositAddresses("{\"btc\":\"address1\"}")
                .build();

        UserRegistrationResponse response = mapper.toResponse(user);

        assertNotNull(response.getEnclaveDepositAddresses());
    }
}
