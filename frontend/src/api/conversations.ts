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

/** 会话场景配置（与后端 ConversationSceneConfigDTO 对齐） */
export interface ConversationConfig {
  conversationId: number
  sceneId: number | null
  description: string
  roleSetting: string
}

/**
 * 获取会话的场景配置（场景ID + 描述 + 角色设定）
 */
export function getConversationConfig(conversationId: number) {
  return request.get<any, { code: number; data: ConversationConfig | null }>('/api/conversations/config', {
    params: { conversationId }
  })
}

/**
 * 删除会话
 */
export function deleteConversation(id: number) {
  return request.delete<any, { code: number }>('/api/conversations', {
    params: { id }
  })
}

/**
 * 更新会话标题
 */
export function updateConversationTitle(conversationId: number, title: string) {
  return request.put<any, { code: number }>('/api/conversations/title', null, {
    params: { conversationId, title }
  })
}
