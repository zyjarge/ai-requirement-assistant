import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'path';

export default defineConfig({
  base: '/admin/app/',
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  server: {
    port: 5173,
    host: '0.0.0.0',
    proxy: {
      '/admin/api': {
        target: 'http://localhost:9080',
        changeOrigin: true,
      },
      '/wecom': {
        target: 'http://localhost:9080',
        changeOrigin: true,
      },
      '/admin/oauth': {
        target: 'http://localhost:9080',
        changeOrigin: true,
      },
    },
  },
  build: {
    outDir: 'dist',
    sourcemap: false,
  },
  plugins: [
    react(),
    {
      // 针对企信内置浏览器（X5/TBS）兼容：
      // 1) 去除 <script type="module">，因为企信内核对 ESM 支持不完整
      // 2) 加 defer 属性，脚本下载并行但延后到 HTML 解析完成后执行
      //    避免 getElementById('root') 返回 null 导致 React 挂载失败
      name: 'wechat-compat',
      transformIndexHtml: {
        order: 'post',
        handler(html) {
          // 将 <script type="module" crossorigin src=...> 替换为 <script defer crossorigin src=...>
          return html.replace(/<script type="module" crossorigin /g, '<script defer crossorigin ');
        },
      },
    },
  ],
});
