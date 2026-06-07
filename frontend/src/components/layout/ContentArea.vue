<template>
  <!-- 沉浸式全屏模式：覆盖整个视口 -->
  <div
    v-if="store.isImmersiveFullscreen"
    class="immersive-fullscreen"
    :style="bgStyle"
    ref="fullscreenRef"
    @mousemove="onMouseMove"
  >
    <!-- 字幕始终可见 -->
    <p v-if="store.subtitleVisible && store.subtitleText" class="floating-subtitle">
      {{ store.subtitleText }}
    </p>

    <!-- 底部浮动工具栏 -->
    <transition name="controls-fade">
      <div v-show="controlsVisible" class="floating-toolbar">
        <!-- 字幕开关 -->
          <button
            :class="['floating-icon-btn', { active: store.subtitleEnabled }]"
            @click="store.toggleSubtitle()"
          >
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <rect x="1" y="4" width="22" height="16" rx="2"/>
              <path d="M7 12h4m-2-2v4"/>
              <line x1="15" y1="11" x2="19" y2="11"/>
              <line x1="15" y1="15" x2="17" y2="15"/>
            </svg>
          </button>

          <!-- 音量滑块 -->
          <div class="volume-control">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <polygon points="11 5 6 9 2 9 2 15 6 15 11 19 11 5"/>
              <path d="M19.07 4.93a10 10 0 0 1 0 14.14M15.54 8.46a5 5 0 0 1 0 7.07"/>
            </svg>
            <input
              type="range"
              min="0"
              max="100"
              :value="store.ttsVolume"
              @input="onVolumeChange"
              class="volume-slider"
            />
          </div>

          <!-- 结束对话 -->
          <button class="floating-stop-btn" @click="handleStopMic">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor" stroke="none">
              <rect x="6" y="6" width="12" height="12" rx="2"/>
            </svg>
          </button>
      </div>
    </transition>
  </div>

  <!-- 普通模式 -->
  <section
    v-else
    class="content-area"
    :class="{ 'has-bg': store.backgroundImageUrl && !store.backgroundLoading }"
    ref="contentRef"
    :style="bgStyle"
  >
    <!-- 背景图加载中遮罩 -->
    <div v-if="store.backgroundLoading" class="bg-loading-overlay">
      <div class="bg-spinner"></div>
      <p>正在生成场景背景...</p>
    </div>

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

        <p v-if="store.aiStatus === 'ready'" class="ai-description">
          你好，我是你的 AI 英语口语伙伴。<br />
          点击下方麦克风开始对话。
        </p>
        <transition name="subtitle-fade">
          <p v-if="store.subtitleVisible" class="subtitle-text">{{ store.subtitleText }}</p>
        </transition>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useAppStore } from '@/stores/app'

const store = useAppStore()
const contentRef = ref<HTMLElement>()
const fullscreenRef = ref<HTMLElement>()

// ========== 浏览器全屏同步 ==========
async function enterBrowserFullscreen() {
  try {
    await document.documentElement.requestFullscreen()
  } catch {
    // 浏览器可能拒绝全屏（需用户手势触发时可能失败）
  }
}

function exitBrowserFullscreen() {
  if (document.fullscreenElement) {
    document.exitFullscreen().catch(() => {})
  }
}

// 监听沉浸式全屏状态变化 → 同步浏览器全屏
watch(
  () => store.isImmersiveFullscreen,
  (val) => {
    if (val) {
      enterBrowserFullscreen()
    } else {
      exitBrowserFullscreen()
    }
  }
)

// 用户按 Esc 退出浏览器全屏 → 结束对话
function onFullscreenChange() {
  if (!document.fullscreenElement && store.isImmersiveFullscreen) {
    store.forceCloseAll()
  }
}

onMounted(() => {
  document.addEventListener('fullscreenchange', onFullscreenChange)
})

onUnmounted(() => {
  document.removeEventListener('fullscreenchange', onFullscreenChange)
  clearHideTimer()
})

// ========== 沉浸式全屏：鼠标移动显隐控制栏 ==========
const controlsVisible = ref(false)
let hideTimer: ReturnType<typeof setTimeout> | null = null

function onMouseMove() {
  controlsVisible.value = true
  clearHideTimer()
  hideTimer = setTimeout(() => {
    controlsVisible.value = false
  }, 3000)
}

function clearHideTimer() {
  if (hideTimer) {
    clearTimeout(hideTimer)
    hideTimer = null
  }
}

function handleStopMic() {
  store.stopVoiceSession()
}

function onVolumeChange(e: Event) {
  const val = Number((e.target as HTMLInputElement).value)
  store.setTtsVolume(val)
}

// ========== 背景样式 ==========
const bgStyle = computed(() => {
  if (store.backgroundImageUrl) {
    return {
      backgroundImage: `url(${store.backgroundImageUrl})`,
      backgroundSize: 'cover',
      backgroundPosition: 'center',
      backgroundRepeat: 'no-repeat'
    }
  }
  return {}
})
</script>

<style lang="scss" scoped>
// ========== 沉浸式全屏 ==========
.immersive-fullscreen {
  position: fixed;
  inset: 0;
  z-index: 9999;
  background-color: #1a1a2e;

  // 半透明暗色遮罩让控件更可见
  &::before {
    content: '';
    position: absolute;
    inset: 0;
    background: linear-gradient(
      to top,
      rgba(0, 0, 0, 0.4) 0%,
      rgba(0, 0, 0, 0.05) 40%,
      rgba(0, 0, 0, 0) 70%
    );
    pointer-events: none;
  }
}

.floating-toolbar {
  position: absolute;
  bottom: 40px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 2;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: rgba(0, 0, 0, 0.45);
  border-radius: 28px;
  backdrop-filter: blur(12px);
}

.floating-subtitle {
  position: absolute;
  bottom: 110px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 2;
  font-size: 18px;
  color: #fff;
  text-align: center;
  max-width: 600px;
  line-height: 1.6;
  text-shadow: 0 2px 12px rgba(0, 0, 0, 0.6);
  margin: 0;
  padding: 10px 20px;
  background: rgba(0, 0, 0, 0.35);
  border-radius: 12px;
  backdrop-filter: blur(8px);
}

.floating-icon-btn {
  width: 38px;
  height: 38px;
  border-radius: 50%;
  border: none;
  background: transparent;
  color: rgba(255, 255, 255, 0.7);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;

  &:hover {
    background: rgba(255, 255, 255, 0.12);
    color: #fff;
  }

  &.active {
    color: #fff;
    background: rgba(79, 70, 229, 0.5);
  }
}

.volume-control {
  display: flex;
  align-items: center;
  gap: 6px;
  color: rgba(255, 255, 255, 0.7);
  padding: 0 4px;

  svg {
    flex-shrink: 0;
  }
}

.volume-slider {
  width: 80px;
  height: 4px;
  -webkit-appearance: none;
  appearance: none;
  background: rgba(255, 255, 255, 0.25);
  border-radius: 2px;
  outline: none;
  cursor: pointer;

  &::-webkit-slider-thumb {
    -webkit-appearance: none;
    width: 14px;
    height: 14px;
    border-radius: 50%;
    background: #fff;
    cursor: pointer;
    box-shadow: 0 1px 4px rgba(0, 0, 0, 0.3);
  }

  &::-moz-range-thumb {
    width: 14px;
    height: 14px;
    border-radius: 50%;
    background: #fff;
    cursor: pointer;
    border: none;
  }
}

.floating-stop-btn {
  width: 38px;
  height: 38px;
  border-radius: 50%;
  border: none;
  background: rgba(239, 68, 68, 0.75);
  color: #fff;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;

  &:hover {
    background: rgba(239, 68, 68, 1);
    transform: scale(1.08);
  }
}

.controls-fade-enter-active,
.controls-fade-leave-active {
  transition: opacity 0.4s ease;
}

.controls-fade-enter-from,
.controls-fade-leave-to {
  opacity: 0;
}

// ========== 普通模式 ==========
.content-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
  padding: 20px 40px 160px;
  background: var(--color-bg-secondary);
  position: relative;

  &::before {
    content: '';
    display: none;
    position: absolute;
    inset: 0;
    background: rgba(255, 255, 255, 0.75);
    z-index: 0;
  }

  &.has-bg::before {
    display: block;
  }
}

.bg-loading-overlay {
  position: absolute;
  inset: 0;
  z-index: 10;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.85);
  gap: 16px;

  p {
    font-size: 14px;
    color: var(--color-text-tertiary);
    margin: 0;
  }
}

.bg-spinner {
  width: 36px;
  height: 36px;
  border: 3px solid var(--color-border);
  border-top-color: var(--color-accent);
  border-radius: 50%;
  animation: bgSpin 0.8s linear infinite;
}

@keyframes bgSpin {
  to { transform: rotate(360deg); }
}

// ========== 欢迎页 ==========
.welcome-container {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  z-index: 1;
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

.ai-description {
  font-size: 14px;
  color: var(--color-text-tertiary);
  line-height: 1.7;
  margin-bottom: 28px;
  max-width: 320px;
}

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
