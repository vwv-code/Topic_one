<template>
  <div class="settings-page">
    <div class="settings-container">
      <!-- 返回按钮 -->
      <div class="page-header">
        <button class="back-btn" @click="$router.push('/')">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
            <path d="m15 18-6-6 6-6"/>
          </svg>
          返回
        </button>
        <h1 class="page-title">设置</h1>
      </div>

      <div class="settings-sections">
        <!-- 场景选择 -->
        <section class="setting-section">
          <h3 class="section-label">对话场景</h3>
          <div class="scene-grid">
            <button
              v-for="scene in allScenes"
              :key="scene.value"
              :class="['scene-card', { active: form.scene === scene.value }]"
              @click="form.scene = scene.value"
            >
              <!-- 自定义场景：彩色首字母头像 -->
              <span
                v-if="isCustomIcon(scene.icon)"
                class="scene-icon letter-avatar"
                :style="{ background: getIconColor(scene.icon) }"
              >{{ getIconLetter(scene.icon) }}</span>
              <!-- 内置场景：emoji -->
              <span v-else class="scene-icon">{{ scene.icon }}</span>
              <span class="scene-name">{{ scene.label }}</span>
            </button>
            <!-- 自定义按钮 -->
            <button
              class="scene-card custom-card"
              @click="showModal = true"
            >
              <span class="scene-icon custom-icon">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
                  <line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/>
                </svg>
              </span>
              <span class="scene-name">自定义</span>
            </button>
          </div>
        </section>

        <!-- 难度等级 -->
        <section class="setting-section">
          <h3 class="section-label">难度等级</h3>
          <div class="difficulty-options">
            <button
              v-for="level in difficulties"
              :key="level.value"
              :class="['diff-btn', { active: form.difficulty === level.value }]"
              @click="form.difficulty = level.value"
            >
              {{ level.label }}
            </button>
          </div>
        </section>

        <!-- 语速调节 -->
        <section class="setting-section">
          <div class="slider-header">
            <h3 class="section-label">AI 语音速度</h3>
            <span class="speed-value">{{ form.speed }}x</span>
          </div>
          <input
            type="range"
            v-model.number="form.speed"
            min="0.5"
            max="2"
            step="0.1"
            class="speed-slider"
          />
          <div class="slider-marks">
            <span>慢</span>
            <span>正常</span>
            <span>快</span>
          </div>
        </section>

        <!-- 保存按钮 -->
        <div class="action-area">
          <button class="save-btn" @click="handleSave">保存设置</button>
        </div>
      </div>
    </div>

    <!-- 自定义场景弹窗 -->
    <Teleport to="body">
      <Transition name="modal-fade">
        <div v-if="showModal" class="modal-overlay" @click.self="showModal = false">
          <div class="modal-container">
            <div class="modal-header">
              <h2 class="modal-title">新建自定义场景</h2>
              <button class="modal-close" @click="showModal = false">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
                  <path d="M18 6L6 18M6 6l12 12"/>
                </svg>
              </button>
            </div>
            <div class="modal-body">
              <label class="input-label">场景名称</label>
              <div class="input-row">
                <input
                  v-model="customSceneName"
                  ref="inputRef"
                  type="text"
                  class="scene-input"
                  placeholder="例如：酒店入住、医院挂号..."
                  maxlength="20"
                  @keyup.enter="handleConfirm"
                />
                <span
                  v-if="customSceneName.trim()"
                  class="avatar-preview"
                  :style="{ background: previewColor }"
                >{{ previewLetter }}</span>
              </div>
              <p class="input-hint">输入后点击确认，新场景将添加到列表中</p>
            </div>
            <div class="modal-footer">
              <button class="btn-cancel" @click="showModal = false">取消</button>
              <button
                class="btn-confirm"
                :disabled="!customSceneName.trim()"
                @click="handleConfirm"
              >确认添加</button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, nextTick, watch, computed } from 'vue'
import { useAppStore } from '@/stores/app'

const store = useAppStore()

// 彩色头像色池（低饱和，与整体设计协调）
const colorPool = [
  '#6366f1', // 靛蓝
  '#8b5cf6', // 紫罗兰
  '#06b6d4', // 青
  '#10b981', // 翠绿
  '#f59e0b', // 琥珀
  '#ef4444', // 红
  '#ec4899', // 粉
  '#3b82f6', // 蓝
]

let customColorIndex = 0

// 弹窗预览：取首字 + 按已创建数量分配颜色
const previewLetter = computed(() => {
  const name = customSceneName.value.trim()
  return name ? name.charAt(0).toUpperCase() : ''
})

const previewColor = computed(() => {
  return colorPool[customScenes.value.length % colorPool.length]
})

const form = reactive({
  scene: 'daily',
  difficulty: 'intermediate',
  speed: 1.0
})

const baseScenes = [
  { value: 'daily', label: '日常对话', icon: '💬' },
  { value: 'restaurant', label: '餐厅点餐', icon: '🍽️' },
  { value: 'business', label: '商务会议', icon: '💼' },
  { value: 'travel', label: '旅游问路', icon: '✈️' },
  { value: 'interview', label: '面试自我介绍', icon: '📋' }
]

// 用户自建的场景列表（运行时追加）
const customScenes = ref<Array<{ value: string; label: string; icon: string }>>([])

const allScenes = computed(() => [...baseScenes, ...customScenes.value])

const difficulties = [
  { value: 'beginner', label: '初级' },
  { value: 'intermediate', label: '中级' },
  { value: 'advanced', label: '高级' }
]

// 弹窗状态
const showModal = ref(false)
const customSceneName = ref('')
const inputRef = ref<HTMLInputElement | null>(null)

watch(showModal, (val) => {
  if (val) {
    customSceneName.value = ''
    nextTick(() => inputRef.value?.focus())
  }
})

function handleConfirm() {
  const name = customSceneName.value.trim()
  if (!name) return

  const value = 'custom_' + Date.now()
  const color = colorPool[customScenes.value.length % colorPool.length]
  // 使用彩色首字母头像，格式：首字|颜色
  const icon = `${name.charAt(0).toUpperCase()}|${color}`
  const newScene = { value, label: name, icon }
  customScenes.value.push(newScene)

  customColorIndex++
  form.scene = value
  showModal.value = false
}

function handleSave() {
  const sceneLabel = allScenes.value.find((s) => s.value === form.scene)?.label || ''
  store.currentScene = sceneLabel + '模式'
}

// 判断是否为自定义图标格式（首字|颜色）
function isCustomIcon(icon: string): boolean {
  return icon.includes('|')
}

function getIconLetter(icon: string): string {
  return icon.split('|')[0] || '?'
}

function getIconColor(icon: string): string {
  return icon.split('|')[1] || '#6366f1'
}
</script>

<style lang="scss" scoped>
.settings-page {
  min-height: 100vh;
  background: var(--color-bg-secondary);
  padding: 40px;
}

.settings-container {
  max-width: 560px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 36px;
}

.back-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  background: var(--color-bg-primary);
  color: var(--color-text-secondary);
  font-size: 13.5px;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    background: var(--color-bg-hover);
    color: var(--color-text-primary);
    border-color: var(--color-border-hover);
  }
}

.page-title {
  font-size: 22px;
  font-weight: 600;
  color: var(--color-text-primary);
}

.setting-section {
  background: var(--color-bg-primary);
  border: 1px solid var(--color-border);
  border-radius: 12px;
  padding: 20px 22px;
  margin-bottom: 16px;

  &:last-child {
    margin-bottom: 0;
  }
}

.section-label {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-secondary);
  margin-bottom: 14px;
  text-transform: uppercase;
  letter-spacing: 0.03em;
}

// 场景网格
.scene-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
}

.scene-card {
  padding: 14px 12px;
  border: 1px solid var(--color-border);
  border-radius: 10px;
  background: transparent;
  cursor: pointer;
  transition: all 0.2s;
  text-align: center;

  .scene-icon {
    font-size: 22px;
    display: block;
    margin-bottom: 6px;
  }

  .scene-name {
    font-size: 12.5px;
    color: var(--color-text-secondary);
    font-weight: 500;
  }

  &:hover {
    border-color: var(--color-border-hover);
    background: var(--color-bg-hover);
  }

  &.active {
    border-color: var(--color-accent-muted);
    background: var(--color-accent-light);

    .scene-name {
      color: var(--color-accent);
    }
  }

  &.custom-card {
    border-style: dashed;
    border-color: var(--color-border-hover);

    .custom-icon {
      width: 36px;
      height: 36px;
      margin: 0 auto 6px;
      display: flex;
      align-items: center;
      justify-content: center;
      background: var(--color-bg-tertiary);
      border-radius: 8px;
      color: var(--color-text-tertiary);
      transition: all 0.2s;
    }

    &:hover {
      border-color: var(--color-accent-muted);

      .custom-icon {
        background: var(--color-accent-subtle);
        color: var(--color-accent);
      }
    }
  }
}

// 难度选项
.difficulty-options {
  display: flex;
  gap: 8px;
}

.diff-btn {
  flex: 1;
  padding: 10px 16px;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  background: transparent;
  color: var(--color-text-secondary);
  font-size: 13.5px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    background: var(--color-bg-hover);
  }

  &.active {
    border-color: var(--color-accent);
    background: var(--color-accent-light);
    color: var(--color-accent);
  }
}

// 滑块样式
.slider-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;

  .section-label {
    margin-bottom: 0;
  }

  .speed-value {
    font-size: 15px;
    font-weight: 600;
    color: var(--color-accent);
    font-family: 'SF Mono', monospace;
  }
}

.speed-slider {
  width: 100%;
  height: 6px;
  -webkit-appearance: none;
  appearance: none;
  background: var(--color-border);
  border-radius: 3px;
  outline: none;

  &::-webkit-slider-thumb {
    -webkit-appearance: none;
    width: 18px;
    height: 18px;
    border-radius: 50%;
    background: var(--color-accent);
    cursor: pointer;
    box-shadow: 0 2px 6px rgba(79, 70, 229, 0.25);
    transition: transform 0.15s;

    &:hover {
      transform: scale(1.15);
    }
  }
}

.slider-marks {
  display: flex;
  justify-content: space-between;
  margin-top: 8px;
  font-size: 11.5px;
  color: var(--color-text-tertiary);
}

.action-area {
  margin-top: 28px;
  text-align: right;
}

.save-btn {
  padding: 11px 28px;
  background: var(--color-accent);
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    background: var(--color-accent-hover);
  }
}

/* ====== 弹窗样式 ====== */
.modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(17, 24, 39, 0.35);
  backdrop-filter: blur(4px);
}

.modal-container {
  width: 420px;
  max-width: 90vw;
  background: var(--color-bg-primary);
  border: 1px solid var(--color-border);
  border-radius: 14px;
  box-shadow:
    0 20px 60px rgba(0, 0, 0, 0.08),
    0 0 0 1px rgba(255, 255, 255, 0.6) inset;
  overflow: hidden;
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 22px 0;
}

.modal-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text-primary);
}

.modal-close {
  width: 30px;
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  border-radius: 7px;
  background: transparent;
  color: var(--color-text-tertiary);
  cursor: pointer;
  transition: all 0.15s;

  &:hover {
    background: var(--color-bg-tertiary);
    color: var(--color-text-secondary);
  }
}

.modal-body {
  padding: 18px 22px;
}

.input-label {
  display: block;
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text-secondary);
  margin-bottom: 8px;
}

.scene-input {
  width: 100%;
  padding: 10px 13px;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  font-size: 14px;
  color: var(--color-text-primary);
  background: var(--color-bg-secondary);
  outline: none;
  transition: border-color 0.2s;
  box-sizing: border-box;

  &::placeholder {
    color: var(--color-text-tertiary);
  }

  &:focus {
    border-color: var(--color-accent);
    box-shadow: 0 0 0 3px var(--color-accent-subtle);
  }
}

// 输入行（输入框 + 头像预览）
.input-row {
  display: flex;
  align-items: center;
  gap: 10px;

  .scene-input {
    flex: 1;
  }
}

// 弹窗内头像预览
.avatar-preview {
  flex-shrink: 0;
  width: 38px;
  height: 38px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 15px;
  font-weight: 600;
  color: #fff;
  animation: avatarPop 0.25s ease-out;
}

@keyframes avatarPop {
  from { transform: scale(0.6); opacity: 0; }
  to   { transform: scale(1); opacity: 1; }
}

// 场景卡片内的彩色首字母头像
.letter-avatar {
  font-size: 20px !important;
  width: 28px;
  height: 28px;
  margin: 0 auto 6px !important;
  border-radius: 7px;
  display: flex !important;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-weight: 600;
}

.input-hint {
  margin-top: 8px;
  font-size: 12px;
  color: var(--color-text-tertiary);
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 0 22px 18px;
}

.btn-cancel,
.btn-confirm {
  padding: 9px 18px;
  border-radius: 8px;
  font-size: 13.5px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-cancel {
  border: 1px solid var(--color-border);
  background: transparent;
  color: var(--color-text-secondary);

  &:hover {
    background: var(--color-bg-tertiary);
  }
}

.btn-confirm {
  border: none;
  background: var(--color-accent);
  color: #fff;

  &:hover:not(:disabled) {
    background: var(--color-accent-hover);
  }

  &:disabled {
    opacity: 0.4;
    cursor: not-allowed;
  }
}

/* 过渡动画 */
.modal-fade-enter-active,
.modal-fade-leave-active {
  transition: opacity 0.2s ease;

  .modal-container {
    transition: transform 0.2s ease, opacity 0.2s ease;
  }
}

.modal-fade-enter-from,
.modal-fade-leave-to {
  opacity: 0;

  .modal-container {
    transform: translateY(12px) scale(0.97);
    opacity: 0;
  }
}
</style>
