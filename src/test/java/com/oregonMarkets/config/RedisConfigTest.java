package com.oregonMarkets.config;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;

class RedisConfigTest {

  @Test
  void reactiveRedisTemplate_CreatesTemplate() {
    RedisConfig config = new RedisConfig();
    ReactiveRedisConnectionFactory factory = mock(ReactiveRedisConnectionFactory.class);

    ReactiveRedisTemplate<String, Object> template = config.reactiveRedisTemplate(factory);

    assertNotNull(template);
  }
}
