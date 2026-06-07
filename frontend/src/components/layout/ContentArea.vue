<template>
  <section class="content-area" ref="contentRef">
    <!-- 始终显示欢迎页（不展示对话消息列表） -->
    <div class="welcome-container">
      <div class="ai-container">
        <!-- AI 虚拟人头像 -->
        <div class="avatar-wrapper">
          <div class="avatar-ring"></div>
          <div class="avatar-main" :class="{ active: store.aiStatus === 'recording' }">
            <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                 stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
              <path d="M12 8V4H8"/>
              <rect width="16" height="12" x="4" y="8" rx="2"/>
              <path d="M2 14h2"/>
              <path d="M20 14h2"/>
              <path d="M15 13v2"/>
              <path d="M9 13v2"/>
            </svg>
          </div>
        </div>

        <h2 class="ai-name">Emma</h2>
        <!-- 提示文字：仅在空闲状态显示，对话中隐藏 -->
        <p v-if="store.aiStatus === 'ready'" class="ai-description">
          你好，我是 Emma，你的 AI 英语口语伙伴。<br />
          点击下方麦克风开始对话。
        </p>
        <!-- 字幕：开启字幕且语音播放时显示 -->
        <transition name="subtitle-fade">
          <p v-if="store.subtitleVisible" class="subtitle-text">{{ store.subtitleText }}</p>
        </transition>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useAppStore } from '@/stores/app'

const store = useAppStore()
const contentRef = ref<HTMLElement>()
</script>

<style lang="scss" scoped>
.content-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
  padding: 20px 40px 160px;
  background: var(--color-bg-secondary);
}

// ========== 欢迎页 ==========
.welcome-container {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.ai-container {
  text-align: center;
  animation: fadeInUp 0.6s ease-out;
}

@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}

.avatar-wrapper {
  position: relative;
  display: inline-block;
  margin-bottom: 28px;
}

.avatar-ring {
  position: absolute;
  inset: -10px;
  border-radius: 50%;
  border: 1px solid var(--color-accent-muted);
  animation: ringRotate 8s linear infinite;

  &::before {
    content: '';
    position: absolute;
    top: -3px;
    left: 50%;
    transform: translateX(-50%);
    width: 6px;
    height: 6px;
    background: var(--color-accent);
    border-radius: 50%;
  }
}

@keyframes ringRotate {
  to { transform: rotate(360deg); }
}

.avatar-main {
  width: 100px;
  height: 100px;
  background: var(--color-bg-primary);
  color: var(--color-text-tertiary);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: var(--shadow-lg);
  transition: all 0.3s ease;

  &.active {
    color: var(--color-accent);
    box-shadow: 0 0 0 4px var(--color-accent-light), var(--shadow-lg);

    svg { animation: pulse 1.5s ease-in-out infinite; }
  }
}

@keyframes pulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.05); }
}

.ai-name {
  font-size: 22px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin-bottom: 8px;
  letter-spacing: -0.02em;
}

.ai-description {
  font-size: 14px;
  color: var(--color-text-tertiary);
  line-height: 1.7;
  margin-bottom: 28px;
  max-width: 320px;
}

// ========== 字幕 ==========
.subtitle-text {
  font-size: 16px;
  color: var(--color-text-primary);
  line-height: 1.8;
  margin-bottom: 28px;
  max-width: 400px;
  min-height: 1.8em;
  word-break: break-word;
  animation: subtitleIn 0.3s ease-out;
}

@keyframes subtitleIn {
  from { opacity: 0; transform: translateY(6px); }
  to { opacity: 1; transform: translateY(0); }
}

.subtitle-fade-enter-active,
.subtitle-fade-leave-active {
  transition: opacity 0.3s ease;
}
.subtitle-fade-enter-from,
.subtitle-fade-leave-to {
  opacity: 0;
}
</style>
