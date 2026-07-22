package com.zhangyong.agent.admin;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;

/**
 * 管理端鉴权 Filter
 *
 * 保护范围（按你之前的选择）：仅 /admin/** 路径（页面 + API）
 * 不影响：/wecom/callback、/wecom/health、其它业务路径
 *
 * Cookie 名称：ADMIN_SESSION_TOKEN
 * 校验：Redis 命中即放行；未命中 401 (API) / 302 → /admin/oauth/login (页面)
 */
@Slf4j
@Component
@Order(20)  // 高于 WeComCallback 不受影响（路径不重叠）
@RequiredArgsConstructor
public class AdminAuthFilter extends OncePerRequestFilter {

    public static final String COOKIE_NAME = "ADMIN_SESSION_TOKEN";
    public static final String ATTR_SESSION = "admin.session";

    /**
     * 不需要认证的白名单路径（OAuth 流程相关）
     *
     * 注意：/admin/oauth/me 不在白名单中，因为它需要 Filter 设置 session 属性后才能正确返回用户信息。
     * 企信内 OAuth 与外部浏览器扫码登录共用一个回调端点 /admin/oauth/callback。
     */
    private static final Set<String> ALLOW_LIST = Set.of(
        "/admin/oauth/login",
        "/admin/oauth/callback",
        "/admin/oauth/logout",
        "/admin/oauth/browser-helper",
        "/admin/oauth/open-in-wxwork"
    );

    private final AdminSessionService sessionService;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse resp, FilterChain chain)
        throws ServletException, IOException {

        String path = req.getRequestURI();
        if (!path.startsWith("/admin")) {
            chain.doFilter(req, resp);
            return;
        }

        // 为 React SPA 添加 CSP，限制外边脚本加载
        // 避免 Cloudflare 边缘自动注入的 beacon 脚本（type="module"，外部域名）
        // 打断企信内置浏览器的 HTML 解析
        if (path.equals("/admin") || path.equals("/admin/")
            || path.startsWith("/admin/app/")) {
            resp.setHeader("Content-Security-Policy",
                "default-src 'self'; " +
                "script-src 'self' 'unsafe-inline' 'unsafe-eval'; " +
                "style-src 'self' 'unsafe-inline'; " +
                "img-src 'self' data: https:; " +
                "font-src 'self' data:; " +
                "connect-src 'self' https://qyapi.weixin.qq.com;");
        }
        if (ALLOW_LIST.contains(path)) {
            chain.doFilter(req, resp);
            return;
        }

        String token = readCookie(req);
        Optional<AdminSessionService.SessionInfo> session = sessionService.find(token);
        if (session.isEmpty()) {
            handleUnauthenticated(req, resp, path);
            return;
        }

        // 命中：把 SessionInfo 放进 request，handle 用
        req.setAttribute(ATTR_SESSION, session.get());

        // /admin 与 /admin/ 转发到 React SPA 入口（新版管理端）
        if (path.equals("/admin") || path.equals("/admin/")) {
            req.getRequestDispatcher("/admin/app/index.html").forward(req, resp);
            return;
        }

        // /admin/app/** 未匹配具体资源时 fallback 到 /admin/app/index.html (SPA 路由)
        // 但有后缀的静态资源 (.js/.css/.png/...) 走 chain 让 Spring 静态资源处理器接管
        if (path.startsWith("/admin/app/") && !hasFileExtension(path)) {
            req.getRequestDispatcher("/admin/app/index.html").forward(req, resp);
            return;
        }

        chain.doFilter(req, resp);
    }

    /**
     * 判断 path 是否包含文件后缀（以 . 开头）
     */
    private boolean hasFileExtension(String path) {
        int lastSlash = path.lastIndexOf('/');
        String lastSegment = lastSlash >= 0 ? path.substring(lastSlash + 1) : path;
        return lastSegment.contains(".");
    }

    /**
     * 处理未认证请求：
     * - API 调用（已知 API 路径或 Accept 头明确要求 JSON）→ 返回 401 JSON
     * - 页面导航 → 302 跳转到企信 OAuth 或浏览器中转页
     */
    private void handleUnauthenticated(HttpServletRequest req, HttpServletResponse resp, String path)
        throws IOException {

        String ua = req.getHeader("User-Agent");
        boolean inWxwork = ua != null
            && (ua.contains("wxwork") || ua.contains("MicroMessenger"));

        // 判断是否为 API/数据请求（应返回 401 JSON 而非重定向）
        // 1) 已知 API 路径前缀或 .json 后缀
        // 2) /admin/oauth/me 是 fetch 调用，不是页面导航
        // 3) Accept 头存在且不含 text/html → 非页面请求
        boolean isApiPath = path.startsWith("/admin/api/")
            || path.endsWith(".json")
            || path.equals("/admin/oauth/me");

        boolean wantsJson = false;
        String accept = req.getHeader("Accept");
        if (accept != null && !accept.contains("text/html")) {
            wantsJson = true;
        }

        if (isApiPath || wantsJson) {
            resp.setStatus(HttpStatus.UNAUTHORIZED.value());
            resp.setContentType("application/json;charset=UTF-8");
            if (inWxwork) {
                resp.getWriter().write("{\"error\":\"unauthorized\"}");
            } else {
                resp.getWriter().write("{\"error\":\"unauthorized\",\"hint\":\"please login via WeCom at /admin/\"}");
            }
            return;
        }

        // 页面导航：跳转到对应的登录页
        if (inWxwork) {
            String redirect = "/admin/oauth/login?redirect=" + java.net.URLEncoder.encode(path, "UTF-8");
            resp.sendRedirect(redirect);
        } else {
            String helperUrl = "/admin/oauth/browser-helper?redirect="
                + java.net.URLEncoder.encode(path, "UTF-8");
            resp.sendRedirect(helperUrl);
        }
    }

    private String readCookie(HttpServletRequest req) {
        Cookie[] cookies = req.getCookies();
        if (cookies == null) return null;
        for (Cookie c : cookies) {
            if (COOKIE_NAME.equals(c.getName())) {
                return c.getValue();
            }
        }
        return null;
    }
}
