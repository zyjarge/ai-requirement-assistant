package com.zhangyong.agent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 企业微信配置
 * 绑定 application.yml 中 wecom.* 配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "wecom")
public class WeComProperties {

    /** 企业 ID（CorpID） */
    private String corpId;

    /** 应用 AgentID */
    private String agentId;

    /** 应用 Secret */
    private String secret;

    /** 回调 Token（用于签名校验） */
    private String token;

    /** EncodingAESKey（43 字符，用于消息加解密） */
    private String encodingAesKey;

    /** 回调路径，默认 /wecom/callback */
    private String callbackPath;

    /** 试点测试用户列表 */
    private List<String> testUsers;

    /** access_token 缓存 key */
    public String getAccessTokenCacheKey() {
        return "wecom:access_token:" + corpId;
    }
}