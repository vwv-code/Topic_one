<template>
  <footer class="voice-input-bar">
    <div class="mic-container">
      <!-- 外圈光环（录音时显示） -->
      <div v-if="store.recordingState.isRecording" class="outer-ring"></div>

      <!-- 麦克风按钮 -->
      <button
        :class="['mic-btn', { recording: store.recordingState.isRecording }]"
        @click="handleToggleRecording"
      >
        <!-- 波形动画 -->
        <div v-if="store.recordingState.isRecording" class="waveform">
          <span
            v-for="(bar, index) in waveBars"
            :key="index"
            class="wave-bar"
            :style="{ animationDelay: `${index * 0.08}s`, height: waveHeights[index] + 'px' }"
          ></span>
        </div>

        <!-- 默认图标：麦克风 -->
        <svg v-if="!store.recordingState.isRecording" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M12 2a3 3 0 0 0-3 3v7a3 3 0 0 0 6 0V5a3 3 0 0 0-3-3Z"/>
          <path d="M19 10v2a7 7 0 0 1-14 0v-2"/>
          <line x1="12" x2="12" y1="19" y2="22"/>
        </svg>
        
        <!-- 录音中图标：内嵌停止标记 -->
        <svg v-else width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M12 2a3 3 0 0 0-3 3v7a3 3 0 0 0 6 0V5a3 3 0 0 0-3-3Z"/>
          <path d="M19 10v2a7 7 0 0 1-14 0v-2"/>
          <line x1="12" x2="12" y1="19" y2="22"/>
          <!-- 停止覆盖层 -->
          <rect x="9" y="9" width="6" height="6" rx="1" fill="currentColor" opacity="0.9"/>
        </svg>
      </button>

      <!-- 提示文字 -->
      <p :class="['hint-text', { active: store.recordingState.isRecording }]">
        {{ hintText }}
      </p>
    </div>
  </footer>
</template>

<script setup lang="ts">
import { computed, ref, onUnmounted } from 'vue'
import { useAppStore } from '@/stores/app'

const store = useAppStore()

// 录音相关
let mediaRecorder: MediaRecorder | null = null
let audioChunks: Blob[] = []
let timerInterval: ReturnType<typeof setInterval> | null = null

// 波形数据
const waveBars = ref(Array.from({ length: 5 }, () => ({})))
const waveHeights = [14, 22, 18, 26, 16]

// 计算属性
const formattedDuration = computed(() => {
  const seconds = store.recordingState.duration
  const mins = Math.floor(seconds / 60).toString().padStart(2, '0')
  const secs = (seconds % 60).toString().padStart(2, '0')
  return `${mins}:${secs}`
})

const hintText = computed(() => {
  if (store.recordingState.isRecording) {
    return '点击停止'
  }
  return '点击开始对话'
})

// 方法
async function handleToggleRecording() {
  if (store.recordingState.isRecording) {
    stopRecording()
  } else {
    await startRecording()
  }
}

async function startRecording() {
  try {
    // 请求麦克风权限
    const stream = await navigator.mediaDevices.getUserMedia({ audio: true })

    // 初始化 MediaRecorder
    mediaRecorder = new MediaRecorder(stream)
    audioChunks = []

    mediaRecorder.ondataavailable = (event) => {
      if (event.data.size > 0) {
        audioChunks.push(event.data)
      }
    }

    mediaRecorder.onstop = () => {
      const audioBlob = new Blob(audioChunks, { type: 'audio/webm' })
      store.recordingState.audioBlob = audioBlob

      // 停止所有音轨
      stream.getTracks().forEach((track) => track.stop())

      console.log('录音完成，音频大小:', audioBlob.size)
    }

    // 开始录制
    mediaRecorder.start(100)

    // 更新状态
    store.startRecording()

    // 启动计时器
    timerInterval = setInterval(() => {
      store.setDuration(store.recordingState.duration + 1)
    }, 1000)

    console.log('开始录音')
  } catch (error) {
    console.error('无法访问麦克风:', error)
  }
}

function stopRecording() {
  if (mediaRecorder && mediaRecorder.state !== 'inactive') {
    mediaRecorder.stop()
  }

  // 停止计时器
  if (timerInterval) {
    clearInterval(timerInterval)
    timerInterval = null
  }

  // 更新状态
  store.stopRecording()
  console.log('停止录音，时长:', formattedDuration.value)
}

// 清理
onUnmounted(() => {
  if (timerInterval) {
    clearInterval(timerInterval)
  }
  if (mediaRecorder && mediaRecorder.state !== 'inactive') {
    mediaRecorder.stop()
  }
})
</script>

<style lang="scss" scoped>
.voice-input-bar {
  position: fixed;
  bottom: 0;
  left: 260px;
  right: 0;
  height: 130px;
  background: var(--color-bg-primary);
  border-top: 1px solid var(--color-border);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 200;
}

.mic-container {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* ========== 外圈扩散光环 ========== */
.outer-ring {
  position: absolute;
  width: 80px;
  height: 80px;
  border-radius: 50%;
  border: 1.5px solid rgba(79, 70, 229, 0.2);
  animation: ringExpand 2s ease-out infinite;
  pointer-events: none;
}

.outer-ring::before,
.outer-ring::after {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: 50%;
  border: inherit;
  border-color: inherit;
}

.outer-ring::after {
  animation-delay: 0.7s;
}

@keyframes ringExpand {
  0% {
    transform: scale(1);
    opacity: 0.6;
  }
  100% {
    transform: scale(1.8);
    opacity: 0;
  }
}

/* ========== 麦克风按钮 ========== */
.mic-btn {
  position: relative;
  z-index: 1;
  width: 56px;
  height: 56px;
  border-radius: 50%;
  border: none;
  background: var(--color-accent);
  color: white;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow:
    0 2px 8px rgba(79, 70, 229, 0.18),
    0 1px 2px rgba(0, 0, 0, 0.04);

  &:hover:not(.recording) {
    transform: scale(1.05);
    background: var(--color-accent-hover);
    box-shadow:
      0 4px 16px rgba(79, 70, 229, 0.25),
      0 2px 4px rgba(0, 0, 0, 0.06);
  }

  &:active:not(.recording) {
    transform: scale(0.97);
  }

  /* ---- 录音状态：同色系深化 ---- */
  &.recording {
    background: #3730a3; /* 深靛蓝，而非红色 */
    box-shadow:
      0 4px 20px rgba(55, 48, 163, 0.3),
      0 0 48px rgba(55, 48, 163, 0.12);
    animation: recGlow 2.5s ease-in-out infinite;

    &:hover {
      background: #312e81; /* 更深的靛蓝 */
    }
  }
}

/* 呼吸光效 */
@keyframes recGlow {
  0%, 100% {
    box-shadow:
      0 4px 20px rgba(55, 48, 163, 0.3),
      0 0 40px rgba(55, 48, 163, 0.08);
  }
  50% {
    box-shadow:
      0 4px 28px rgba(55, 48, 163, 0.38),
      0 0 56px rgba(55, 48, 163, 0.15);
  }
}

/* ========== 波形动画 ========== */
.waveform {
  position: absolute;
  bottom: calc(100% + 10px);
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  gap: 3px;
  align-items: flex-end;
  height: 28px;
  padding: 0 8px;
  animation: badgeIn 0.35s cubic-bezier(0.34, 1.56, 0.64, 1) 0.05s both;
}

.wave-bar {
  width: 3px;
  background: linear-gradient(to top, #4f46e5, #818cf8); /* 同系渐变 */
  border-radius: 2px;
  animation: waveBounce 1s ease-in-out infinite;
  opacity: 0.85;

  &:nth-child(odd) {
    opacity: 0.65;
  }
}

@keyframes waveBounce {
  0%, 100% {
    transform: scaleY(1);
  }
  50% {
    transform: scaleY(0.3);
  }
}

/* ========== 提示文字 ========== */
.hint-text {
  position: absolute;
  bottom: -28px;
  left: 50%;
  transform: translateX(-50%);
  font-size: 12px;
  color: var(--color-text-tertiary);
  white-space: nowrap;
  transition: all 0.25s ease;
  letter-spacing: 0.01em;

  &.active {
    color: #4f46e6; /* 靛蓝，非红色 */
    font-weight: 500;
  }
}
</style>
