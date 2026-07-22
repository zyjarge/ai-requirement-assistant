// 企微用户
export interface WeComUser {
  userid: string;
  name: string;
  department?: string[];
}

// 会话信息
export interface SessionInfo {
  userId: string;
  displayName: string;
}
