import dayjs from 'dayjs';

/**
 * 格式化为 yyyy-MM-dd HH:mm:ss
 */
export function fmtTime(s: string | undefined | null): string {
  if (!s) return '';
  const d = dayjs(s);
  return d.isValid() ? d.format('YYYY-MM-DD HH:mm:ss') : s;
}

/**
 * 格式化为 yyyy-MM-dd
 */
export function fmtDate(s: string | undefined | null): string {
  if (!s) return '';
  const d = dayjs(s);
  return d.isValid() ? d.format('YYYY-MM-DD') : s;
}

/**
 * 相对时间（如"3 分钟前"）
 */
export function fmtRelative(s: string | undefined | null): string {
  if (!s) return '';
  const d = dayjs(s);
  if (!d.isValid()) return s;
  const diff = Date.now() - d.valueOf();
  const sec = Math.floor(diff / 1000);
  if (sec < 60) return '刚刚';
  const min = Math.floor(sec / 60);
  if (min < 60) return `${min} 分钟前`;
  const hr = Math.floor(min / 60);
  if (hr < 24) return `${hr} 小时前`;
  const day = Math.floor(hr / 24);
  if (day < 30) return `${day} 天前`;
  return fmtDate(s);
}
