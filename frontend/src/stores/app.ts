import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getSceneList, type SceneItem } from '@/api/scenes'
import {
  getConversationList,
  createConversation,
  deleteConversation,
  updateConversationTitle,
  type ConversationItem
} from '@/api/conversations'

export interface ChatHistory {
  id: string
  title: string
  createdAt: Date
  isActive: boolean
  /** 关联的场景名称 */
  sceneName: string
}

export interface RecordingState {
  isRecording: boolean
  duration: number
  audioBlob: Blob | null
}

export const useAppStore = defineStore('app', () => {
  // ========== 状态 ==========
  const chatHistories = ref<ChatHistory[]>([])
  const conversationsLoaded = ref(false)

  // 场景列表（从后端加载）
  const scenes = ref<SceneItem[]>([])
  const scenesLoaded = ref(false)

  const recordingState = ref<RecordingState>({
    isRecording: false,
    duration: 0,
    audioBlob: null
  })

  const currentScene = ref('日常对话')
  const isFavorited = ref(false)
  const aiStatus = ref<'ready' | 'recording' | 'processing' | 'speaking'>('ready')

  // 当前用户 ID（后续对接登录后替换为真实值）
  const userId = ref(1)

  // ========== 计算属性 ==========
  const activeChatId = computed(() =>
    chatHistories.value.find(h => h.isActive)?.id || ''
  )

  // ========== 方法 ==========

  /** 从后端加载场景列表 */
  async function fetchScenes() {
    try {
      const res = await getSceneList(userId.value)
      if (res.code === 200 && res.data) {
        scenes.value = res.data
        scenesLoaded.value = true
      }
    } catch (e) {
      console.error('加载场景失败:', e)
      scenesLoaded.value = true
    }
  }

  /** 从后端加载会话历史 */
  async function fetchConversations() {
    // 记住当前激活的会话ID，加载后恢复
    const prevActiveId = chatHistories.value.find(h => h.isActive)?.id || ''
    try {
      const res = await getConversationList(userId.value)
      if (res.code === 200 && res.data) {
        // 场景名查找表（sceneId → sceneName）
        const sceneMap = new Map<number, string>()
        scenes.value.forEach(s => sceneMap.set(Number(s.sceneId), s.sceneName))

        chatHistories.value = res.data.map(item => ({
          id: String(item.conversationId),
          title: item.title,
          createdAt: new Date(item.createTime),
          isActive: false,
          sceneName: item.sceneId ? (sceneMap.get(item.sceneId) || '未知场景') : '未设置'
        }))
        // 恢复之前的激活状态，若该会话已不存在则默认选第一条
        if (chatHistories.value.length > 0) {
          const target = chatHistories.value.find(h => h.id === prevActiveId)
          if (target) {
            target.isActive = true
          } else {
            chatHistories.value[0].isActive = true
          }
          // 同步 Header 的场景名
          const active = chatHistories.value.find(h => h.isActive)
          if (active) currentScene.value = active.sceneName
        }
        conversationsLoaded.value = true
      }
    } catch (e) {
      console.error('加载会话失败:', e)
      conversationsLoaded.value = true
    }
  }

  async function createNewChat(title?: string) {
    try {
      const res = await createConversation({
        userId: userId.value,
        sceneId: undefined, // 后续可关联当前场景
        title: title || undefined
      })
      if (res.code === 200 && res.data) {
        const item = res.data
        // 查找场景名
        const sceneName = item.sceneId
          ? (scenes.value.find(s => Number(s.sceneId) === item.sceneId)?.sceneName || '未知场景')
          : '未设置'
        const newHistory: ChatHistory = {
          id: String(item.conversationId),
          title: item.title,
          createdAt: new Date(item.createTime),
          isActive: true,
          sceneName
        }
        // 全部取消激活，新会话置顶
        chatHistories.value.forEach(h => (h.isActive = false))
        chatHistories.value.unshift(newHistory)
        currentScene.value = sceneName
      }
    } catch (e) {
      console.error('创建会话失败:', e)
    }
  }

  function selectChat(id: string) {
    chatHistories.value.forEach(h => {
      h.isActive = h.id === id
    })
    // 同步更新当前场景名
    const active = chatHistories.value.find(h => h.id === id)
    if (active) {
      currentScene.value = active.sceneName
    }
  }

  async function updateChatTitle(conversationId: number, title: string) {
    const res = await updateConversationTitle(conversationId, title)
    if (res.code === 200) {
      // 同步更新本地状态
      const item = chatHistories.value.find(h => h.id === String(conversationId))
      if (item) item.title = title
    }
  }

  function toggleRecording() {
    if (recordingState.value.isRecording) {
      stopRecording()
    } else {
      startRecording()
    }
  }

  function startRecording() {
    recordingState.value.isRecording = true
    recordingState.value.duration = 0
    aiStatus.value = 'recording'
  }

  function stopRecording() {
    recordingState.value.isRecording = false
    aiStatus.value = 'processing'
    
    setTimeout(() => {
      aiStatus.value = 'ready'
    }, 1500)
  }

  function setDuration(seconds: number) {
    recordingState.value.duration = seconds
  }

  async function deleteChat(id: string) {
    try {
      await deleteConversation(Number(id))
      const idx = chatHistories.value.findIndex(h => h.id === id)
      if (idx !== -1) {
        const wasActive = chatHistories.value[idx].isActive
        chatHistories.value.splice(idx, 1)
        // 如果删的是当前激活的，自动激活第一条
        if (wasActive && chatHistories.value.length > 0) {
          chatHistories.value[0].isActive = true
        }
      }
    } catch (e) {
      console.error('删除会话失败:', e)
    }
  }

  function toggleFavorite() {
    isFavorited.value = !isFavorited.value
  }

  return {
    chatHistories,
    conversationsLoaded,
    recordingState,
    currentScene,
    isFavorited,
    aiStatus,
    activeChatId,
    scenes,
    scenesLoaded,
    userId,
    fetchScenes,
    fetchConversations,
    createNewChat,
    selectChat,
    updateChatTitle,
    deleteChat,
    toggleRecording,
    startRecording,
    stopRecording,
    setDuration,
    toggleFavorite
  }
})
