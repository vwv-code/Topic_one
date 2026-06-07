import request from '@/api/request'

export interface BackgroundResponse {
  hasImage: boolean
  imageUrl: string | null
  generating: boolean
}

/**
 * 获取或生成会话背景图
 */
export function getBackground(conversationId: number) {
  return request.get<any, { code: number; data: BackgroundResponse }>('/api/background', {
    params: { conversationId },
    timeout: 120000 // 文生图可能耗时 30-60s
  })
}
