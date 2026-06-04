<template>
  <section class="content-area">
    <div class="ai-container">
      <!-- AI 虚拟人头像 - 极简设计 -->
      <div class="avatar-wrapper">
        <div class="avatar-ring"></div>
        <div class="avatar-main" :class="{ active: store.aiStatus === 'recording' }">
          <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
            <path d="M12 8V4H8"/>
            <rect width="16" height="12" x="4" y="8" rx="2"/>
            <path d="M2 14h2"/>
            <path d="M20 14h2"/>
            <path d="M15 13v2"/>
            <path d="M9 13v2"/>
          </svg>
        </div>
      </div>

      <!-- AI 名称和描述 -->
      <h2 class="ai-name">Emma</h2>
      <p class="ai-description">
        你好，我是 Emma，你的 AI 英语口语伙伴。<br />
        点击下方麦克风开始对话。
      </p>

      <!-- 状态指示器 -->
    </div>
  </section>
</template>

<script setup lang="ts">
import { useAppStore } from '@/stores/app'

const store = useAppStore()
</script>

<style lang="scss" scoped>
.content-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px 150px;
  background: var(--color-bg-secondary);
}

.ai-container {
  text-align: center;
  animation: fadeInUp 0.6s ease-out;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

// 头像区域
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
  to {
    transform: rotate(360deg);
  }
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
    
    svg {
      animation: pulse 1.5s ease-in-out infinite;
    }
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
</style>
