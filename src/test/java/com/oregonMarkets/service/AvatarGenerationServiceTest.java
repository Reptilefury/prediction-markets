package com.oregonMarkets.service;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

class AvatarGenerationServiceTest {

  @Test
  void generateAndUploadAvatar_Success() {
    AvatarGenerationService service = new AvatarGenerationService();
    UUID userId = UUID.randomUUID();

    StepVerifier.create(service.generateAndUploadAvatar(userId))
        .expectNextMatches(url -> url.contains("avatars") && url.contains(userId.toString()))
        .verifyComplete();
  }

  @Test
  void generateAndUploadAvatar_NullUserId() {
    AvatarGenerationService service = new AvatarGenerationService();

    StepVerifier.create(service.generateAndUploadAvatar(null))
        .expectNextMatches(url -> url.contains("mock-bucket") && url.contains("unknown"))
        .verifyComplete();
  }
}
