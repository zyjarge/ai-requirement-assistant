package com.zhangyong.agent.admin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhangyong.agent.config.WeComProperties;
import com.zhangyong.agent.tool.AccessTokenManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * 企业微信 OAuth 客户端
 *
 * snsapi_base：仅拿 userid（成员 UserID）
 * 用 userid 另调 /cgi-bin/user/get 拿 display_name（姓名）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WeComOAuthClient {

    /** 外部浏览器扫码登录 URL */
    public static final String QR_CONNECT_URL = "https://open.work.weixin.qq.com/wwopen/sso/qrConnect";

    private final WeComProperties properties;
    private final AccessTokenManager accessTokenManager;
    private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public static final String SCOPE_BASE = "snsapi_base";

    /**
     * 拼出企业微信内嵌浏览器 OAuth URL（企信工作台内打开时用）
     */
    public String buildAuthorizeUrl(String redirectUri, String state) {
        StringBuilder sb = new StringBuilder("https://open.weixin.qq.com/connect/oauth2/authorize?");
        sb.append("appid=").append(properties.getCorpId());
        sb.append("&redirect_uri=").append(urlEncode(redirectUri));
        sb.append("&response_type=code");
        sb.append("&scope=").append(SCOPE_BASE);
        sb.append("&state=").append(urlEncode(state));
        sb.append("&agentid=").append(properties.getAgentId());
        sb.append("#wechat_redirect");
        return sb.toString();
    }

    /**
     * 拼出外部浏览器扫码登录 URL（mac Safari 打开会显示二维码）
     */
    public String buildQrConnectUrl(String redirectUri, String state) {
        return QR_CONNECT_URL
            + "?appid=" + urlEncode(properties.getCorpId())
            + "&agentid=" + urlEncode(properties.getAgentId())
            + "&redirect_uri=" + urlEncode(redirectUri)
            + "&state=" + urlEncode(state);
    }

    /**
     * 用 code 换 userid（snsapi_base 走 /cgi-bin/auth/getuserinfo）
     */
    public String exchangeCodeForUserId(String code) throws IOException, InterruptedException {
        String token = accessTokenManager.getAccessToken();
        String url = "https://qyapi.weixin.qq.com/cgi-bin/auth/getuserinfo"
            + "?access_token=" + token + "&code=" + urlEncode(code);
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
            throw new IOException("getuserinfo failed: errcode=" + errcode
                + " errmsg=" + root.path("errmsg").asText(""));
        }
        String userId = root.path("userid").asText("");
        if (userId.isEmpty()) {
            // snsapi_base 返回的是 UserId；snsapi_userinfo 是 OpenId
            // 部分账号 / 第三方应用没有 userid 字段，需要回退到 OpenId
            String openId = root.path("OpenId").asText("");
            if (openId.isEmpty()) {
                throw new IOException("getuserinfo returned no userid/openid: " + resp.body());
            }
            return openId;
        }
        return userId;
    }

    /**
     * 通过 userid 拿成员姓名（用于前端展示）。失败时返回 null，前端 fallback 到 userid。
     */
    public String fetchDisplayName(String userId) {
        try {
            String token = accessTokenManager.getAccessToken();
            String url = "https://qyapi.weixin.qq.com/cgi-bin/user/get"
                + "?access_token=" + token + "&userid=" + urlEncode(userId);
            HttpResponse<String> resp = httpClient.send(
                HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build(),
                HttpResponse.BodyHandlers.ofString()
            );
            JsonNode root = objectMapper.readTree(resp.body());
            int errcode = root.path("errcode").asInt(-1);
            if (errcode != 0) {
                log.warn("user/get failed: errcode={} errmsg={}", errcode, root.path("errmsg").asText(""));
                return null;
            }
            String name = root.path("name").asText("");
            return name.isEmpty() ? null : name;
        } catch (Exception e) {
            log.warn("fetchDisplayName({}) failed: {}", userId, e.toString());
            return null;
        }
    }

    private static String urlEncode(String s) {
        return java.net.URLEncoder.encode(s, java.nio.charset.StandardCharsets.UTF_8);
    }
}
