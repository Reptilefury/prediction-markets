package com.oregonMarkets.config;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class RedisConfigTest {

    @Test
    void reactiveRedisConnectionFactory_ShouldReturnValidFactory() {
        RedisConfig config = new RedisConfig();
        ReflectionTestUtils.setField(config, "host", "localhost");
        ReflectionTestUtils.setField(config, "port", 6379);
        
        ReactiveRedisConnectionFactory factory = config.reactiveRedisConnectionFactory();
        assertNotNull(factory);
    }

    @Test
    void reactiveRedisTemplate_ShouldReturnValidTemplate() {
        RedisConfig config = new RedisConfig();
        ReflectionTestUtils.setField(config, "host", "localhost");
        ReflectionTestUtils.setField(config, "port", 6379);
        
        ReactiveRedisConnectionFactory factory = config.reactiveRedisConnectionFactory();
        ReactiveRedisTemplate<String, Object> template = config.reactiveRedisTemplate(factory);
        assertNotNull(template);
    }
}