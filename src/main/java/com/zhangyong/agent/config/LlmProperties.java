package com.zhangyong.agent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * LLM 配置
 *
 * 绑定 application.yml 中 agentscope.openai.* 配置
 * 用作 MiniMax / DeepSeek / Qwen 等 OpenAI 兼容 provider
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "agentscope.anthropic")
public class LlmProperties {

    /** API Key */
    private String apiKey;

    /** 模型名 */
    private String modelName;

    /** Base URL（如 https://api.minimax.io/v1） */
    private String baseUrl;

    /** Endpoint path（默认 /chat/completions） */
    private String endpointPath = "/chat/completions";

    /** 是否流式 */
    private Boolean stream = false;
}