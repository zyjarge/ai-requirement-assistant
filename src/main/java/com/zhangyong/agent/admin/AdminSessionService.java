package com.zhangyong.agent.admin;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;

/**
 * 管理端 session：Redis key=admin:session:{token}，value=JSON{userId,displayName,createdAt}
 *
 * TTL 24h（按你之前的选择）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminSessionService {

    private static final String KEY_PREFIX = "admin:session:";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final StringRedisTemplate redisTemplate;

    @Value("${app.admin.session-ttl-hours:24}")
    private long ttlHours;

    /**
     * 创建 session，返回随机 token
     */
    public String create(String userId, String displayName) {
        byte[] buf = new byte[24];
        RANDOM.nextBytes(buf);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
        String value = userId + "|" + (displayName == null ? "" : displayName);
        redisTemplate.opsForValue().set(KEY_PREFIX + token, value, Duration.ofHours(ttlHours));
        log.info("admin session created: user={} ttl={}h", userId, ttlHours);
        return token;
    }

    /**
     * 查询 session
     */
    public Optional<SessionInfo> find(String token) {
        if (token == null || token.isEmpty()) {
            return Optional.empty();
        }
        String value = redisTemplate.opsForValue().get(KEY_PREFIX + token);
        if (value == null) {
            return Optional.empty();
        }
        int sep = value.indexOf('|');
        String userId = sep < 0 ? value : value.substring(0, sep);
        String displayName = sep < 0 ? null : value.substring(sep + 1);
        if (displayName != null && displayName.isEmpty()) {
            displayName = null;
        }
        return Optional.of(new SessionInfo(token, userId, displayName));
    }

    /**
     * 续期 / 刷新（仅当存在时）
     */
    public void touch(String token) {
        Boolean exists = redisTemplate.hasKey(KEY_PREFIX + token);
        if (Boolean.TRUE.equals(exists)) {
            redisTemplate.expire(KEY_PREFIX + token, Duration.ofHours(ttlHours));
        }
    }

    /**
     * 销毁
     */
    public void destroy(String token) {
        redisTemplate.delete(KEY_PREFIX + token);
    }

    public record SessionInfo(String token, String userId, String displayName) { }
}
