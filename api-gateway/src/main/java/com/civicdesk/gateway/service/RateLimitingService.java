package com.civicdesk.gateway.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

/**
 * Rate Limiting Service - Implements distributed rate limiting using Redis.
 * Uses a sliding window counter approach with per-second granularity.
 * 
 * Note: Currently not used by RateLimitingFilter (which uses in-memory counting).
 * This service can be used for future Redis-backed distributed rate limiting.
 */
@Service
public class RateLimitingService {
    private static final Logger log = LoggerFactory.getLogger(RateLimitingService.class);

    private final ReactiveRedisTemplate<String, String> redisTemplate;

    @Value("${gateway.rate-limit.requests-per-minute:600}")
    private int requestsPerMinute;

    @Value("${gateway.rate-limit.requests-per-second:10}")
    private int requestsPerSecond;

    public RateLimitingService(ReactiveRedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Check if a request is allowed for the given client.
     * Implements token bucket algorithm with second-level resolution.
     */
    public Mono<Boolean> isRequestAllowed(String clientId, String path) {
        String key = buildRedisKey(clientId, path);
        long currentSecond = System.currentTimeMillis() / 1000;

        return redisTemplate.opsForValue()
                .get(key)
                .switchIfEmpty(Mono.just("0"))
                .flatMap(countStr -> {
                    long currentCount = Long.parseLong(countStr);
                    
                    if (currentCount < requestsPerSecond) {
                        // Increment and allow
                        return redisTemplate.opsForValue()
                                .set(key, String.valueOf(currentCount + 1), Duration.ofSeconds(2))
                                .then(Mono.just(true));
                    } else {
                        // Rate limit exceeded
                        log.debug("Rate limit exceeded for client: {} path: {} count: {}", 
                                clientId, path, currentCount);
                        return Mono.just(false);
                    }
                })
                .retryWhen(Retry.backoff(2, Duration.ofMillis(10))
                        .filter(err -> !(err instanceof TimeoutException)))
                .onErrorReturn(true); // Fail open on errors
    }

    /**
     * Get remaining requests for a client.
     */
    public Mono<Long> getRemainingRequests(String clientId, String path) {
        String key = buildRedisKey(clientId, path);
        
        return redisTemplate.opsForValue()
                .get(key)
                .switchIfEmpty(Mono.just("0"))
                .map(countStr -> Math.max(0L, requestsPerSecond - Long.parseLong(countStr)))
                .onErrorReturn(0L);
    }

    /**
     * Reset rate limit for a client (admin operation).
     */
    public Mono<Boolean> resetRateLimit(String clientId, String path) {
        String key = buildRedisKey(clientId, path);
        return redisTemplate.delete(key).map(deletedCount -> deletedCount > 0);
    }

    private String buildRedisKey(String clientId, String path) {
        long currentSecond = System.currentTimeMillis() / 1000;
        return String.format("ratelimit:%s:%s:%d", clientId, path, currentSecond);
    }
}
