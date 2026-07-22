package com.zhangyong.agent.channel;

import com.zhangyong.agent.config.WeComProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WeComCallbackControllerTest {

    @Test
    void validCallbackDelegatesProcessingToSeparateAsyncBean() {
        WeComCryptoUtil cryptoUtil = mock(WeComCryptoUtil.class);
        WeComProperties properties = new WeComProperties();
        WeComMessageProcessor processor = mock(WeComMessageProcessor.class);
        WeComCallbackController controller = new WeComCallbackController(cryptoUtil, properties, processor);

        when(cryptoUtil.verifySignature(anyString(), anyString(), anyString(), anyString())).thenReturn(true);
        when(cryptoUtil.decrypt("ciphertext")).thenReturn("""
            <xml>
              <FromUserName><![CDATA[ZhangYong]]></FromUserName>
              <CreateTime>1710000000</CreateTime>
              <MsgType><![CDATA[text]]></MsgType>
              <Content><![CDATA[我要做销售月报]]></Content>
              <MsgId>123456</MsgId>
            </xml>
            """);

        String result = controller.handleMessage(
            "signature", "timestamp", "nonce",
            "<xml><Encrypt><![CDATA[ciphertext]]></Encrypt></xml>"
        );

        assertEquals("success", result);
        verify(processor).processAsync(org.mockito.ArgumentMatchers.argThat(msg ->
            "ZhangYong".equals(msg.getFromUserName())
                && "我要做销售月报".equals(msg.getContent())
        ));
    }
}
