<template>
  <aside class="sidebar">
    <div class="sidebar-header">
      <div class="logo">
        <div class="logo-icon">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M12 2a3 3 0 0 0-3 3v7a3 3 0 0 0 6 0V5a3 3 0 0 0-3-3Z"/>
            <path d="M19 10v2a7 7 0 0 1-14 0v-2"/>
            <line x1="12" x2="12" y1="19" y2="22"/>
          </svg>
        </div>
        <span class="logo-text">AI 口语陪练</span>
      </div>
      <button class="new-chat-btn" @click="showTitleModal = true">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round">
          <line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/>
        </svg>
        新对话
      </button>
      <button class="daily-summary-btn" @click="showDailySummary = true">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
          <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
          <polyline points="14 2 14 8 20 8"/>
          <line x1="16" y1="13" x2="8" y2="13"/>
          <line x1="16" y1="17" x2="8" y2="17"/>
        </svg>
        每日总结
      </button>
    </div>

    <!-- 新对话标题弹窗 -->
    <Teleport to="body">
      <Transition name="modal-fade">
        <div v-if="showTitleModal" class="modal-overlay" @click.self="showTitleModal = false">
          <div class="title-modal">
            <h3>新建对话</h3>
            <input
              v-model="newChatTitle"
              ref="titleInputRef"
              type="text"
              class="title-input"
              placeholder="请输入对话标题..."
              maxlength="50"
              @keyup.enter="confirmNewChat"
            />
            <div class="title-modal-footer">
              <button class="btn-cancel" @click="showTitleModal = false">取消</button>
              <button
                class="btn-confirm"
                :disabled="!newChatTitle.trim()"
                @click="confirmNewChat"
              >确认</button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <div class="chat-history">
      <div class="history-title">对话历史</div>
      <ul class="history-list">
        <li
          v-for="chat in store.chatHistories"
          :key="chat.id"
          :class="['history-item', { active: chat.isActive }]"
          @click="store.selectChat(chat.id)"
        >
          <span class="item-icon">
            <svg v-if="!chat.isActive" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
              <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
            </svg>
            <svg v-else width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
              <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
            </svg>
          </span>
          <span class="item-text">{{ chat.title }}</span>
          <span v-if="chat.sceneName" class="scene-tag">{{ chat.sceneName }}</span>
          <!-- 删除按钮（hover 显示） -->
          <span
            class="history-delete-btn"
            @click.stop="store.deleteChat(chat.id)"
            title="删除对话"
          >
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round">
              <path d="M18 6L6 18M6 6l12 12"/>
            </svg>
          </span>
        </li>
      </ul>
    </div>

    <div class="user-info" ref="userInfoRef">
      <!-- 弹出菜单 -->
      <Transition name="popup-slide">
        <div v-if="showUserMenu" class="user-popup">
          <button class="popup-item logout" @click="handleLogout">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
              <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/>
              <polyline points="16 17 21 12 16 7"/>
              <line x1="21" y1="12" x2="9" y2="12"/>
            </svg>
            退出登录
          </button>
        </div>
      </Transition>
      <div class="avatar" @click="toggleUserMenu">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
          <path d="M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2"/>
          <circle cx="12" cy="7" r="4"/>
        </svg>
      </div>
      <div class="user-details">
        <div class="username">学习者</div>
        <div class="user-status">连续学习 7 天</div>
      </div>
    </div>

    <!-- 每日总结弹窗 -->
    <DailySummaryModal
      :visible="showDailySummary"
      :user-id="store.userId"
      @close="showDailySummary = false"
    />
  </aside>
</template>

<script setup lang="ts">
import { ref, nextTick, watch, onMounted, onUnmounted } from 'vue'
import { useAppStore } from '@/stores/app'
import DailySummaryModal from '@/components/layout/DailySummaryModal.vue'

const store = useAppStore()

// 新对话标题弹窗
const showTitleModal = ref(false)
const newChatTitle = ref('')
const titleInputRef = ref<HTMLInputElement | null>(null)

watch(showTitleModal, (val) => {
  if (val) {
    newChatTitle.value = ''
    nextTick(() => titleInputRef.value?.focus())
  }
})

async function confirmNewChat() {
  const title = newChatTitle.value.trim()
  if (!title) return
  showTitleModal.value = false
  await store.createNewChat(title)
}

// 用户菜单
const showUserMenu = ref(false)
const showDailySummary = ref(false)
const userInfoRef = ref<HTMLElement | null>(null)

function toggleUserMenu() {
  showUserMenu.value = !showUserMenu.value
}

function handleLogout() {
  showUserMenu.value = false
  localStorage.removeItem('token')
  localStorage.removeItem('userId')
  localStorage.removeItem('username')
  console.log('[退出登录] 用户退出')
  window.location.href = '/login'
}

// 点击外部关闭菜单
function handleClickOutside(e: MouseEvent) {
  if (userInfoRef.value && !userInfoRef.value.contains(e.target as Node)) {
    showUserMenu.value = false
  }
}

onMounted(async () => {
  if (!store.scenesLoaded) await store.fetchScenes()
  await store.fetchConversations()
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})
</script>

<style lang="scss" scoped>
.sidebar {
  width: 260px;
  height: 100vh;
  background: var(--color-bg-primary);
  border-right: 1px solid var(--color-border);
  display: flex;
  flex-direction: column;
  position: fixed;
  left: 0;
  top: 0;
  z-index: 100;
}

.sidebar-header {
  padding: 20px 16px;
  border-bottom: 1px solid var(--color-border);
}

.logo {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 14px;
}

.logo-icon {
  width: 34px;
  height: 34px;
  background: var(--color-accent-light);
  color: var(--color-accent);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.logo-text {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text-primary);
  letter-spacing: -0.01em;
}

.new-chat-btn {
  width: 100%;
  padding: 10px 14px;
  background: var(--color-bg-primary);
  border: 1px dashed var(--color-border-hover);
  border-radius: 8px;
  color: var(--color-text-secondary);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;

  &:hover {
    background: var(--color-accent-subtle);
    border-color: var(--color-accent-muted);
    color: var(--color-accent);
  }
}

.daily-summary-btn {
  width: 100%;
  padding: 9px 14px;
  margin-top: 8px;
  background: var(--color-bg-primary);
  border: 1px dashed var(--color-border-hover);
  border-radius: 8px;
  color: var(--color-text-secondary);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;

  &:hover {
    background: var(--color-accent-subtle);
    border-color: var(--color-accent-muted);
    color: var(--color-accent);
  }
}

.chat-history {
  flex: 1;
  overflow-y: auto;
  padding: 12px 8px;
}

.history-title {
  font-size: 11px;
  font-weight: 600;
  color: var(--color-text-tertiary);
  text-transform: uppercase;
  letter-spacing: 0.05em;
  padding: 8px 12px 6px;
}

.history-list {
  list-style: none;
}

.history-item {
  padding: 9px 12px;
  margin-bottom: 2px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.15s ease;
  display: flex;
  align-items: center;
  gap: 8px;

  .item-icon {
    opacity: 0.4;
    flex-shrink: 0;
    color: var(--color-text-tertiary);
  }

  .item-text {
    font-size: 13.5px;
    color: var(--color-text-secondary);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    flex: 1;
    min-width: 0;
  }

  // 场景名标签
  .scene-tag {
    flex-shrink: 0;
    font-size: 11px;
    padding: 1px 6px;
    border-radius: 4px;
    background: var(--color-bg-tertiary);
    color: var(--color-text-tertiary);
    white-space: nowrap;
  }

  // 历史项删除按钮
  .history-delete-btn {
    margin-left: auto;
    flex-shrink: 0;
    width: 20px;
    height: 20px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 50%;
    color: var(--color-text-tertiary);
    opacity: 0;
    transition: all 0.15s;
    pointer-events: none;

    &:hover {
      background: #fee2e2;
      color: #ef4444;
    }
  }

  &:hover {
    background: var(--color-bg-hover);

    .item-text {
      color: var(--color-text-primary);
    }

    .history-delete-btn {
      opacity: 1;
      pointer-events: auto;
    }
  }

  &.active {
    background: var(--color-accent-light);

    .item-icon {
      opacity: 1;
      color: var(--color-accent);
    }

    .item-text {
      color: var(--color-accent);
      font-weight: 500;
    }
  }
}

.user-info {
  padding: 14px 16px;
  border-top: 1px solid var(--color-border);
  display: flex;
  align-items: center;
  gap: 11px;
  position: relative;
}

.avatar {
  width: 36px;
  height: 36px;
  background: var(--color-bg-tertiary);
  color: var(--color-text-tertiary);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.15s;

  &:hover {
    background: var(--color-accent-light);
    color: var(--color-accent);
  }
}

/* 头像弹出菜单 */
.user-popup {
  position: absolute;
  left: 16px;
  bottom: 62px;
  background: var(--color-bg-primary);
  border: 1px solid var(--color-border);
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  padding: 6px;
  min-width: 180px;
  z-index: 200;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.popup-item {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 10px 12px;
  border: none;
  background: transparent;
  color: var(--color-text-secondary);
  font-size: 13.5px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.12s;

  &:hover {
    background: var(--color-bg-hover);
    color: var(--color-text-primary);
  }

  &.logout {
    &:hover {
      background: #fef2f2;
      color: #ef4444;
    }

    svg {
      opacity: 0.7;
    }
  }
}

/* 弹出动画 */
.popup-slide-enter-active {
  transition: all 0.2s ease;
}

.popup-slide-leave-active {
  transition: all 0.15s ease;
}

.popup-slide-enter-from,
.popup-slide-leave-to {
  opacity: 0;
  transform: translateY(8px) scale(0.96);
}

.user-details {
  flex: 1;
}

.username {
  font-size: 13.5px;
  font-weight: 500;
  color: var(--color-text-primary);
}

.user-status {
  font-size: 12px;
  color: var(--color-text-tertiary);
  margin-top: 1px;
}

/* 新对话标题弹窗 */
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

.title-modal {
  width: 360px;
  max-width: 90vw;
  background: var(--color-bg-primary);
  border: 1px solid var(--color-border);
  border-radius: 14px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.08);
  padding: 24px;

  h3 {
    font-size: 16px;
    font-weight: 600;
    color: var(--color-text-primary);
    margin: 0 0 16px;
  }

  .title-input {
    width: 100%;
    padding: 10px 14px;
    border: 1px solid var(--color-border-hover);
    border-radius: 8px;
    font-size: 14px;
    color: var(--color-text-primary);
    outline: none;
    transition: border-color 0.2s;
    box-sizing: border-box;

    &::placeholder { color: var(--color-text-tertiary); }
    &:focus { border-color: var(--color-accent); }
  }

  .title-modal-footer {
    display: flex;
    justify-content: flex-end;
    gap: 10px;
    margin-top: 16px;
  }

  .btn-cancel,
  .btn-confirm {
    padding: 8px 18px;
    border-radius: 8px;
    font-size: 13.5px;
    cursor: pointer;
    transition: all 0.2s;
  }

  .btn-cancel {
    border: 1px solid var(--color-border);
    background: transparent;
    color: var(--color-text-secondary);

    &:hover { background: var(--color-bg-tertiary); }
  }

  .btn-confirm {
    border: none;
    background: var(--color-accent);
    color: #fff;

    &:hover:not(:disabled) { background: var(--color-accent-hover); }
    &:disabled { opacity: 0.5; cursor: not-allowed; }
  }
}
</style>
