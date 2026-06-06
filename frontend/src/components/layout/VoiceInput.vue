<template>
  <footer class="voice-input-bar">
    <div class="mic-container">
      <!-- 外圈光环（录音时显示） -->
      <div v-if="store.recordingState.isRecording" class="outer-ring"></div>

      <!-- 麦克风按钮 -->
      <button
        :class="['mic-btn', {
          recording: store.recordingState.isRecording,
          processing: store.aiStatus === 'processing' || store.aiStatus === 'speaking',
          disabled: !store.activeChatId
        }]"
        :disabled="!store.activeChatId || store.aiStatus === 'processing'"
        @click="handleToggleRecording"
      >
        <!-- 波形动画（录音中） -->
        <div v-if="store.recordingState.isRecording" class="waveform">
          <span
            v-for="(bar, index) in waveBars"
            :key="index"
            class="wave-bar"
            :style="{ animationDelay: `${index * 0.08}s`, height: waveHeights[index] + 'px' }"
          ></span>
        </div>

        <!-- 处理中/播放中：加载旋转 -->
        <svg v-else-if="store.aiStatus === 'processing' || store.aiStatus === 'speaking'"
             width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor"
             stroke-width="2" stroke-linecap="round" stroke-linejoin="round"
             class="spin-icon">
          <path d="M21 12a9 9 0 1 1-6.219-8.56"/>
        </svg>

        <!-- 默认图标：麦克风 -->
        <svg v-else width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M12 2a3 3 0 0 0-3 3v7a3 3 0 0 0 6 0V5a3 3 0 0 0-3-3Z"/>
          <path d="M19 10v2a7 7 0 0 1-14 0v-2"/>
          <line x1="12" x2="12" y1="19" y2="22"/>
        </svg>
      </button>

      <!-- 提示文字 -->
      <p :class="['hint-text', { active: store.recordingState.isRecording }]">
        {{ hintText }}
      </p>

      <!-- 实时转写文本（录音过程中显示） -->
      <transition name="fade">
        <p v-if="store.recognitionText && store.recordingState.isRecording" class="recognition-live">
          {{ store.recognitionText }}
        </p>
      </transition>
    </div>
  </footer>
</template>

<script setup lang="ts">
import { computed, ref, onUnmounted } from 'vue'
import { useAppStore } from '@/stores/app'

const store = useAppStore()

// 波形数据
const waveBars = ref(Array.from({ length: 5 }, () => ({})))
const waveHeights = [14, 22, 18, 26, 16]

// 计算属性
const hintText = computed(() => {
  if (!store.activeChatId) return '请先选择或创建一个会话'

  switch (store.aiStatus) {
    case 'recording': return '点击停止录音'
    case 'processing': return '正在思考...'
    case 'speaking': return '正在播放...'
    default: return '点击开始对话'
  }
})

/** 切换录音状态 */
async function handleToggleRecording() {
  if (!store.activeChatId) return

  // 如果正在处理/播放中，强制关闭所有连接
  if (store.aiStatus === 'processing' || store.aiStatus === 'speaking') {
    store.forceCloseAll()
    return
  }

  if (store.recordingState.isRecording) {
    // 停止录音 → 触发 ASR → LLM → TTS 流水线
    store.stopVoiceSession()
  } else {
    // 开始语音对话：建立 WebSocket + PCM 录音
    await store.startVoiceSession()
  }
}

// 组件卸载时清理
onUnmounted(() => {
  store.forceCloseAll()
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

  &::before,
  &::after {
    content: '';
    position: absolute;
    inset: 0;
    border-radius: 50%;
    border: inherit;
    border-color: inherit;
  }

  &::after {
    animation-delay: 0.7s;
  }
}

@keyframes ringExpand {
  0% { transform: scale(1); opacity: 0.6; }
  100% { transform: scale(1.8); opacity: 0; }
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

  &:hover:not(.recording):not(.processing):not(.disabled) {
    transform: scale(1.05);
    background: var(--color-accent-hover);
    box-shadow:
      0 4px 16px rgba(79, 70, 229, 0.25),
      0 2px 4px rgba(0, 0, 0, 0.06);
  }

  &:active:not(.recording):not(.processing):not(.disabled) {
    transform: scale(0.97);
  }

  /* 录音中 */
  &.recording {
    background: #3730a3;
    box-shadow:
      0 4px 20px rgba(55, 48, 163, 0.3),
      0 0 48px rgba(55, 48, 163, 0.12);
    animation: recGlow 2.5s ease-in-out infinite;

    &:hover {
      background: #312e81;
    }
  }

  /* 处理/播放中 */
  &.processing {
    background: #6366f1;
    cursor: wait;

    &:hover {
      background: #4f46e5;
    }
  }

  /* 禁用（无会话时） */
  &.disabled {
    background: var(--color-border);
    cursor: not-allowed;
    opacity: 0.6;
  }
}

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

/* 加载旋转图标 */
.spin-icon {
  animation: spin 1.5s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
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
  background: linear-gradient(to top, #4f46e5, #818cf8);
  border-radius: 2px;
  animation: waveBounce 1s ease-in-out infinite;
  opacity: 0.85;

  &:nth-child(odd) { opacity: 0.65; }
}

@keyframes waveBounce {
  0%, 100% { transform: scaleY(1); }
  50% { transform: scaleY(0.3); }
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
    color: #4f46e6;
    font-weight: 500;
  }
}

/* ========== 实时转写文字 ========== */
.recognition-live {
  position: absolute;
  bottom: -52px;
  left: 50%;
  transform: translateX(-50%);
  max-width: 320px;
  font-size: 13px;
  color: var(--color-text-secondary);
  line-height: 1.5;
  text-align: center;
  white-space: pre-wrap;
  word-break: break-word;
  padding: 8px 16px;
  background: var(--color-bg-secondary);
  border-radius: 10px;
  border: 1px solid var(--color-border);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);

  &::before {
    content: '';
    position: absolute;
    top: -5px;
    left: 50%;
    transform: translateX(-50%);
    width: 8px;
    height: 8px;
    background: var(--color-bg-secondary);
    border-left: 1px solid var(--color-border);
    border-top: 1px solid var(--color-border);
    transform: translateX(-50%) rotate(45deg);
  }
}

.fade-enter-active { transition: all 0.25s ease; }
.fade-leave-active { transition: all 0.15s ease; }
.fade-enter-from { opacity: 0; transform: translateY(6px); }
.fade-leave-to { opacity: 0; }
</style>
