package com.zhangyong.agent.channel;

import com.zhangyong.agent.config.WeComProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/wecom")
@RequiredArgsConstructor
public class WeComCallbackController {

    private final WeComCryptoUtil cryptoUtil;
    private final WeComProperties wecomProperties;
    private final WeComMessageProcessor messageProcessor;

    @GetMapping("/callback")
    public void verifyUrl(@RequestParam String msg_signature,
                          @RequestParam String timestamp,
                          @RequestParam String nonce,
                          @RequestParam String echostr,
                          jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {
        if (!cryptoUtil.verifySignature(msg_signature, timestamp, nonce, echostr)) {
            response.getWriter().write("signature error");
            return;
        }
        try {
            response.setContentType("text/plain; charset=utf-8");
            response.getWriter().write(cryptoUtil.decrypt(echostr));
        } catch (Exception e) {
            response.getWriter().write("decrypt error");
        }
    }

    @PostMapping(value = "/callback")
    public String handleMessage(@RequestParam String msg_signature,
                                @RequestParam String timestamp,
                                @RequestParam String nonce,
                                @RequestBody String rawXml) {
        log.info("[POST] 收到企业微信消息");

        String encrypt = extractEncrypt(rawXml);
        if (encrypt == null || !cryptoUtil.verifySignature(msg_signature, timestamp, nonce, encrypt)) {
            log.error("[POST] 签名校验失败或无密文");
            return "error";
        }

        String decryptedXml;
        try {
            decryptedXml = cryptoUtil.decrypt(encrypt);
        } catch (Exception e) {
            log.error("[POST] 解密失败", e);
            return "error";
        }

        WeComMessage msg = WeComMessage.parse(decryptedXml);
        log.info("[POST] 解析: from={}, content={}", msg.getFromUserName(), msg.getContent());

        messageProcessor.processAsync(msg);

        return "success";
    }

    private String extractEncrypt(String rawXml) {
        if (rawXml == null) return null;
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(
            "<Encrypt>\\s*<!\\[CDATA\\[(.*?)\\]\\]>\\s*</Encrypt>",
            java.util.regex.Pattern.DOTALL
        );
        java.util.regex.Matcher m = p.matcher(rawXml);
        return m.find() ? m.group(1) : null;
    }

    @GetMapping("/health")
    public java.util.Map<String, Object> health() {
        String aesKey = wecomProperties.getEncodingAesKey();
        return java.util.Map.of(
            "status", "UP",
            "service", "ai-requirement-assistant",
            "wecom", java.util.Map.of(
                "corpId", wecomProperties.getCorpId(),
                "agentId", wecomProperties.getAgentId(),
                "callbackPath", wecomProperties.getCallbackPath()
            )
        );
    }
}
