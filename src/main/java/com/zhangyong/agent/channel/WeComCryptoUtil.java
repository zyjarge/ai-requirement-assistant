package com.zhangyong.agent.channel;

import com.zhangyong.agent.config.WeComProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;

/**
 * 企业微信加解密 + 签名工具
 *
 * 使用 NoPadding + 手动 PKCS#7 unpad 避免中文 UTF-8 密文 BadPaddingException
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WeComCryptoUtil {

    private final WeComProperties properties;
    private static final int BLOCK_SIZE = 16;

    /**
     * SHA1 签名校验
     */
    public boolean verifySignature(String msgSignature, String timestamp, String nonce, String encrypt) {
        String expected = computeSignature(timestamp, nonce, encrypt);
        boolean match = expected.equals(msgSignature);
        if (!match) {
            log.warn("签名不匹配: 期望={}, 收到={}", expected, msgSignature);
        }
        return match;
    }

    public String computeSignature(String timestamp, String nonce, String encrypt) {
        String[] params = { properties.getToken(), timestamp, nonce, encrypt };
        Arrays.sort(params);
        return sha1(String.join("", params));
    }

    /**
     * 解密企业微信消息
     */
    public String decrypt(String encrypt) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(properties.getEncodingAesKey() + "=");
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(keyBytes, 0, BLOCK_SIZE);

            Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);

            byte[] encrypted = Base64.getDecoder().decode(encrypt);
            byte[] decrypted = cipher.doFinal(encrypted);

            decrypted = pkcs7Unpad(decrypted);

            byte[] networkOrder = Arrays.copyOfRange(decrypted, 16, 20);
            int msgLen = ((networkOrder[0] & 0xff) << 24)
                       | ((networkOrder[1] & 0xff) << 16)
                       | ((networkOrder[2] & 0xff) << 8)
                       | (networkOrder[3] & 0xff);

            byte[] msgBytes = Arrays.copyOfRange(decrypted, 20, 20 + msgLen);
            return new String(msgBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("解密失败: {}", e.getMessage(), e);
            throw new RuntimeException("解密企业微信消息失败", e);
        }
    }

    /**
     * 加密企业微信消息（用于回复）
     */
    public String encrypt(String replyMsg) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(properties.getEncodingAesKey() + "=");
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(keyBytes, 0, BLOCK_SIZE);

            byte[] randomBytes = new byte[16];
            new java.security.SecureRandom().nextBytes(randomBytes);

            byte[] msgBytes = replyMsg.getBytes(StandardCharsets.UTF_8);
            byte[] lenBytes = new byte[]{
                (byte) ((msgBytes.length >> 24) & 0xff),
                (byte) ((msgBytes.length >> 16) & 0xff),
                (byte) ((msgBytes.length >> 8) & 0xff),
                (byte) (msgBytes.length & 0xff)
            };
            byte[] receiveId = properties.getCorpId().getBytes(StandardCharsets.UTF_8);

            byte[] content = new byte[16 + 4 + msgBytes.length + receiveId.length];
            System.arraycopy(randomBytes, 0, content, 0, 16);
            System.arraycopy(lenBytes, 0, content, 16, 4);
            System.arraycopy(msgBytes, 0, content, 20, msgBytes.length);
            System.arraycopy(receiveId, 0, content, 20 + msgBytes.length, receiveId.length);

            byte[] padded = pkcs7Pad(content, BLOCK_SIZE);
            Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
            byte[] encrypted = cipher.doFinal(padded);

            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            log.error("加密失败: {}", e.getMessage(), e);
            throw new RuntimeException("加密企业微信消息失败", e);
        }
    }

    /**
     * PKCS#7 padding
     */
    private byte[] pkcs7Pad(byte[] data, int blockSize) {
        int padLen = blockSize - (data.length % blockSize);
        byte[] padded = new byte[data.length + padLen];
        System.arraycopy(data, 0, padded, 0, data.length);
        for (int i = data.length; i < padded.length; i++) {
            padded[i] = (byte) padLen;
        }
        return padded;
    }

    /**
     * 手动 PKCS#7 unpadding - 容忍各种 padding 长度（包括 16 字节的 PKCS#7 标准）
     */
    private byte[] pkcs7Unpad(byte[] data) {
        if (data.length == 0) return data;
        int lastByte = data[data.length - 1] & 0xFF;
        if (lastByte < 1 || lastByte > BLOCK_SIZE) {
            return data;
        }
        for (int i = data.length - lastByte; i < data.length; i++) {
            if ((data[i] & 0xFF) != lastByte) {
                return data;
            }
        }
        return Arrays.copyOfRange(data, 0, data.length - lastByte);
    }

    private String sha1(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] digest = md.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("SHA1 计算失败", e);
        }
    }
}
