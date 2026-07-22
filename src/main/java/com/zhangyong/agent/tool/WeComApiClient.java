package com.zhangyong.agent.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhangyong.agent.config.WeComProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * 企业微信主动消息发送 Client
 *
 * 调用 /cgi-bin/message/send 发送应用消息
 */
@Slf4j
@Component
public class WeComApiClient {

    private static final String SEND_URL = "https://qyapi.weixin.qq.com/cgi-bin/message/send";

    private final AccessTokenManager accessTokenManager;
    private final WeComProperties properties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public WeComApiClient(AccessTokenManager accessTokenManager, WeComProperties properties) {
        this(accessTokenManager, properties, new RestTemplate());
    }

    WeComApiClient(AccessTokenManager accessTokenManager, WeComProperties properties, RestTemplate restTemplate) {
        this.accessTokenManager = accessTokenManager;
        this.properties = properties;
        this.restTemplate = restTemplate;
    }

    /**
     * 发送文本消息给指定用户
     *
     * @param userIds 用户 ID 列表（多个用 | 分隔）
     * @param content 文本内容
     * @return 是否成功
     */
    public boolean sendTextMessage(String userIds, String content) {
        String token = accessTokenManager.getAccessToken();
        String url = SEND_URL + "?access_token=" + token;

        Map<String, Object> body = Map.of(
            "touser", userIds,
            "msgtype", "text",
            "agentid", properties.getAgentId(),
            "text", Map.of("content", content),
            "safe", 0
        );

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            String resp = restTemplate.postForObject(url, entity, String.class);
            JsonNode response = objectMapper.readTree(resp);
            int errcode = response.path("errcode").asInt(-1);
            log.info("发送企业微信消息: to={}, errcode={}, errmsg={}",
                userIds, errcode, response.path("errmsg").asText(""));
            return errcode == 0;
        } catch (Exception e) {
            log.error("发送企业微信消息失败", e);
            return false;
        }
    }
}