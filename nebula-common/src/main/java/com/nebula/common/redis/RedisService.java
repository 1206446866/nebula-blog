package com.nebula.common.redis;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.List;

@Slf4j
@Component
public class RedisService {
    private static final DefaultRedisScript<Long> RELEASE_OWNED = new DefaultRedisScript<>("""
            if redis.call('GET', KEYS[1]) == ARGV[1] then
                return redis.call('DEL', KEYS[1])
            end
            return 0
            """, Long.class);

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    /** 原子写入带有效期的占位；已有记录或Redis异常时返回false，不能退化为重复计数。 */
    public boolean setIfAbsent(String key, Object owner, long timeout, TimeUnit unit) {
        try {
            return Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(key, owner, timeout, unit));
        } catch (Exception e) {
            log.warn("Redis原子占位失败,key={}", key, e);
            return false;
        }
    }

    /** 仅释放仍属于本次操作的占位，避免补偿误删过期后由其他请求取得的新记录。 */
    public boolean deleteIfValue(String key, Object owner) {
        try {
            return Long.valueOf(1).equals(redisTemplate.execute(RELEASE_OWNED, List.of(key), owner));
        } catch (Exception e) {
            log.warn("Redis占位释放失败,key={}", key, e);
            return false;
        }
    }


    /**
     * 保存对象
     */
    public void set(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, value);
        } catch (Exception e) {
            log.warn("Redis写入失败,key={}", key, e);
        }
    }


    /**
     * 保存对象并设置过期时间
     */
    public void set(String key, Object value, long timeout, TimeUnit unit) {
        try {
            redisTemplate.opsForValue().set(key, value, timeout, unit);
        } catch (Exception e) {
            log.warn("Redis写入失败,key={}", key, e);
        }
    }


    /**
     * 获取对象
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        try {
            return (T) redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.warn("Redis读取失败,key={}", key, e);
            return null;
        }
    }


    /**
     * 删除
     */
    public Boolean delete(String key) {
        try {
            return redisTemplate.delete(key);
        } catch (Exception e) {
            log.warn("Redis删除失败,key={}", key, e);
            return false;
        }
    }


    /**
     * 判断存在
     */
    public Boolean hasKey(String key) {
        try {
            return redisTemplate.hasKey(key);
        } catch (Exception e) {
            log.warn("Redis检查key失败,key={}", key, e);
            return false;
        }
    }


    /**
     * 设置过期
     */
    public Boolean expire(String key, long timeout, TimeUnit unit) {
        try {
            return redisTemplate.expire(key, timeout, unit);
        } catch (Exception e) {
            log.warn("Redis设置过期失败,key={}", key, e);
            return false;
        }
    }

    /**
     * 获取过期时间（秒）
     */
    public Long getExpire(String key) {
        try {
            return redisTemplate.getExpire(key, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Redis获取过期时间失败,key={}", key, e);
            return -3L;
        }
    }
}
