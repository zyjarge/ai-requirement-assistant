package com.zhangyong.agent.admin;

import com.zhangyong.agent.config.WeComProperties;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminSessionServiceTest {

    @Test
    void createAndFindRoundTrip() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(ops);
        when(ops.get("admin:session:tok-1")).thenReturn("ZhangYong|张三");

        AdminSessionService svc = new AdminSessionService(redis);
        setTtl(svc, 24);

        String token = svc.create("ZhangYong", "张三");
        verify(ops).set(eq("admin:session:" + token), eq("ZhangYong|张三"), any(Duration.class));

        Optional<AdminSessionService.SessionInfo> info = svc.find("tok-1");
        assertTrue(info.isPresent());
        assertEquals("ZhangYong", info.get().userId());
        assertEquals("张三", info.get().displayName());
    }

    @Test
    void findWithoutDisplayName() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(ops);
        when(ops.get("admin:session:tok-2")).thenReturn("li4");

        AdminSessionService svc = new AdminSessionService(redis);
        Optional<AdminSessionService.SessionInfo> info = svc.find("tok-2");

        assertTrue(info.isPresent());
        assertEquals("li4", info.get().userId());
        assertTrue(info.get().displayName() == null);
    }

    @Test
    void findMissingReturnsEmpty() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(ops);
        when(ops.get("admin:session:missing")).thenReturn(null);

        AdminSessionService svc = new AdminSessionService(redis);
        assertFalse(svc.find("missing").isPresent());
    }

    @Test
    void destroyDeletesKey() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        AdminSessionService svc = new AdminSessionService(redis);

        svc.destroy("tok-x");
        verify(redis).delete("admin:session:tok-x");
    }

    private static void setTtl(AdminSessionService svc, long hours) {
        try {
            java.lang.reflect.Field f = AdminSessionService.class.getDeclaredField("ttlHours");
            f.setAccessible(true);
            f.setLong(svc, hours);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
