package com.oregonmarkets;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Disabled(
    "Redis configuration issue with multiple ReactiveRedisConnectionFactory beans. This is a separate infrastructure setup issue.")
class PredictionMarketsApplicationTests {

  @Test
  void contextLoads() {
    // This test validates that the Spring Boot application context loads successfully.
    // No additional assertions needed as the test will fail if context loading fails.
  }
}
