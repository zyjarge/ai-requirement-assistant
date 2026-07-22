package com.zhangyong.agent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * AI 需求助理 启动入口
 *
 * 启动方式：
 *   mvn spring-boot:run
 *   或 java -jar target/ai-requirement-assistant.jar
 */
@SpringBootApplication
@EnableAsync
public class AgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgentApplication.class, args);
        System.out.println("\n" +
            "====================================================\n" +
            "  AI 需求助理 已启动\n" +
            "  监听端口: 9080\n" +
            "  企业微信回调: /wecom/callback\n" +
            "====================================================\n");
    }
}