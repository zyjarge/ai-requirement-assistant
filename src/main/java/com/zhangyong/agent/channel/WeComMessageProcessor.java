package com.zhangyong.agent.channel;

import com.zhangyong.agent.tool.WeComApiClient;
import com.zhangyong.agent.workflow.DemandPipelineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 在独立 Spring Bean 中异步处理企业微信消息，避免回调线程等待 LLM 调用。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WeComMessageProcessor {

    private final DemandPipelineService pipelineService;
    private final WeComApiClient weComApiClient;

    @Async
    public void processAsync(WeComMessage msg) {
        try {
            String replyContent;
            if ("text".equals(msg.getMsgType())) {
                replyContent = pipelineService.handle(msg);
            } else if ("event".equals(msg.getMsgType())) {
                replyContent = handleEvent(msg);
            } else {
                replyContent = "暂不支持的消息类型：" + msg.getMsgType();
            }

            log.info("[Async] 处理完成，准备主动发送消息给 user={}", msg.getFromUserName());
            boolean sent = weComApiClient.sendTextMessage(msg.getFromUserName(), replyContent);
            log.info("[Async] 主动发送消息结果: sent={}", sent);
        } catch (Exception e) {
            log.error("[Async] 处理异常", e);
        }
    }

    private String handleEvent(WeComMessage msg) {
        if ("subscribe".equals(msg.getEvent())) {
            return "欢迎使用 AI 需求助理！\n\n请直接发送您想提的需求。";
        }
        return "收到事件：" + msg.getEvent();
    }
}
