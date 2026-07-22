package com.zhangyong.agent.tool;

import com.zhangyong.agent.config.WeComProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class WeComApiClientTest {

    @Test
    void sendTextMessageUsesConfiguredAgentId() {
        AccessTokenManager tokenManager = mock(AccessTokenManager.class);
        when(tokenManager.getAccessToken()).thenReturn("test-token");

        WeComProperties properties = new WeComProperties();
        properties.setAgentId("1000003");

        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        server.expect(requestTo("https://qyapi.weixin.qq.com/cgi-bin/message/send?access_token=test-token"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().json("""
                {
                  "touser": "ZhangYong",
                  "msgtype": "text",
                  "agentid": "1000003",
                  "text": {"content": "处理完成"},
                  "safe": 0
                }
                """))
            .andRespond(withSuccess("{\"errcode\":0,\"errmsg\":\"ok\"}", MediaType.APPLICATION_JSON));

        WeComApiClient client = new WeComApiClient(tokenManager, properties, restTemplate);

        org.junit.jupiter.api.Assertions.assertTrue(client.sendTextMessage("ZhangYong", "处理完成"));
        server.verify();
    }
}
