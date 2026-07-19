package com.nebula.common.redis;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class RedisService {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;


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

}