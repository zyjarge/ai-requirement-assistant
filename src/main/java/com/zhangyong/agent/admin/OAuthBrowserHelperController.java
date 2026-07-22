package com.zhangyong.agent.admin;

import com.zhangyong.agent.config.WeComProperties;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 浏览器中转页：用户在普通浏览器（非企信 App）打开 /admin/ 时显示。
 *
 * 展示二维码供企信 App 扫码登录，扫码后跳转到 OAuth 回调完成认证。
 * 与企信内嵌浏览器 OAuth 共用同一个 Agent（Agent A），使用同一个回调端点。
 */
@RestController
@RequestMapping("/admin/oauth")
@RequiredArgsConstructor
public class OAuthBrowserHelperController {

    private final WeComOAuthClient oauthClient;
    private final WeComProperties wecomProperties;

    @Value("${app.admin.oauth.redirect-base:}")
    private String redirectBase;

    /**
     * 中转页：使用企信官方 WxLogin JS 组件显示二维码
     */
    @GetMapping(value = "/browser-helper", produces = MediaType.TEXT_HTML_VALUE)
    public String helper(@RequestParam(value = "redirect", defaultValue = "/admin/") String redirect) {
        // 复用主 OAuth callback（与企信内嵌浏览器 OAuth 共用同一端点）
        String callback = redirectBase + "/admin/oauth/callback";
        String callbackEnc = enc(callback);

        String wxworkUrl = "wxwork://open.weixin.qq.com/connect/oauth2/authorize"
            + "?appid=" + wecomProperties.getCorpId()
            + "&agentid=" + wecomProperties.getAgentId()
            + "&redirect_uri=" + callbackEnc
            + "&response_type=code"
            + "&scope=snsapi_base"
            + "&state=browser_helper"
            + "#wechat_redirect";

        String iframeSrc = "https://open.work.weixin.qq.com/wwopen/sso/qrConnect"
            + "?appid=" + esc(wecomProperties.getCorpId())
            + "&agentid=" + esc(wecomProperties.getAgentId())
            + "&redirect_uri=" + esc(callbackEnc)
            + "&state=browser_helper";

        return """
            <!DOCTYPE html>
            <html lang="zh-CN">
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
              <title>扫码登录管理端</title>
              <style>
                body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", "PingFang SC", sans-serif;
                       margin: 0; padding: 40px 20px; background: #f5f6f8; color: #1a1a1a; }
                .card { max-width: 560px; margin: 0 auto; background: #fff; border-radius: 8px;
                        box-shadow: 0 2px 8px rgba(0,0,0,.06); padding: 32px; }
                h1 { font-size: 18px; color: #1a73e8; margin: 0 0 8px; }
                h2 { font-size: 15px; margin: 20px 0 8px; }
                p { line-height: 1.6; color: #333; font-size: 14px; }
                .url-box { background: #f6f8fa; border: 1px solid #d0d0d0; border-radius: 4px;
                           padding: 12px; font-family: ui-monospace, "SF Mono", Menlo, monospace;
                           font-size: 12px; word-break: break-all; color: #444; user-select: all; }
                .btn { display: inline-block; padding: 10px 20px; margin: 4px 4px 4px 0;
                       background: #1a73e8; color: #fff; border: none; border-radius: 4px;
                       text-decoration: none; font-size: 14px; cursor: pointer; font: inherit; }
                .btn.secondary { background: #fff; color: #1a73e8; border: 1px solid #1a73e8; }
                .btn:hover { opacity: 0.92; }
                .note { background: #e8f0fe; border-left: 3px solid #1a73e8; padding: 10px 14px;
                        font-size: 13px; color: #1967d2; margin: 16px 0; border-radius: 0 4px 4px 0; }
                ol { line-height: 1.8; padding-left: 20px; }
                .copy-feedback { color: #137333; font-size: 12px; margin-left: 8px; display: none; }
                code { background: #eef; padding: 1px 6px; border-radius: 3px; font-size: 12px; }
                hr { margin: 24px 0; border: none; border-top: 1px solid #ececec; }
                details { margin-top: 8px; }
                summary { cursor: pointer; color: #1a73e8; font-size: 13px; }
              </style>
              <script src="https://open.work.weixin.qq.com/wwopen/js/jweixin-1.0.0.js"></script>
            </head>
            <body>
              <div class="card">
                <h1>📱 扫码登录管理端</h1>
                <p>用企业微信 App 扫一扫下方二维码，授权后会自动跳到管理端。</p>

                <div class="note">
                  <b>用法：</b>打开企业微信 → 点右上角"+" → 扫一扫 → 对准下方二维码
                </div>

                <div id="wxlogin" style="text-align:center; min-height: 320px; display:flex; flex-direction:column; align-items:center; justify-content:center;">
                  <p style="color:#999; font-size:13px;">加载中...</p>
                </div>

                <details>
                  <summary>没装企信？其它登录方式</summary>
                  <div style="padding: 12px 0 0; color: #555;">
                    <h2 style="margin-top:0">方式 A：企信内"工作台 → AI 需求助理"</h2>
                    <ol>
                      <li>打开企业微信 App</li>
                      <li>底部"工作台"标签</li>
                      <li>找到"AI 需求助理"并点击</li>
                    </ol>

                    <h2>方式 B：复制链接 → 粘到企信对话里点开</h2>
                    <div class="url-box" id="urlBox">__WXWORK_URL__</div>
                    <p>
                      <button class="btn" onclick="copyUrl()">复制链接</button>
                      <a class="btn secondary" href="javascript:void(0)" onclick="tryOpen()">尝试直接打开</a>
                      <span class="copy-feedback" id="copyFb">已复制 ✓</span>
                    </p>
                  </div>
                </details>

                <hr>
                <p style="font-size:12px;color:#888">
                  授权完成后会跳回: <code>__REDIRECT__</code>
                </p>
              </div>

              <script>
                var redirectUri = "__CALLBACK_ENC__";
                try {
                  var obj = new WxLogin({
                    id: "wxlogin",
                    appid: "__CORP_ID__",
                    agentid: "__AGENT_ID__",
                    redirect_uri: redirectUri,
                    state: "browser_helper",
                    href: "",
                    lang: "zh"
                  });
                } catch (e) {
                  console.error("WxLogin init failed, fallback to iframe", e);
                  document.getElementById('wxlogin').innerHTML =
                    '<iframe src="__IFRAME_SRC__" width="320" height="320" frameborder="0" scrolling="no" allow="camera" style="border:1px solid #e0e0e0; border-radius:4px;"></iframe>' +
                    '<div style="margin-top:8px; color:#666; font-size:12px;">扫码后会自动跳到管理端</div>';
                }

                var url = document.getElementById('urlBox') ? document.getElementById('urlBox').textContent : '';
                function copyUrl() {
                  navigator.clipboard.writeText(url).then(() => {
                    document.getElementById('copyFb').style.display = 'inline';
                    setTimeout(() => document.getElementById('copyFb').style.display = 'none', 2000);
                  }).catch(() => {
                    var r = document.createRange();
                    r.selectNode(document.getElementById('urlBox'));
                    window.getSelection().removeAllRanges();
                    window.getSelection().addRange(r);
                    document.execCommand('copy');
                    document.getElementById('copyFb').style.display = 'inline';
                    setTimeout(() => document.getElementById('copyFb').style.display = 'none', 2000);
                  });
                }
                function tryOpen() { window.location.href = url; }
              </script>
            </body>
            </html>
            """
            .replace("__CORP_ID__", esc(wecomProperties.getCorpId()))
            .replace("__AGENT_ID__", esc(wecomProperties.getAgentId()))
            .replace("__CALLBACK_ENC__", esc(callbackEnc))
            .replace("__IFRAME_SRC__", esc(iframeSrc))
            .replace("__WXWORK_URL__", esc(wxworkUrl))
            .replace("__REDIRECT__", esc(redirect));
    }

    /**
     * 重定向到企信 App（如果用户设备支持）
     */
    @GetMapping("/open-in-wxwork")
    public void openInWxwork(@RequestParam(value = "redirect", defaultValue = "/admin/") String redirect,
                             HttpServletResponse resp) throws IOException {
        String callback = redirectBase + "/admin/oauth/callback";
        String wxworkUrl = "wxwork://open.weixin.qq.com/connect/oauth2/authorize"
            + "?appid=" + wecomProperties.getCorpId()
            + "&redirect_uri=" + enc(callback)
            + "&response_type=code"
            + "&scope=snsapi_base"
            + "&state=redirect_helper"
            + "&agentid=" + wecomProperties.getAgentId()
            + "#wechat_redirect";
        resp.setStatus(302);
        resp.setHeader("Location", wxworkUrl);
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, java.nio.charset.StandardCharsets.UTF_8);
    }
    private static String esc(String s) {
        return s.replace("<", "&lt;").replace(">", "&gt;").replace("&", "&amp;");
    }
}
