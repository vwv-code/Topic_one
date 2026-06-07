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
        :class="['favorite-btn', { active: store.isFavorited }]"
        @click="store.toggleFavorite()"
      >
        <svg width="16" height="16" viewBox="0 0 24 24" :fill="store.isFavorited ? 'currentColor' : 'none'" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/>
        </svg>
        {{ store.isFavorited ? '已收藏' : '收藏' }}
      </button>
    </div>
  </header>
</template>

<script setup lang="ts">
import { ref, computed, watch, nextTick } from 'vue'
import { useAppStore } from '@/stores/app'

const store = useAppStore()

const titleInputRef = ref<HTMLInputElement | null>(null)
const isEditing = ref(false)
const editingTitle = ref('')

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

.favorite-btn {
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
    color: #f59e0b;

    &:hover {
      background: #fffbeb;
    }
  }
}
</style>
