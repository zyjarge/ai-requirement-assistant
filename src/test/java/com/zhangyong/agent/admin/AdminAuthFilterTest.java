package com.zhangyong.agent.admin;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminAuthFilterTest {

    @Test
    void nonAdminPathPassesThrough() throws Exception {
        AdminSessionService svc = mock(AdminSessionService.class);
        AdminAuthFilter filter = new AdminAuthFilter(svc);
        FilterChain chain = mock(FilterChain.class);

        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/wecom/health");
        MockHttpServletResponse resp = new MockHttpServletResponse();

        filter.doFilter(req, resp, chain);
        verify(chain).doFilter(req, resp);
    }

    @Test
    void oauthCallbackAlwaysAllowed() throws Exception {
        AdminSessionService svc = mock(AdminSessionService.class);
        AdminAuthFilter filter = new AdminAuthFilter(svc);
        FilterChain chain = mock(FilterChain.class);

        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/admin/oauth/callback");
        req.setQueryString("code=xxx&state=yyy");
        MockHttpServletResponse resp = new MockHttpServletResponse();

        filter.doFilter(req, resp, chain);
        verify(chain).doFilter(req, resp);
    }

    @Test
    void adminApiWithoutSessionReturns401() throws Exception {
        AdminSessionService svc = mock(AdminSessionService.class);
        when(svc.find(any())).thenReturn(Optional.empty());
        AdminAuthFilter filter = new AdminAuthFilter(svc);
        FilterChain chain = mock(FilterChain.class);

        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/admin/api/requirements");
        MockHttpServletResponse resp = new MockHttpServletResponse();

        filter.doFilter(req, resp, chain);

        assertEquals(401, resp.getStatus());
        assertEquals("application/json;charset=UTF-8", resp.getContentType());
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void adminPageWithoutSessionRedirectsToLogin() throws Exception {
        AdminSessionService svc = mock(AdminSessionService.class);
        when(svc.find(any())).thenReturn(Optional.empty());
        AdminAuthFilter filter = new AdminAuthFilter(svc);
        FilterChain chain = mock(FilterChain.class);

        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/admin/");
        // 企信 App UA，走 OAuth login
        req.addHeader("User-Agent", "wxwork/4.0.0");
        MockHttpServletResponse resp = new MockHttpServletResponse();

        filter.doFilter(req, resp, chain);

        assertEquals(302, resp.getStatus());
        String location = resp.getHeader("Location");
        assertEquals("/admin/oauth/login?redirect=%2Fadmin%2F", location);
    }

    @Test
    void adminPageWithValidSessionPassesThrough() throws Exception {
        AdminSessionService svc = mock(AdminSessionService.class);
        AdminSessionService.SessionInfo s = new AdminSessionService.SessionInfo("tok-1", "ZhangYong", "张三");
        when(svc.find("tok-1")).thenReturn(Optional.of(s));
        AdminAuthFilter filter = new AdminAuthFilter(svc);
        FilterChain chain = mock(FilterChain.class);

        // /admin/index.html 走静态资源，但 filter 先校验 session
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/admin/index.html");
        req.setCookies(new Cookie("ADMIN_SESSION_TOKEN", "tok-1"));
        MockHttpServletResponse resp = new MockHttpServletResponse();

        filter.doFilter(req, resp, chain);

        verify(chain).doFilter(req, resp);
        assertEquals(s, req.getAttribute(AdminAuthFilter.ATTR_SESSION));
    }

    @Test
    void browserUaOnAdminPageRedirectsToHelper() throws Exception {
        AdminSessionService svc = mock(AdminSessionService.class);
        when(svc.find(any())).thenReturn(Optional.empty());
        AdminAuthFilter filter = new AdminAuthFilter(svc);
        FilterChain chain = mock(FilterChain.class);

        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/admin/");
        req.addHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Safari/605.1.15");
        MockHttpServletResponse resp = new MockHttpServletResponse();

        filter.doFilter(req, resp, chain);

        assertEquals(302, resp.getStatus());
        String loc = resp.getHeader("Location");
        assertNotNull(loc);
        assertTrue(loc.contains("/admin/oauth/browser-helper"), "expected helper page, got: " + loc);
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void wxworkUaOnAdminPageRedirectsToOauthLogin() throws Exception {
        AdminSessionService svc = mock(AdminSessionService.class);
        when(svc.find(any())).thenReturn(Optional.empty());
        AdminAuthFilter filter = new AdminAuthFilter(svc);
        FilterChain chain = mock(FilterChain.class);

        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/admin/");
        req.addHeader("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 16_0) AppleWebKit/605.1.15 wxwork/4.0.0 MicroMessenger/8.0.49");
        MockHttpServletResponse resp = new MockHttpServletResponse();

        filter.doFilter(req, resp, chain);

        assertEquals(302, resp.getStatus());
        String loc = resp.getHeader("Location");
        assertNotNull(loc);
        assertTrue(loc.contains("/admin/oauth/login"), "expected oauth login, got: " + loc);
    }

    @Test
    void browserHelperAlwaysAllowed() throws Exception {
        AdminSessionService svc = mock(AdminSessionService.class);
        AdminAuthFilter filter = new AdminAuthFilter(svc);
        FilterChain chain = mock(FilterChain.class);

        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/admin/oauth/browser-helper");
        req.addHeader("User-Agent", "Mozilla/5.0 Safari");
        MockHttpServletResponse resp = new MockHttpServletResponse();

        filter.doFilter(req, resp, chain);
        verify(chain).doFilter(req, resp);
    }
}
