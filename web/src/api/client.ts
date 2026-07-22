import axios, { type AxiosInstance, type InternalAxiosRequestConfig } from 'axios';
import { message } from 'antd';

const client: AxiosInstance = axios.create({
  baseURL: '',
  timeout: 30000,
  withCredentials: true,  // 关键：自动带 cookie
  headers: {
    'Content-Type': 'application/json',
  },
});

// 请求拦截器
client.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    return config;
  },
  (error) => Promise.reject(error)
);

// 响应拦截器
client.interceptors.response.use(
  (response) => {
    // 业务错误处理
    if (response.data && response.data.error) {
      return Promise.reject(new Error(response.data.error));
    }
    return response;
  },
  (error) => {
    if (error.response) {
      const status = error.response.status;
      if (status === 401) {
        // 未登录，跳转扫码页
        const redirect = encodeURIComponent(window.location.pathname + window.location.search);
        window.location.href = `/admin/oauth/browser-helper?redirect=${redirect}`;
        return Promise.reject(new Error('未授权'));
      }
      if (status === 403) {
        message.error('没有权限');
      } else if (status >= 500) {
        message.error('服务器错误');
      } else {
        const msg = error.response.data?.message || error.response.data?.error || '请求失败';
        message.error(msg);
      }
    } else if (error.request) {
      message.error('网络错误，请检查连接');
    } else {
      message.error(error.message);
    }
    return Promise.reject(error);
  }
);

export default client;
