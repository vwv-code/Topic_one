<template>
  <section class="content-area" ref="contentRef">
    <!-- 空状态：无消息时显示欢迎页 -->
    <div v-if="store.messages.length === 0 && !store.aiStreamingText" class="welcome-container">
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
        <p class="ai-description">
          你好，我是 Emma，你的 AI 英语口语伙伴。<br />
          点击下方麦克风开始对话。
        </p>
      </div>
    </div>

    <!-- 消息列表 -->
    <div v-else class="messages-container">
      <div
        v-for="msg in store.messages"
        :key="msg.id"
        :class="['message-item', `message-${msg.role}`]"
      >
        <!-- 头像 -->
        <div class="msg-avatar">
          <div v-if="msg.role === 'assistant'" class="avatar-small">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                 stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
              <path d="M12 8V4H8"/><rect width="16" height="12" x="4" y="8" rx="2"/>
              <path d="M15 13v2"/><path d="M9 13v2"/>
            </svg>
          </div>
          <span v-else class="user-label">你</span>
        </div>

        <!-- 内容 -->
        <div class="msg-content">
          <p class="msg-text">{{ msg.content }}</p>
          <span class="msg-time">{{ formatTime(msg.timestamp) }}</span>
        </div>
      </div>

      <!-- AI 正在流式输出（尚未完成的消息） -->
      <div v-if="store.aiStreamingText" class="message-item message-assistant streaming">
        <div class="msg-avatar">
          <div class="avatar-small">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                 stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
              <path d="M12 8V4H8"/><rect width="16" height="12" x="4" y="8" rx="2"/>
              <path d="M15 13v2"/><path d="M9 13v2"/>
            </svg>
          </div>
        </div>
        <div class="msg-content">
          <p class="msg-text">{{ store.aiStreamingText }}<span class="cursor-blink">|</span></p>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { ref, watch, nextTick } from 'vue'
import { useAppStore } from '@/stores/app'

const store = useAppStore()
const contentRef = ref<HTMLElement>()

/** 自动滚动到底部 */
async function scrollToBottom() {
  await nextTick()
  if (contentRef.value) {
    contentRef.value.scrollTop = contentRef.value.scrollHeight
  }
}

/** 格式化时间 */
function formatTime(date: Date): string {
  const h = date.getHours().toString().padStart(2, '0')
  const m = date.getMinutes().toString().padStart(2, '0')
  return `${h}:${m}`
}

// 监听消息变化，自动滚动
watch(
  () => [store.messages.length, store.aiStreamingText],
  () => scrollToBottom(),
  { deep: true }
)
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

// ========== 消息列表 ==========
.messages-container {
  max-width: 720px;
  width: 100%;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 20px;
  padding-top: 10px;
}

.message-item {
  display: flex;
  gap: 12px;
  animation: msgSlideIn 0.3s ease-out;

  &.message-user {
    flex-direction: row-reverse;

    .msg-content {
      background: var(--color-accent);
      color: white;
      border-radius: 18px 18px 4px 18px;
    }

    .msg-time { text-align: right; }
  }

  &.message-assistant {
    .msg-content {
      background: var(--color-bg-primary);
      color: var(--color-text-primary);
      border-radius: 18px 18px 18px 4px;
      border: 1px solid var(--color-border);
    }

    &.streaming .msg-content {
      border-color: var(--color-accent-muted);
    }
  }
}

@keyframes msgSlideIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.msg-avatar {
  flex-shrink: 0;
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.avatar-small {
  width: 36px;
  height: 36px;
  background: linear-gradient(135deg, #4f46e5, #818cf8);
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.user-label {
  font-size: 12px;
  font-weight: 600;
  color: var(--color-accent);
  background: rgba(79, 70, 229, 0.08);
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.msg-content {
  max-width: 75%;
  padding: 12px 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.msg-text {
  font-size: 14.5px;
  line-height: 1.7;
  word-break: break-word;
  margin: 0;
}

.msg-time {
  display: block;
  margin-top: 6px;
  font-size: 11px;
  color: inherit;
  opacity: 0.45;
}

.cursor-blink {
  animation: blink 0.8s step-end infinite;
  color: var(--color-accent);
  font-weight: 300;
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}
</style>
