import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getSceneList, type SceneItem } from '@/api/scenes'
import {
  getConversationList,
  createConversation,
  deleteConversation,
  type ConversationItem
} from '@/api/conversations'

export interface ChatHistory {
  id: string
  title: string
  createdAt: Date
  isActive: boolean
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
    try {
      const res = await getConversationList(userId.value)
      if (res.code === 200 && res.data) {
        chatHistories.value = res.data.map(item => ({
          id: String(item.conversationId),
          title: item.title,
          createdAt: new Date(item.createTime),
          isActive: false
        }))
        // 默认选中第一条（最新）
        if (chatHistories.value.length > 0) {
          chatHistories.value[0].isActive = true
        }
        conversationsLoaded.value = true
      }
    } catch (e) {
      console.error('加载会话失败:', e)
      conversationsLoaded.value = true
    }
  }

  async function createNewChat() {
    try {
      const res = await createConversation({
        userId: userId.value,
        sceneId: undefined // 后续可关联当前场景
      })
      if (res.code === 200 && res.data) {
        const item = res.data
        const newHistory: ChatHistory = {
          id: String(item.conversationId),
          title: item.title,
          createdAt: new Date(item.createTime),
          isActive: true
        }
        // 全部取消激活，新会话置顶
        chatHistories.value.forEach(h => (h.isActive = false))
        chatHistories.value.unshift(newHistory)
      }
    } catch (e) {
      console.error('创建会话失败:', e)
    }
  }

  function selectChat(id: string) {
    chatHistories.value.forEach(h => {
      h.isActive = h.id === id
    })
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
    deleteChat,
    toggleRecording,
    startRecording,
    stopRecording,
    setDuration,
    toggleFavorite
  }
})
