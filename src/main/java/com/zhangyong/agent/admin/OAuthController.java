package com.zhangyong.agent.admin;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

/**
 * 企业微信 OAuth 入口 / 回调 / 登出
 *
 * 路径：
 *  - GET /admin/oauth/login?redirect=/admin/   → 302 到企业微信扫码页
 *  - GET /admin/oauth/callback?code=...&state=...  → 换 userid，set cookie，302 到 redirect
 *  - GET /admin/oauth/logout                   → 清 cookie，302 到 /admin/oauth/login
 */
@Slf4j
@RestController
@RequestMapping("/admin/oauth")
@RequiredArgsConstructor
public class OAuthController {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final WeComOAuthClient oauthClient;
    private final AdminSessionService sessionService;

    @Value("${app.admin.oauth.redirect-base:}")
    private String redirectBase;

    @GetMapping("/login")
    public void login(@RequestParam(value = "redirect", defaultValue = "/admin/") String redirect,
                      HttpServletRequest req,
                      HttpServletResponse resp) throws IOException {
        // 外部浏览器（非企信内）→ 跳到扫码中转页
        if (!isWxworkUA(req)) {
            String helperUrl = "/admin/oauth/browser-helper?redirect=" + URLEncoder.encode(redirect, StandardCharsets.UTF_8);
            resp.setStatus(HttpStatus.FOUND.value());
            resp.setHeader("Location", helperUrl);
            return;
        }

        // 企信内浏览器 → 走 OAuth 自动授权
        String state = randomState();
        String callback = redirectBase + "/admin/oauth/callback";
        String url = oauthClient.buildAuthorizeUrl(callback, state);
        // redirect 暂存到 cookie（取时校验）
        Cookie r = new Cookie("ADMIN_OAUTH_REDIRECT", URLEncoder.encode(redirect, StandardCharsets.UTF_8));
        r.setPath("/");
        r.setMaxAge(300);
        // 暂存 state（防伪）
        Cookie s = new Cookie("ADMIN_OAUTH_STATE", state);
        s.setPath("/");
        s.setMaxAge(300);
        resp.addCookie(r);
        resp.addCookie(s);
        resp.setStatus(HttpStatus.FOUND.value());
        resp.setHeader("Location", url);
    }

    @GetMapping("/callback")
    public void callback(@RequestParam("code") String code,
                         @RequestParam(value = "state", required = false) String state,
                         HttpServletRequest req,
                         HttpServletResponse resp) throws IOException {
        // 1) 校验 state
        //    - 企信内 OAuth（/admin/oauth/login）：有 ADMIN_OAUTH_STATE cookie，严格校验
        //    - 外部浏览器扫码（/admin/oauth/browser-helper）：state 固定为 "browser_helper"
        String expectedState = readCookie(req, "ADMIN_OAUTH_STATE");
        String redirect = readCookie(req, "ADMIN_OAUTH_REDIRECT");
        if (redirect == null || redirect.isEmpty()) redirect = "/admin/";

        boolean stateOk = (expectedState != null && state != null && expectedState.equals(state))
            || "browser_helper".equals(state);
        if (!stateOk) {
            log.warn("OAuth state mismatch: expected={} got={}", expectedState, state);
            resp.setStatus(HttpStatus.BAD_REQUEST.value());
            resp.getWriter().write("state mismatch");
            return;
        }

        // 2) code → userid
        String userId;
        try {
            userId = oauthClient.exchangeCodeForUserId(code);
        } catch (Exception e) {
            log.error("OAuth exchangeCodeForUserId failed", e);
            resp.setStatus(HttpStatus.BAD_GATEWAY.value());
            resp.getWriter().write("getuserinfo failed: " + e.getMessage());
            return;
        }
        log.info("OAuth login userid={}", userId);

        // 3) 拿 display name（最佳努力）
        String displayName = oauthClient.fetchDisplayName(userId);

        // 4) 建 session
        String token = sessionService.create(userId, displayName);

        // 5) set cookie + 清临时 cookie
        Cookie sessionCookie = new Cookie(AdminAuthFilter.COOKIE_NAME, token);
        sessionCookie.setPath("/");
        sessionCookie.setMaxAge((int) (24 * 60 * 60));
        sessionCookie.setHttpOnly(false);   // 你之前选了"不强制"
        sessionCookie.setSecure(false);     // 你之前选了"不强制"
        resp.addCookie(sessionCookie);

        Cookie clearState = new Cookie("ADMIN_OAUTH_STATE", "");
        clearState.setPath("/");
        clearState.setMaxAge(0);
        resp.addCookie(clearState);

        resp.setStatus(HttpStatus.FOUND.value());
        resp.setHeader("Location", redirect);
    }

    @GetMapping("/logout")
    public void logout(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String token = null;
        if (req.getCookies() != null) {
            for (Cookie c : req.getCookies()) {
                if (AdminAuthFilter.COOKIE_NAME.equals(c.getName())) {
                    token = c.getValue();
                }
            }
        }
        if (token != null) {
            sessionService.destroy(token);
        }
        Cookie clear = new Cookie(AdminAuthFilter.COOKIE_NAME, "");
        clear.setPath("/");
        clear.setMaxAge(0);
        resp.addCookie(clear);

        // 外部浏览器 → 跳到扫码中转页；企信内 → 走 OAuth 登录
        if (isWxworkUA(req)) {
            resp.setStatus(HttpStatus.FOUND.value());
            resp.setHeader("Location", "/admin/oauth/login");
        } else {
            resp.setStatus(HttpStatus.FOUND.value());
            resp.setHeader("Location", "/admin/oauth/browser-helper");
        }
    }

    /**
     * 判断是否在企信内嵌浏览器中
     */
    private boolean isWxworkUA(HttpServletRequest req) {
        String ua = req.getHeader("User-Agent");
        return ua != null && (ua.contains("wxwork") || ua.contains("MicroMessenger"));
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me(HttpServletRequest req) {
        Object attr = req.getAttribute(AdminAuthFilter.ATTR_SESSION);
        if (!(attr instanceof AdminSessionService.SessionInfo s)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "unauthorized"));
        }
        return ResponseEntity.ok(Map.of(
            "userId", s.userId(),
            "displayName", s.displayName() == null ? "" : s.displayName()
        ));
    }

    private String readCookie(HttpServletRequest req, String name) {
        if (req.getCookies() == null) return null;
        for (Cookie c : req.getCookies()) {
            if (name.equals(c.getName())) return c.getValue();
        }
        return null;
    }

    private String randomState() {
        byte[] buf = new byte[16];
        RANDOM.nextBytes(buf);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
    }
}
