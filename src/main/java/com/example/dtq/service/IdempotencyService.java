package com.example.dtq.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.data.redis.core.StringRedisTemplate;


@Service
public class IdempotencyService {
    private final StringRedisTemplate redis;

    public IdempotencyService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    private static final java.time.Duration IDEMPOTENCY_TTL= java.time.Duration.ofHours(24);

    public String getJobIdForKey(String tenantId, String idempotencyKey){
        String redisKey="idem:"+tenantId+":"+ idempotencyKey;
        return redis.opsForValue().get(redisKey);
    }

    public boolean saveJobIdIfAbsent(String tenantId, String idempotencyKey,String jobId){
        String redisKey="idem:"+tenantId+":"+idempotencyKey;
        Boolean result=redis.opsForValue().setIfAbsent(redisKey,jobId,IDEMPOTENCY_TTL);
        return Boolean.TRUE.equals(result);
    }

}
