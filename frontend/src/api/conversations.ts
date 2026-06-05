import request from '@/api/request'

/** 会话数据（与后端 ConversationDTO 对齐） */
export interface ConversationItem {
  conversationId: number
  userId: number
  sceneId: number | null
  title: string
  createTime: string
}

/**
 * 获取会话列表
 */
export function getConversationList(userId: number) {
  return request.get<any, { code: number; data: ConversationItem[] }>('/api/conversations', {
    params: { userId }
  })
}

/**
 * 创建新会话
 */
export function createConversation(data: { userId: number; sceneId?: number; title?: string }) {
  return request.post<any, { code: number; data: ConversationItem }>('/api/conversations', data)
}

/**
 * 删除会话
 */
export function deleteConversation(id: number) {
  return request.delete<any, { code: number }>('/api/conversations', {
    params: { id }
  })
}
