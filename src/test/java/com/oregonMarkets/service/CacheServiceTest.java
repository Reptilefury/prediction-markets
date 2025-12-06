package com.oregonMarkets.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CacheServiceTest {

    @Mock
    private ReactiveRedisTemplate<String, Object> redisTemplate;
    
    @Mock
    private ReactiveValueOperations<String, Object> valueOps;

    private CacheService cacheService;

    @BeforeEach
    void setUp() {
        cacheService = new CacheService(redisTemplate);
    }

    @Test
    void set_Success() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.set(eq("key"), eq("value"), any(Duration.class)))
            .thenReturn(Mono.just(true));

        StepVerifier.create(cacheService.set("key", "value", Duration.ofMinutes(5)))
            .verifyComplete();
    }

    @Test
    void set_Error() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.set(eq("key"), eq("value"), any(Duration.class)))
            .thenReturn(Mono.error(new RuntimeException("Redis error")));

        StepVerifier.create(cacheService.set("key", "value", Duration.ofMinutes(5)))
            .expectError(RuntimeException.class)
            .verify();
    }

    @Test
    void get_Success() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("key"))
            .thenReturn(Mono.just("value"));

        StepVerifier.create(cacheService.get("key"))
            .expectNext("value")
            .verifyComplete();
    }

    @Test
    void get_NotFound() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("key"))
            .thenReturn(Mono.empty());

        StepVerifier.create(cacheService.get("key"))
            .verifyComplete();
    }

    @Test
    void delete_Success() {
        when(redisTemplate.delete("key"))
            .thenReturn(Mono.just(1L));

        StepVerifier.create(cacheService.delete("key"))
            .expectNext(true)
            .verifyComplete();
    }

    @Test
    void delete_NotFound() {
        when(redisTemplate.delete("key"))
            .thenReturn(Mono.just(0L));

        StepVerifier.create(cacheService.delete("key"))
            .expectNext(false)
            .verifyComplete();
    }

    @Test
    void exists_True() {
        when(redisTemplate.hasKey("key"))
            .thenReturn(Mono.just(true));

        StepVerifier.create(cacheService.exists("key"))
            .expectNext(true)
            .verifyComplete();
    }

    @Test
    void exists_False() {
        when(redisTemplate.hasKey("key"))
            .thenReturn(Mono.just(false));

        StepVerifier.create(cacheService.exists("key"))
            .expectNext(false)
            .verifyComplete();
    }
}