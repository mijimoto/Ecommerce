package com.mijimoto.ECommerce.auth.services;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class RedisTokenService {

    private final RedisTemplate<String,String> redis;
    public static final String PREFIX = "auth:access:";

    public RedisTokenService(RedisTemplate<String,String> redis) {
        this.redis = redis;
    }

    public void storeJti(String jti, String payloadJson, long ttlSeconds) {
        String key = PREFIX + jti;
        redis.opsForValue().set(key, payloadJson, Duration.ofSeconds(ttlSeconds));
    }

    public boolean jtiExists(String jti) {
        String key = PREFIX + jti;
        Boolean b = redis.hasKey(key);
        return Boolean.TRUE.equals(b);
    }

    public void deleteJti(String jti) {
        redis.delete(PREFIX + jti);
    }
}
