package com.zhangyong.agent.channel;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 企业微信 XML 消息解析
 *
 * 消息格式示例：
 * <xml>
 *   <ToUserName><![CDATA[corpid]]></ToUserName>
 *   <FromUserName><![CDATA[userid]]></FromUserName>
 *   <CreateTime>1348831860</CreateTime>
 *   <MsgType><![CDATA[text]]></MsgType>
 *   <Content><![CDATA[hello]]></Content>
 *   <MsgId>1234567890123456</MsgId>
 * </xml>
 */
@Slf4j
@Data
public class WeComMessage {

    private String toUserName;
    private String fromUserName;
    private long createTime;
    private String msgType;
    private String content;
    private String msgId;
    private String event;

    /**
     * 解析 XML 字符串为 WeComMessage 对象
     */
    public static WeComMessage parse(String xml) {
        WeComMessage msg = new WeComMessage();
        try {
            SAXReader reader = new SAXReader();
            Document doc = reader.read(new StringReader(xml));
            Element root = doc.getRootElement();

            msg.setToUserName(textOf(root, "ToUserName"));
            msg.setFromUserName(textOf(root, "FromUserName"));
            String ct = textOf(root, "CreateTime");
            if (ct != null) msg.setCreateTime(Long.parseLong(ct));
            msg.setMsgType(textOf(root, "MsgType"));
            msg.setContent(textOf(root, "Content"));
            msg.setMsgId(textOf(root, "MsgId"));
            msg.setEvent(textOf(root, "Event"));
        } catch (DocumentException e) {
            log.error("解析企业微信 XML 失败: {}", e.getMessage());
            throw new RuntimeException("解析企业微信消息失败", e);
        }
        return msg;
    }

    /**
     * 序列化为 XML（用于回复消息）
     */
    public String toXml() {
        StringBuilder sb = new StringBuilder();
        sb.append("<xml>");
        appendElement(sb, "ToUserName", toUserName);
        appendElement(sb, "FromUserName", fromUserName);
        sb.append("<CreateTime>").append(createTime).append("</CreateTime>");
        appendElement(sb, "MsgType", msgType);
        appendElement(sb, "Content", content);
        if (msgId != null) {
            sb.append("<MsgId>").append(msgId).append("</MsgId>");
        }
        sb.append("</xml>");
        return sb.toString();
    }

    private static void appendElement(StringBuilder sb, String name, String value) {
        if (value == null) return;
        sb.append("<").append(name).append(">");
        if (value.contains("<") || value.contains("&") || value.contains(">")) {
            sb.append("<![CDATA[").append(value).append("]]>");
        } else {
            sb.append(value);
        }
        sb.append("</").append(name).append(">");
    }

    private static String textOf(Element root, String name) {
        List<Element> nodes = root.elements(name);
        if (nodes == null || nodes.isEmpty()) return null;
        return nodes.get(0).getTextTrim();
    }

    /**
     * 用于回复消息的工厂方法
     */
    public static WeComMessage buildReply(String toUser, String fromUser, String content) {
        WeComMessage reply = new WeComMessage();
        reply.setToUserName(toUser);
        reply.setFromUserName(fromUser);
        reply.setCreateTime(System.currentTimeMillis() / 1000);
        reply.setMsgType("text");
        reply.setContent(content);
        return reply;
    }
}