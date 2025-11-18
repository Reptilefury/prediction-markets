package com.oregonMarkets.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class CacheService {
    
    private final ReactiveRedisTemplate<String, Object> redisTemplate;
    
    public Mono<Void> set(String key, Object value, Duration ttl) {
        return redisTemplate.opsForValue()
            .set(key, value, ttl)
            .doOnSuccess(result -> log.debug("Cached key: {}", key))
            .doOnError(error -> log.error("Failed to cache key {}: {}", key, error.getMessage()))
            .then();
    }
    
    public Mono<Object> get(String key) {
        return redisTemplate.opsForValue()
            .get(key)
            .doOnSuccess(result -> log.debug("Retrieved key: {} = {}", key, result != null))
            .doOnError(error -> log.error("Failed to get key {}: {}", key, error.getMessage()));
    }
    
    public Mono<Boolean> delete(String key) {
        return redisTemplate.delete(key)
            .map(count -> count > 0)
            .doOnSuccess(deleted -> log.debug("Deleted key: {} = {}", key, deleted))
            .doOnError(error -> log.error("Failed to delete key {}: {}", key, error.getMessage()));
    }
    
    public Mono<Boolean> exists(String key) {
        return redisTemplate.hasKey(key)
            .doOnError(error -> log.error("Failed to check key {}: {}", key, error.getMessage()));
    }
}