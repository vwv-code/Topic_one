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
      <button class="new-chat-btn" @click="store.createNewChat()">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round">
          <line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/>
        </svg>
        新对话
      </button>
    </div>

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

    <div class="user-info">
      <div class="avatar">
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
  </aside>
</template>

<script setup lang="ts">
import { useAppStore } from '@/stores/app'
import { onMounted } from 'vue'

const store = useAppStore()

onMounted(() => {
  store.fetchConversations()
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
</style>
