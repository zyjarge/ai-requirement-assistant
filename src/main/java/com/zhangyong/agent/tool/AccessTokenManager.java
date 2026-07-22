package com.zhangyong.agent.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhangyong.agent.config.WeComProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Map;

/**
 * 企业微信 access_token 管理
 *
 * - 自动缓存到 Redis（key: wecom:access_token:{corpId}）
 * - access_token 有效期 7200 秒，提前 5 分钟刷新
 * - 调用 /cgi-bin/token 获取
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccessTokenManager {

    private static final String TOKEN_URL = "https://qyapi.weixin.qq.com/cgi-bin/gettoken";
    private static final String CACHE_KEY_PREFIX = "wecom:access_token:";
    /** 提前 5 分钟刷新（避免临界过期） */
    private static final long REFRESH_MARGIN_SECONDS = 300;

    private final WeComProperties properties;
    private final StringRedisTemplate redisTemplate;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 获取有效的 access_token（带 Redis 缓存）
     */
    public String getAccessToken() {
        String cacheKey = CACHE_KEY_PREFIX + properties.getCorpId();
        String cached = redisTemplate.opsForValue().get(cacheKey);

        if (cached != null && !cached.isEmpty()) {
            log.debug("从 Redis 命中 access_token");
            return cached;
        }

        if (cached != null && cached.isEmpty()) {
            log.warn("检测到 Redis 中存在空 access_token，删除后重新获取");
            redisTemplate.delete(cacheKey);
        }

        return refreshAccessToken();
    }

    /**
     * 主动刷新 access_token
     */
    public String refreshAccessToken() {
        log.info("刷新企业微信 access_token ...");
        String url = TOKEN_URL + "?corpid=" + properties.getCorpId() + "&corpsecret=" + properties.getSecret();
        try {
            String resp = restTemplate.getForObject(url, String.class);
            JsonNode node = objectMapper.readTree(resp);

            int errcode = node.path("errcode").asInt(0);
            if (errcode != 0) {
                String errmsg = node.path("errmsg").asText("unknown");
                throw new RuntimeException("获取 access_token 失败: errcode=" + errcode + ", errmsg=" + errmsg);
            }

            String token = node.path("access_token").asText();
            long expiresIn = node.path("expires_in").asLong(7200);

            // 缓存（减去提前刷新时间）
            long cacheSeconds = Math.max(expiresIn - REFRESH_MARGIN_SECONDS, 60);
            String cacheKey = CACHE_KEY_PREFIX + properties.getCorpId();
            redisTemplate.opsForValue().set(cacheKey, token, Duration.ofSeconds(cacheSeconds));

            log.info("access_token 刷新成功，缓存 {} 秒", cacheSeconds);
            return token;
        } catch (Exception e) {
            log.error("刷新 access_token 失败", e);
            throw new RuntimeException("刷新 access_token 失败", e);
        }
    }
}