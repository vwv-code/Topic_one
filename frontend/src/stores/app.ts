import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getSceneList, type SceneItem } from '@/api/scenes'

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
  const chatHistories = ref<ChatHistory[]>([
    { id: '1', title: '日常问候对话练习', createdAt: new Date(), isActive: true },
    { id: '2', title: '餐厅点餐场景模拟', createdAt: new Date(), isActive: false },
    { id: '3', title: '商务会议英语交流', createdAt: new Date(), isActive: false },
    { id: '4', title: '旅游问路场景练习', createdAt: new Date(), isActive: false },
    { id: '5', title: '面试自我介绍训练', createdAt: new Date(), isActive: false },
    { id: '6', title: '购物场景对话', createdAt: new Date(), isActive: false }
  ])

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

  function createNewChat() {
    const newId = Date.now().toString()
    chatHistories.value.forEach(h => (h.isActive = false))
    chatHistories.value.unshift({
      id: newId,
      title: `新对话 ${new Date().toLocaleDateString()}`,
      createdAt: new Date(),
      isActive: true
    })
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

  function toggleFavorite() {
    isFavorited.value = !isFavorited.value
  }

  return {
    chatHistories,
    recordingState,
    currentScene,
    isFavorited,
    aiStatus,
    activeChatId,
    scenes,
    scenesLoaded,
    userId,
    fetchScenes,
    createNewChat,
    selectChat,
    toggleRecording,
    startRecording,
    stopRecording,
    setDuration,
    toggleFavorite
  }
})
