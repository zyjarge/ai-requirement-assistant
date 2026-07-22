package com.zhangyong.agent.admin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhangyong.agent.tool.AccessTokenManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 企微成员目录，缓存 10 分钟（Redis）。
 *
 * 用于派单下拉列表。简单场景：一次 /cgi-bin/user/list 拉全部。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WeComUserDirectory {

    private static final String CACHE_KEY = "wecom:userlist:cache";
    private static final Duration CACHE_TTL = Duration.ofMinutes(10);

    private final AccessTokenManager accessTokenManager;
    private final StringRedisTemplate redisTemplate;
    private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public static class User {
        public String userid;
        public String name;
        public User() {}
        public User(String userid, String name) { this.userid = userid; this.name = name; }
    }

    /**
     * 拿全部成员（缓存 10 分钟）
     */
    public List<User> listAll() {
        String cached = redisTemplate.opsForValue().get(CACHE_KEY);
        if (cached != null) {
            try {
                JsonNode arr = objectMapper.readTree(cached);
                List<User> out = new ArrayList<>();
                for (JsonNode n : arr) {
                    out.add(new User(n.path("userid").asText(), n.path("name").asText("")));
                }
                return out;
            } catch (Exception e) {
                log.warn("userlist cache parse failed, refetching", e);
            }
        }
        return fetchAndCache();
    }

    private List<User> fetchAndCache() {
        try {
            String token = accessTokenManager.getAccessToken();
            String url = "https://qyapi.weixin.qq.com/cgi-bin/user/simplelist"
                + "?access_token=" + token + "&department_id=1&fetch_child=1";
            HttpResponse<String> resp = httpClient.send(
                HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build(),
                HttpResponse.BodyHandlers.ofString()
            );
            JsonNode root = objectMapper.readTree(resp.body());
            int errcode = root.path("errcode").asInt(-1);
            if (errcode != 0) {
                log.warn("user/simplelist failed: errcode={} errmsg={}",
                    errcode, root.path("errmsg").asText(""));
                return List.of();
            }
            JsonNode userlist = root.path("userlist");
            redisTemplate.opsForValue().set(CACHE_KEY, userlist.toString(), CACHE_TTL);

            List<User> out = new ArrayList<>();
            for (JsonNode n : userlist) {
                out.add(new User(n.path("userid").asText(), n.path("name").asText("")));
            }
            return out;
        } catch (Exception e) {
            log.warn("fetchAndCache userlist failed", e);
            return List.of();
        }
    }
}
