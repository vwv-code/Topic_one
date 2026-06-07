<template>
  <header class="top-header">
    <div class="header-left">
      <el-tooltip content="设置场景" placement="bottom" :show-after="300">
        <button class="settings-btn" @click="$router.push('/settings')">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="12" cy="12" r="3"/>
            <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06A1.65 1.65 0 0 0 4.68 15a1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06A1.65 1.65 0 0 0 9 4.68a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"/>
          </svg>
        </button>
      </el-tooltip>
      <span class="scene-badge">{{ store.currentScene }}</span>
    </div>

    <div class="header-center">
      <input
        v-if="store.activeChatId"
        ref="titleInputRef"
        v-model="editingTitle"
        class="title-input"
        :class="{ editing: isEditing }"
        placeholder="输入对话标题..."
        maxlength="30"
        @focus="isEditing = true"
        @blur="handleSaveTitle"
        @keyup.enter="titleInputRef?.blur()"
      />
      <span v-else class="header-title">AI 口语陪练</span>
    </div>

    <div class="header-right">
      <button
        :class="['subtitle-btn', { active: store.subtitleEnabled }]"
        @click="store.toggleSubtitle()"
      >
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <rect x="1" y="4" width="22" height="16" rx="2"/>
          <path d="M7 12h4m-2-2v4"/>
          <line x1="15" y1="11" x2="19" y2="11"/>
          <line x1="15" y1="15" x2="17" y2="15"/>
        </svg>
        字幕
      </button>
      <button
        :class="['subtitle-btn', { active: immersiveActive }]"
        @click="onImmersiveClick"
      >
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <rect x="3" y="3" width="18" height="18" rx="2"/>
          <circle cx="8.5" cy="8.5" r="1.5"/>
          <polyline points="21 15 16 10 5 21"/>
        </svg>
        沉浸体验
      </button>

    </div>
  </header>

  <!-- 沉浸式体验确认弹窗 -->
  <Teleport to="body">
    <Transition name="modal-fade">
      <div v-if="showImmersiveModal" class="modal-overlay" @click.self="showImmersiveModal = false">
        <div class="immersive-modal">
          <p class="immersive-modal-text">
            是否开启沉浸式体验？<br/>
            <span class="immersive-modal-sub">开启后将为当前对话场景生成背景画面</span>
          </p>
          <div class="immersive-modal-btns">
            <button class="immersive-btn btn-no" @click="cancelImmersive">否</button>
            <button class="immersive-btn btn-yes" @click="confirmImmersive">是</button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { ref, computed, watch, nextTick } from 'vue'
import { useAppStore } from '@/stores/app'

const store = useAppStore()

const titleInputRef = ref<HTMLInputElement | null>(null)
const isEditing = ref(false)
const editingTitle = ref('')

// 沉浸式体验确认弹窗
const showImmersiveModal = ref(false)

const immersiveActive = computed(() =>
  store.immersiveEnabled || store.backgroundLoading
)

function onImmersiveClick() {
  if (store.immersiveEnabled) {
    // 已开启 → 关闭
    store.disableImmersive()
  } else {
    // 未开启 → 弹确认窗
    showImmersiveModal.value = true
  }
}

function confirmImmersive() {
  showImmersiveModal.value = false
  store.enableImmersive()
}

function cancelImmersive() {
  showImmersiveModal.value = false
}

// 从当前激活会话的 title 同步
const activeChatTitle = computed(() => {
  const chat = store.chatHistories.find(h => h.isActive)
  return chat?.title || ''
})

watch(activeChatTitle, (val) => {
  editingTitle.value = val
}, { immediate: true })

async function handleSaveTitle() {
  isEditing.value = false
  const newTitle = editingTitle.value.trim()
  if (!newTitle || !store.activeChatId) return
  // 恢复原标题（保存失败时回滚）
  const oldTitle = activeChatTitle.value
  if (newTitle === oldTitle) return
  try {
    await store.updateChatTitle(Number(store.activeChatId), newTitle)
  } catch (e) {
    console.error('保存标题失败:', e)
    editingTitle.value = oldTitle
  }
}
</script>

<style lang="scss" scoped>
.top-header {
  height: 56px;
  background: var(--color-bg-primary);
  border-bottom: 1px solid var(--color-border);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 28px;
  position: sticky;
  top: 0;
  z-index: 50;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.settings-btn {
  width: 34px;
  height: 34px;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: var(--color-text-secondary);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;

  &:hover {
    background: var(--color-bg-hover);
    color: var(--color-text-primary);
  }
}

.scene-badge {
  padding: 5px 12px;
  background: var(--color-accent-light);
  color: var(--color-accent);
  border-radius: 6px;
  font-size: 12.5px;
  font-weight: 500;
}

.header-center {
  display: flex;
  align-items: center;
  justify-content: center;
}

.header-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text-primary);
  letter-spacing: -0.01em;
}

.title-input {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text-primary);
  letter-spacing: -0.01em;
  border: none;
  background: transparent;
  text-align: center;
  outline: none;
  width: 200px;
  padding: 4px 8px;
  border-radius: 6px;
  transition: all 0.2s ease;

  &.editing {
    background: var(--color-bg-hover);
    box-shadow: 0 0 0 1.5px var(--color-accent);
    width: 240px;
  }

  &::placeholder {
    color: var(--color-text-tertiary);
    font-weight: 400;
  }
}

.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.subtitle-btn {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 6px 12px;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: var(--color-text-secondary);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover {
    background: var(--color-bg-hover);
    color: var(--color-text-primary);
  }

  &.active {
    color: var(--color-accent);
    background: rgba(79, 70, 229, 0.08);

    &:hover {
      background: rgba(79, 70, 229, 0.14);
    }
  }
}
</style>

<style lang="scss">
/* 沉浸式体验确认弹窗（Teleport 到 body，不能用 scoped） */
.immersive-modal {
  width: 380px;
  max-width: 90vw;
  background: var(--color-bg-primary);
  border: 1px solid var(--color-border);
  border-radius: 14px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.1);
  padding: 28px 24px;
  text-align: center;
}

.immersive-modal-text {
  font-size: 15px;
  color: var(--color-text-primary);
  margin: 0 0 6px;
  line-height: 1.6;
}

.immersive-modal-sub {
  font-size: 13px;
  color: var(--color-text-tertiary);
}

.immersive-modal-btns {
  display: flex;
  gap: 12px;
  margin-top: 22px;
  justify-content: center;
}

.immersive-btn {
  padding: 9px 28px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  border: none;
  transition: all 0.15s;

  &.btn-no {
    background: var(--color-bg-secondary);
    color: var(--color-text-secondary);
    border: 1px solid var(--color-border-hover);

    &:hover {
      background: var(--color-bg-hover);
      color: var(--color-text-primary);
    }
  }

  &.btn-yes {
    background: var(--color-accent);
    color: #fff;

    &:hover {
      background: var(--color-accent-hover);
    }
  }
}

.modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 10000;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(17, 24, 39, 0.3);
  backdrop-filter: blur(4px);
}

.modal-fade-enter-active,
.modal-fade-leave-active {
  transition: opacity 0.2s ease;
}

.modal-fade-enter-from,
.modal-fade-leave-to {
  opacity: 0;
}
</style>
