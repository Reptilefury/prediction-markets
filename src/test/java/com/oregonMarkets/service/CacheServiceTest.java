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
    private ReactiveValueOperations<String, Object> valueOperations;

    private CacheService cacheService;

    @BeforeEach
    void setUp() {
        cacheService = new CacheService(redisTemplate);
    }

    @Test
    void set_Success() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.set(eq("key1"), eq("value1"), any(Duration.class)))
                .thenReturn(Mono.just(true));

        StepVerifier.create(cacheService.set("key1", "value1", Duration.ofMinutes(5)))
                .verifyComplete();
    }

    @Test
    void set_Failure() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.set(eq("key1"), eq("value1"), any(Duration.class)))
                .thenReturn(Mono.error(new RuntimeException("Redis error")));

        StepVerifier.create(cacheService.set("key1", "value1", Duration.ofMinutes(5)))
                .expectError(RuntimeException.class)
                .verify();
    }

    @Test
    void get_Success() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("key1")).thenReturn(Mono.just("value1"));

        StepVerifier.create(cacheService.get("key1"))
                .expectNext("value1")
                .verifyComplete();
    }

    @Test
    void get_NotFound() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("key1")).thenReturn(Mono.empty());

        StepVerifier.create(cacheService.get("key1"))
                .verifyComplete();
    }

    @Test
    void get_Failure() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("key1")).thenReturn(Mono.error(new RuntimeException("Redis error")));

        StepVerifier.create(cacheService.get("key1"))
                .expectError(RuntimeException.class)
                .verify();
    }

    @Test
    void delete_Success() {
        when(redisTemplate.delete("key1")).thenReturn(Mono.just(1L));

        StepVerifier.create(cacheService.delete("key1"))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void delete_NotFound() {
        when(redisTemplate.delete("key1")).thenReturn(Mono.just(0L));

        StepVerifier.create(cacheService.delete("key1"))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void delete_Failure() {
        when(redisTemplate.delete("key1")).thenReturn(Mono.error(new RuntimeException("Redis error")));

        StepVerifier.create(cacheService.delete("key1"))
                .expectError(RuntimeException.class)
                .verify();
    }

    @Test
    void exists_True() {
        when(redisTemplate.hasKey("key1")).thenReturn(Mono.just(true));

        StepVerifier.create(cacheService.exists("key1"))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void exists_False() {
        when(redisTemplate.hasKey("key1")).thenReturn(Mono.just(false));

        StepVerifier.create(cacheService.exists("key1"))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void exists_Failure() {
        when(redisTemplate.hasKey("key1")).thenReturn(Mono.error(new RuntimeException("Redis error")));

        StepVerifier.create(cacheService.exists("key1"))
                .expectError(RuntimeException.class)
                .verify();
    }
}
