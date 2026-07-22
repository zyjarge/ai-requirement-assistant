// HTML escape
export function esc(s: unknown): string {
  if (s == null) return '';
  return String(s).replace(/[<>&"]/g, (c) => ({ '<': '&lt;', '>': '&gt;', '&': '&amp;', '"': '&quot;' }[c] || c));
}

// 截断字符串
export function truncate(s: string, max = 50): string {
  if (!s) return '';
  return s.length > max ? s.slice(0, max) + '...' : s;
}
