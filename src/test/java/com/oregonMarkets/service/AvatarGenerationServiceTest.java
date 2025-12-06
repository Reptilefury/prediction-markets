package com.oregonMarkets.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.test.StepVerifier;

import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class AvatarGenerationServiceTest {

    private AvatarGenerationService avatarGenerationService;

    @BeforeEach
    void setUp() {
        avatarGenerationService = new AvatarGenerationService();
        ReflectionTestUtils.setField(avatarGenerationService, "gcpProjectId", "test-project");
        ReflectionTestUtils.setField(avatarGenerationService, "bucketName", "test-bucket");
    }

    @Test
    void generateAndUploadAvatar_Success() {
        UUID userId = UUID.randomUUID();

        StepVerifier.create(avatarGenerationService.generateAndUploadAvatar(userId))
            .expectNextMatches(url -> 
                url.contains("test-bucket") && 
                url.contains("avatars") && 
                url.contains(userId.toString()) &&
                url.endsWith(".png")
            )
            .verifyComplete();
    }

    @Test
    void generateAndUploadAvatar_NullUserId_ThrowsException() {
        StepVerifier.create(avatarGenerationService.generateAndUploadAvatar(null))
            .expectError(RuntimeException.class)
            .verify();
    }
}