package com.zhangyong.agent.channel;

import com.zhangyong.agent.tool.WeComApiClient;
import com.zhangyong.agent.workflow.DemandPipelineService;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Async;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WeComMessageProcessorTest {

    @Test
    void textMessageIsProcessedAndSentByDedicatedAsyncBean() throws Exception {
        DemandPipelineService pipelineService = mock(DemandPipelineService.class);
        WeComApiClient apiClient = mock(WeComApiClient.class);
        WeComMessage msg = new WeComMessage();
        msg.setMsgType("text");
        msg.setFromUserName("ZhangYong");
        when(pipelineService.handle(msg)).thenReturn("下一条追问");

        WeComMessageProcessor processor = new WeComMessageProcessor(pipelineService, apiClient);
        processor.processAsync(msg);

        verify(pipelineService).handle(msg);
        verify(apiClient).sendTextMessage("ZhangYong", "下一条追问");
        assertNotNull(
            WeComMessageProcessor.class.getMethod("processAsync", WeComMessage.class)
                .getAnnotation(Async.class)
        );
    }
}
