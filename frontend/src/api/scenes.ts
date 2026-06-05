import request from './request'

/** 场景数据结构（与后端 SceneDTO 对齐） */
export interface SceneItem {
  sceneId: number | string
  sceneName: string
  description: string
  roleSetting: string
  difficulty: number
  vocabulary: string
  sentences: string
  isBuiltin: boolean
  icon: string
  sortOrder: number
}

/** 创建自定义场景请求 */
export interface CreateScenePayload {
  userId: number
  sceneName: string
  description: string
  roleSetting?: string
  difficulty?: number
}

/** 用户设置数据（与后端 UserSettingsDTO 对齐） */
export interface UserSettings {
  currentSceneId: number | null
  difficulty: string
  speechSpeed: number
}

/** 保存用户设置请求 */
export interface SaveSettingsPayload {
  userId: number
  currentSceneId: number | null
  difficulty: string
  speechSpeed: number
  /** 当前场景ID（同步更新场景描述/角色设定） */
  sceneId: number | null
  /** 场景描述 */
  description: string
  /** 角色设定 */
  roleSetting: string
  /** 当前激活的会话ID（有值时写入会话级配置表，否则更新场景默认模板） */
  conversationId?: number | null
}

/**
 * 获取用户全部场景列表
 */
export function getSceneList(userId: number) {
  return request.get<any, { code: number; data: SceneItem[] }>('/api/scenes', {
    params: { userId }
  })
}

/**
 * 创建自定义场景
 */
export function createScene(data: CreateScenePayload) {
  return request.post<any, { code: number; data: SceneItem }>('/api/scenes', data)
}

/**
 * 获取用户设置（场景、难度、语速）
 */
export function getSettings(userId: number) {
  return request.get<any, { code: number; data: UserSettings }>('/api/settings', {
    params: { userId }
  })
}

/**
 * 保存用户设置
 */
export function saveSettings(data: SaveSettingsPayload) {
  return request.post<any, { code: number }>('/api/settings', data)
}

/**
 * 删除自定义场景
 */
export function deleteScene(sceneId: number) {
  return request.delete<any, { code: number }>('/api/scenes', {
    params: { sceneId }
  })
}
