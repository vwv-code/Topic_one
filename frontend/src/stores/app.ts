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

export interface ChatMessage {
  id: string
  role: 'user' | 'assistant'
  content: string
  timestamp: Date
}

export interface RecordingState {
  isRecording: boolean
  duration: number
  audioBlob: Blob | null
}

export type WsConnectionStatus = 'disconnected' | 'connected' | 'recording' | 'processing' | 'speaking'

export interface PronunciationWordDetail {
  word: string
  score: number
  startMs: number
  endMs: number
  phonemes: PronunciationPhonemeDetail[]
}

export interface PronunciationPhonemeDetail {
  phoneme: string
  score: number
  hasError: boolean
}

export interface PronunciationSentenceDetail {
  score: number
  stressScore: number
  toneScore: number
  senseScore: number
}

export interface PronunciationResultItem {
  refText: string
  overallScore: number
  accuracyScore: number
  fluencyScore: number
  integrityScore: number
  speed: number
  audioDuration: number
  sentenceDetail: PronunciationSentenceDetail | null
  wordDetails: PronunciationWordDetail[]
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

  // ========== 实时对话状态 ==========
  
  /** 当前会话的消息列表 */
  const messages = ref<ChatMessage[]>([])

  /** WebSocket 连接状态 */
  const wsStatus = ref<WsConnectionStatus>('disconnected')

  /** ASR 实时转写文本（录音过程中实时更新） */
  const recognitionText = ref('')

  /** AI 正在流式输出的文本（逐字追加） */
  const aiStreamingText = ref('')

  /** AI 完整回复（流式结束后确定） */
  const aiFullResponse = ref('')

  // ========== 字幕状态 ==========
  /** 是否开启字幕 */
  const subtitleEnabled = ref(false)
  /** 字幕是否当前可见（受时间控制） */
  const subtitleVisible = ref(false)
  /** 当前字幕文字内容 */
  const subtitleText = ref('')
  /** 字幕延迟/消失定时器 */
  let subtitleTimer: ReturnType<typeof setTimeout> | null = null

  // ========== 发音评测状态 ==========
  /** 评测结果列表（每句一个） */
  const pronunciationResults = ref<PronunciationResultItem[]>([])
  /** 是否正在评测中 */
  const pronunciationEvaluating = ref(false)
  /** 评测面板是否展开 */
  const pronunciationPanelVisible = ref(false)

  /** WebSocket 实例 */
  let ws: WebSocket | null = null

  /** 音频上下文（用于 PCM 录音和播放） */
  let audioContext: AudioContext | null = null
  let mediaStream: MediaStream | null = null
  let scriptProcessor: ScriptProcessorNode | null = null
  let sourceNode: MediaStreamAudioSourceNode | null = null

  /** TTS 音频播放队列 */
  let ttsAudioQueue: ArrayBuffer[] = []
  let isPlayingTts = false

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
    // 先关闭当前正在进行的语音会话（防止旧会话的 WebSocket 残留数据）
    forceCloseAll()

    chatHistories.value.forEach(h => {
      h.isActive = h.id === id
    })
    // 同步更新当前场景名
    const active = chatHistories.value.find(h => h.id === id)
    if (active) {
      currentScene.value = active.sceneName
    }

    // ★ 清空消息面板，避免显示上一个会话的内容
    messages.value = []
    recognitionText.value = ''
    aiStreamingText.value = ''
    aiFullResponse.value = ''
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
      stopVoiceSession()
    } else {
      startVoiceSession()
    }
  }

  function startRecording() {
    recordingState.value.isRecording = true
    recordingState.value.duration = 0
    aiStatus.value = 'recording'
    console.log('[Voice] 录音标志已设置 isRecording=true, 等待 PCM 数据...')
  }

  function stopRecording() {
    recordingState.value.isRecording = false
    aiStatus.value = 'processing'
    console.log('[Voice] 录音已停止')
  }

  // ========== WebSocket 实时对话方法 ==========

  /**
   * 开始语音对话：建立 WebSocket 连接 + 开始 PCM 录音
   */
  async function startVoiceSession() {
    const convId = activeChatId.value
    if (!convId) {
      console.warn('[Voice] 没有激活的会话，无法开始对话')
      return
    }

    console.log(`[Voice] 开始语音对话, conversationId=${convId}`)

    // 清除上一次的评测结果
    pronunciationResults.value = []
    pronunciationEvaluating.value = true
    pronunciationPanelVisible.value = false

    try {
      // 1. 建立 WebSocket 连接
      const wsUrl = `ws://localhost:8080/voice?conversationId=${convId}`
      ws = new WebSocket(wsUrl)

      ws.onopen = () => {
        console.log('[Voice] WebSocket 已连接, 发送 start 指令')
        wsStatus.value = 'connected' as WsConnectionStatus
        ws?.send(JSON.stringify({ type: 'start', conversationId: Number(convId) }))
      }

      ws.onmessage = (event) => {
        const msg = JSON.parse(event.data)
        console.log('[Voice] 收到消息:', msg.type, msg.data)
        handleWsMessage(msg)
      }

      ws.onerror = (error) => {
        console.error('WebSocket 错误', error)
        wsStatus.value = 'disconnected' as WsConnectionStatus
      }

      ws.onclose = () => {
        console.log('WebSocket 已关闭')
        wsStatus.value = 'disconnected' as WsConnectionStatus
        stopPcmRecording()
      }

      // 2. 先设置录音标志，确保 onaudioprocess 触发时就能发送数据
      startRecording()

      // 3. 初始化 PCM 录音（AudioContext + 麦克风 + ScriptProcessorNode）
      console.log('[Voice] 正在初始化 PCM 录音...')
      await startPcmRecording()
      console.log('[Voice] 语音对话已完全启动')

      wsStatus.value = 'recording' as WsConnectionStatus

    } catch (error) {
      console.error('启动语音对话失败', error)
      // 回滚：初始化失败时重置录音状态
      recordingState.value.isRecording = false
      aiStatus.value = 'ready'
      wsStatus.value = 'disconnected' as WsConnectionStatus
      stopPcmRecording()
      if (ws) { ws.close(); ws = null }
    }
  }

  /**
   * 结束语音对话：停止录音 + 发送停止指令 + 关闭连接
   */
  function stopVoiceSession() {
    // 发送停止指令（触发后端 ASR → LLM → TTS 流水线）
    if (ws && ws.readyState === WebSocket.OPEN) {
      ws.send(JSON.stringify({ type: 'stop' }))
      // 不立即关闭连接，等待后端返回 AI 回复和音频数据
      // 连接在收到 audio_complete 后由服务端关闭，或超时关闭
    }

    // 停止本地 PCM 录音
    stopPcmRecording()
    stopRecording()
    wsStatus.value = 'processing' as WsConnectionStatus
    // 停止时立即隐藏字幕
    hideSubtitle()
  }

  /**
   * 强制关闭所有连接（用户再次点击麦克风时调用）
   */
  function forceCloseAll() {
    stopPcmRecording()
    if (ws) {
      ws.close()
      ws = null
    }
    stopTtsPlayback()
    recordingState.value.isRecording = false
    wsStatus.value = 'disconnected' as WsConnectionStatus
    aiStatus.value = 'ready'
    recognitionText.value = ''
    aiStreamingText.value = ''
    // 清除字幕状态
    hideSubtitle()
    // 清除发音评测状态
    pronunciationResults.value = []
    pronunciationEvaluating.value = false
    pronunciationPanelVisible.value = false
  }

  /**
   * 处理 WebSocket 服务端消息（自动循环模式）
   *
   * 状态流转：
   *   connected → recording(ASR识别中) → processing(LLM思考) → speaking(TTS播放) → recording(回到录音)
   *
   * 关键变化：
   * - 不再在 stop 时关闭连接，而是保持长连接受后端驱动状态切换
   * - 收到 status=recording 后自动恢复录音发送
   */
  function handleWsMessage(msg: { type: string; data: unknown }) {
    switch (msg.type) {
      case 'status':
        handleStatusChange(msg.data as string)
        break

      case 'recognition_text':
        // ASR 实时中间结果
        recognitionText.value = msg.data as string
        break

      case 'recognition_final':
        // ASR 最终识别结果 → 添加到消息列表
        {
          const text = msg.data as string
          recognitionText.value = ''
          messages.value.push({
            id: `user-${Date.now()}`,
            role: 'user',
            content: text,
            timestamp: new Date()
          })
        }
        break

      case 'ai_response_text':
        // AI 流式回复片段
        aiStreamingText.value += msg.data as string
        break

      case 'ai_response_complete':
        // AI 回复完成 → 添加到消息列表 + 显示字幕（语音播放前）
        {
          const fullText = msg.data as string
          aiFullResponse.value = fullText
          messages.value.push({
            id: `assistant-${Date.now()}`,
            role: 'assistant',
            content: fullText,
            timestamp: new Date()
          })
          aiStreamingText.value = ''
          // 字幕文字就绪，开启字幕且状态活跃时才显示（防止停止后残留消息触发）
          subtitleText.value = fullText
          if (subtitleEnabled.value && aiStatus.value !== 'ready') {
            subtitleVisible.value = true
          }
        }
        break

      case 'audio_chunk':
        // TTS 音频分片 → 加入播放队列
        enqueueTtsAudio(msg.data as string)
        break

      case 'audio_complete':
        // TTS 播放完毕 → 等待后端发 status=recording 恢复录音
        // 不主动改状态，由后端 status 消息驱动
        console.log('[Voice] TTS 音频播放完毕')
        break

      case 'error':
        console.error('服务端错误:', msg.data)
        break

      case 'pronunciation_result':
        // 收到一条发音评测结果
        pronunciationResults.value.push(msg.data as PronunciationResultItem)
        break

      case 'pronunciation_complete':
        // 全部评测完成
        pronunciationEvaluating.value = false
        pronunciationPanelVisible.value = true
        console.log('[Voice] 发音评测完成, 共', pronunciationResults.value.length, '条结果')
        break

      default:
        console.log('未知消息类型:', msg.type)
    }
  }

  /** 处理状态变更（后端驱动的状态机） */
  function handleStatusChange(status: string) {
    console.log(`[Voice] 状态变更: ${wsStatus.value} → ${status}`)
    wsStatus.value = status as WsConnectionStatus

    switch (status) {
      case 'connected':
        // 连接已建立，等待 start 响应
        break

      case 'recording':
        // 录音中 / 从 TTS 恢复回录音
        aiStatus.value = 'recording'
        // 确保录音标志为 true（从 speaking 回来时需要恢复）
        if (!recordingState.value.isRecording && ws?.readyState === WebSocket.OPEN) {
          recordingState.value.isRecording = true
          console.log('[Voice] 恢复录音状态（TTS 播放结束）')
        }
        break

      case 'processing':
        // LLM 思考中，暂停录音输入
        aiStatus.value = 'processing'
        break

      case 'speaking':
        // TTS 播放中，暂停录音输入
        aiStatus.value = 'speaking'
        break

      case 'ready':
        // 会话完全结束（用户点了停止）
        aiStatus.value = 'ready'
        break
    }
  }

  // ========== PCM 录音（Web Audio API）==========

  /**
   * 使用 Web Audio API 进行 PCM 录音（16kHz, 16bit, mono）
   *
   * 关键要点：
   *   1. ws.send(ArrayBuffer) 自动以 binary 帧发送，无需额外设置 ✅
   *   2. getUserMedia 的 sampleRate 约束浏览器可能忽略，需用 AudioContext 重采样 ✅
   *   3. ScriptProcessorNode 已废弃但仍可用，必须正确连接到 audio graph 才会触发 ✅
   */
  async function startPcmRecording(): Promise<void> {
    try {
      // ---- 步骤 1：请求麦克风权限 ----
      console.log('[Voice] 正在请求麦克风权限...')
      mediaStream = await navigator.mediaDevices.getUserMedia({
        audio: {
          channelCount: 1,
          echoCancellation: true,
          noiseSuppression: true,
          // 注意：sampleRate 在 getUserMedia 中是 hint，浏览器不一定遵守
          // 实际重采样由 AudioContext({sampleRate: 16000}) 完成
        }
      })

      const track = mediaStream.getAudioTracks()[0]
      const settings = track.getSettings()
      console.log(`[Voice] 麦克风已获取, 设备sampleRate=${settings.sampleRate}, channels=${settings.channelCount || 'default'}`)

      if (!track.enabled || mediaStream.getAudioTracks().length === 0) {
        throw new Error('麦克风轨道不可用')
      }

      // ---- 步骤 2：创建 AudioContext 并确保运行 ----
      // 注意：{sampleRate: 16000} 是建议值，浏览器可能忽略（Chrome 通常返回 48000）
      // 我们会在数据层做手动重采样，不依赖 AudioContext 的采样率
      audioContext = new AudioContext()
      const actualSampleRate = audioContext.sampleRate
      console.log(`[Voice] AudioContext 创建成功, 实际采样率=${actualSampleRate}Hz`)

      if (audioContext.state === 'suspended') {
        await audioContext.resume()
        console.log('[Voice] AudioContext 已从 suspended 恢复')
      }

      if (audioContext.state !== 'running') {
        throw new Error('AudioContext 无法启动, state=' + audioContext.state)
      }

      // ---- 步骤 3：构建音频处理图 ----
      sourceNode = audioContext.createMediaStreamSource(mediaStream)

      const bufferSize = 4096 // 4096 samples @16kHz ≈ 256ms per frame
      scriptProcessor = audioContext.createScriptProcessor(bufferSize, 1, 1)

      let frameCount = 0
      let lastLogTime = Date.now()

      scriptProcessor.onaudioprocess = (e) => {
        frameCount++
        const now = Date.now()

        // 每 ~2s 或前 3 帧打印诊断日志
        if (frameCount <= 3 || now - lastLogTime > 2000) {
          const wsState = ws?.readyState
          const wsStateName = wsState === 0 ? 'CONNECTING' : wsState === 1 ? 'OPEN' : wsState === 2 ? 'CLOSING' : wsState === 3 ? 'CLOSED' : String(wsState)
          console.log(
            `[Voice] audioProcess #${frameCount} | ` +
            `recording=${recordingState.value.isRecording} | ` +
            `ws=${wsStateName} | ` +
            `samples=${e.inputBuffer.length} | ` +
            `ctxState=${audioContext?.state}`
          )
          lastLogTime = now
        }

        // 条件守卫
        if (!recordingState.value.isRecording) return
        if (!ws || ws.readyState !== WebSocket.OPEN) return

        // ---- 数据转换：Float32 → Int16 PCM + 强制 16kHz 重采样 ----
        const inputData = e.inputBuffer.getChannelData(0)
        const pcmData = resampleAndConvertToPcm(inputData, actualSampleRate)

        try {
          ws.send(pcmData)
          // 发送成功日志（每 20 帧打印一次）
          if (frameCount % 20 === 0) {
            console.log(`[Voice] ✓ 已发送 ${frameCount} 帧 | 源=${actualSampleRate}Hz → 目标=16kHz | ${pcmData.byteLength} bytes`)
          }
        } catch (err) {
          console.error('[Voice] ws.send 失败', err)
        }
      }

      // 连接音频图：source → processor → destination
      // ⚠️ 必须连接到 destination 或其他输出节点，否则 onaudioprocess 不会触发！
      sourceNode.connect(scriptProcessor)
      scriptProcessor.connect(audioContext.destination)

      console.log(`[Voice] 录音引擎就绪 | ctx=${audioContext.state} | buffer=${bufferSize} samples | 源采样率=${actualSampleRate}Hz → 输出=16kHz/16bit/mono/PCM`)
    } catch (error) {
      // 区分不同错误类型
      if (error instanceof DOMException && error.name === 'NotAllowedError') {
        console.error('[Voice] 麦克风权限被拒绝，请在浏览器设置中允许麦克风访问')
      } else if (error instanceof DOMException && error.name === 'NotFoundError') {
        console.error('[Voice] 未找到麦克风设备')
      } else {
        console.error('[Voice] 录音初始化失败', error)
      }
      throw error
    }
  }

  /** 停止 PCM 录音 */
  function stopPcmRecording() {
    if (scriptProcessor) {
      scriptProcessor.disconnect()
      scriptProcessor = null
    }
    if (sourceNode) {
      sourceNode.disconnect()
      sourceNode = null
    }
    if (mediaStream) {
      mediaStream.getTracks().forEach(track => track.stop())
      mediaStream = null
    }
    if (audioContext && audioContext.state !== 'closed') {
      audioContext.close().catch(() => {})
      audioContext = null
    }
    console.log('PCM 录音已停止')
  }

  /**
   * 重采样 + 格式转换：Float32(任意采样率) → Int16 PCM(16kHz/16bit/mono/Little-Endian)
   *
   * 阿里云 ASR 要求的严格格式：
   *   - 采样率：16000 Hz（必须精确）
   *   - 位深：16 bit
   *   - 声道：单声道（mono）
   *   - 格式：PCM 裸流（无 WAV 头）
   *   - 字节序：Little-Endian（JS Int16Array 默认）
   */
  function resampleAndConvertToPcm(float32Array: Float32Array, sourceSampleRate: number): ArrayBuffer {
    const TARGET_SAMPLE_RATE = 16000

    // 如果已经是 16kHz，直接转换
    if (sourceSampleRate === TARGET_SAMPLE_RATE) {
      return float32ToInt16Buffer(float32Array)
    }

    // 线性插值重采样到 16kHz
    const ratio = sourceSampleRate / TARGET_SAMPLE_RATE
    const outputLength = Math.round(float32Array.length / ratio)
    const resampled = new Float32Array(outputLength)

    for (let i = 0; i < outputLength; i++) {
      const srcIndex = i * ratio
      const srcIndex0 = Math.floor(srcIndex)
      const srcIndex1 = Math.min(srcIndex0 + 1, float32Array.length - 1)
      const fraction = srcIndex - srcIndex0

      // 线性插值
      resampled[i] = float32Array[srcIndex0] * (1 - fraction) + float32Array[srcIndex1] * fraction
    }

    return float32ToInt16Buffer(resampled)
  }

  /** Float32 → Int16 PCM ArrayBuffer (16bit, Little-Endian) */
  function float32ToInt16Buffer(float32Array: Float32Array): ArrayBuffer {
    const int16Array = new Int16Array(float32Array.length)
    for (let i = 0; i < float32Array.length; i++) {
      const s = Math.max(-1, Math.min(1, float32Array[i]))
      int16Array[i] = s < 0 ? s * 0x8000 : s * 0x7FFF
    }
    return int16Array.buffer
  }

  // ========== TTS 音频播放 ==========

  /** 复用的 AudioContext（避免每片创建/销毁造成的断档和杂音） */
  let ttsAudioContext: AudioContext | null = null
  /** 下一次 source.start(t) 的时间（秒），在同一个 ctx.currentTime 时间线上调度 */
  let ttsNextStartTime = 0
  /** 已调度的 source 节点集合（用于生命周期追踪） */
  const scheduledSources = new Set<AudioBufferSourceNode>()

  /** 将 base64 音频数据加入播放队列，播放中主动触发调度填补时间线 */
  function enqueueTtsAudio(base64Data: string) {
    const binaryString = atob(base64Data)
    const bytes = new Uint8Array(binaryString.length)
    for (let i = 0; i < binaryString.length; i++) {
      bytes[i] = binaryString.charCodeAt(i)
    }
    ttsAudioQueue.push(bytes.buffer.slice(0)) // slice 避免 detached buffer

    if (!isPlayingTts) {
      startTtsPlayback()
    } else {
      // ★ 播放中收到新数据，立即尝试预调度（不再只依赖 onended）
      scheduleNextChunk()
    }
  }

  /** 启动 TTS 播放引擎（只创建一次 AudioContext，所有分片在同一时间线上调度） */
  function startTtsPlayback() {
    if (isPlayingTts) return
    isPlayingTts = true

    // 复用或创建 AudioContext
    if (!ttsAudioContext || ttsAudioContext.state === 'closed') {
      ttsAudioContext = new AudioContext({ sampleRate: 16000 })
    }
    if (ttsAudioContext.state === 'suspended') {
      ttsAudioContext.resume()
    }

    ttsNextStartTime = ttsAudioContext.currentTime + 0.05 // 50ms 缓冲，留够首次解码时间
    scheduleNextChunk()
  }

  /**
   * 调度分片到 AudioContext 时间线
   *
   * 关键优化：
   *   1. 时间漂移保护：调度前校准 ttsNextStartTime，防止落后于实际时间
   *   2. 预缓冲：积攒 2 个分片才开始播放，消除网络抖动的首片卡顿
   *   3. 主动调度：enqueueTtsAudio 和 onended 双重触发，不依赖单一事件
   */
  function scheduleNextChunk() {
    const ctx = ttsAudioContext
    if (!ctx || ctx.state === 'closed') return

    if (ttsAudioQueue.length === 0) {
      // 没有数据了，等后续 enqueue 触发或超时结束
      return
    }

    // ★ 时间漂移保护：如果调度线落后了，从当前时间重新开始
    if (ttsNextStartTime < ctx.currentTime) {
      ttsNextStartTime = ctx.currentTime + 0.005 // 对齐到当前时刻
    }

    const TTS_SAMPLE_RATE = ctx.sampleRate
    const SCHEDULE_AHEAD_SECONDS = 0.5 // 最多预调度 500ms 后的分片

    // ★ 批量调度：一次性把 500ms 窗口内的分片全部排好
    let scheduledCount = 0
    while (ttsAudioQueue.length > 0 && ttsNextStartTime < ctx.currentTime + SCHEDULE_AHEAD_SECONDS) {
      const buffer = ttsAudioQueue.shift()!

      try {
        const int16View = new Int16Array(buffer)
        const numFrames = int16View.length
        if (numFrames === 0) continue

        const duration = numFrames / TTS_SAMPLE_RATE

        const audioBuffer = ctx.createBuffer(1, numFrames, TTS_SAMPLE_RATE)
        const channelData = audioBuffer.getChannelData(0)

        for (let i = 0; i < numFrames; i++) {
          const s = int16View[i]
          channelData[i] = s < 0 ? s / 32768.0 : s / 32767.0
        }

        const source = ctx.createBufferSource()
        source.buffer = audioBuffer
        source.connect(ctx.destination)

        const startTime = ttsNextStartTime
        ttsNextStartTime += duration

        scheduledSources.add(source)
        source.onended = () => {
          scheduledSources.delete(source)
          // 尝试从队列取更多分片调度
          scheduleNextChunk()
        }
        source.start(startTime)
        scheduledCount++
      } catch (e) {
        console.error('TTS 音频调度失败', e)
      }
    }

    if (scheduledCount > 0) {
      console.log(`[TTS] 批量调度: ${scheduledCount} 片, 时间线截止 @ ${ttsNextStartTime.toFixed(3)}s`)
    }

    // 如果队列空了且所有 source 都播完了，结束播放
    if (ttsAudioQueue.length === 0 && scheduledSources.size === 0) {
      finishTtsPlayback()
    }
  }

  /** 结束 TTS 播放 */
  function finishTtsPlayback() {
    isPlayingTts = false
    ttsNextStartTime = 0
    scheduledSources.clear()
    // 字幕在语音结束后0.3s消失
    hideSubtitle(300)
    console.log('[TTS] 播放引擎空闲')
  }

  /** 停止 TTS 播放（立即终止） */
  function stopTtsPlayback() {
    ttsAudioQueue = []
    // 停止所有已调度的 source（避免残留声音）
    scheduledSources.forEach(source => {
      try { source.stop() } catch (_) { /* 已停止的忽略 */ }
    })
    scheduledSources.clear()
    isPlayingTts = false
    ttsNextStartTime = 0
    // 强制停止时立即隐藏字幕
    hideSubtitle()
    if (ttsAudioContext && ttsAudioContext.state !== 'closed') {
      ttsAudioContext.close().catch(() => {})
      ttsAudioContext = null
    }
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

  function toggleSubtitle() {
    subtitleEnabled.value = !subtitleEnabled.value
    if (subtitleEnabled.value) {
      // 开启时：如果状态活跃且有字幕文字，立即显示
      if (aiStatus.value !== 'ready' && subtitleText.value) {
        subtitleVisible.value = true
      }
    } else {
      // 关闭时立即隐藏
      hideSubtitle()
    }
  }

  /** 显示字幕（语音播放前0.3s调用） */
  function showSubtitle(text: string, delayMs: number = 0) {
    if (!subtitleEnabled.value) return
    clearSubtitleTimer()
    subtitleText.value = text
    if (delayMs > 0) {
      subtitleTimer = setTimeout(() => {
        subtitleVisible.value = true
      }, delayMs)
    } else {
      subtitleVisible.value = true
    }
  }

  /** 隐藏字幕（语音结束后0.3s调用） */
  function hideSubtitle(delayMs: number = 0) {
    clearSubtitleTimer()
    if (delayMs > 0) {
      subtitleTimer = setTimeout(() => {
        subtitleVisible.value = false
        subtitleText.value = ''
      }, delayMs)
    } else {
      subtitleVisible.value = false
      subtitleText.value = ''
    }
  }

  function clearSubtitleTimer() {
    if (subtitleTimer) {
      clearTimeout(subtitleTimer)
      subtitleTimer = null
    }
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
    // 实时对话状态
    messages,
    wsStatus,
    recognitionText,
    aiStreamingText,
    aiFullResponse,
    // 字幕状态
    subtitleEnabled,
    subtitleVisible,
    subtitleText,
    // 发音评测状态
    pronunciationResults,
    pronunciationEvaluating,
    pronunciationPanelVisible,
    // 场景和会话
    fetchScenes,
    fetchConversations,
    createNewChat,
    selectChat,
    updateChatTitle,
    deleteChat,
    toggleFavorite,
    toggleSubtitle,
    // 录音（兼容旧调用）
    toggleRecording,
    startRecording,
    stopRecording,
    setDuration,
    // WebSocket 实时对话
    startVoiceSession,
    stopVoiceSession,
    forceCloseAll
  }
})
