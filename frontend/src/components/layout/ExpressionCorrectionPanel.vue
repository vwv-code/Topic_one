<template>
  <transition name="panel-slide">
    <div v-if="store.expressionCorrectionPanelVisible && store.expressionCorrectionResults.length > 0" class="correction-panel">
      <div class="panel-header">
        <h3 class="panel-title">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M12 20h9"/>
            <path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z"/>
          </svg>
          表达纠错
        </h3>
        <button class="close-btn" @click="store.expressionCorrectionPanelVisible = false">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
          </svg>
        </button>
      </div>

      <div class="panel-body">
        <div class="sentence-list">
          <div
            v-for="(item, idx) in store.expressionCorrectionResults"
            :key="idx"
            class="sentence-item"
          >
            <div class="sentence-header" @click="toggleSentence(idx)">
              <span class="sentence-number">#{{ idx + 1 }}</span>
              <span class="sentence-text">{{ item.originalText }}</span>
              <span class="badge" :class="item.originalText === item.correctedText ? 'badge-ok' : 'badge-fix'">
                {{ item.originalText === item.correctedText ? '正确' : '已纠' }}
              </span>
              <svg
                class="expand-icon"
                :class="{ expanded: expandedSentences.has(idx) }"
                width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
              >
                <polyline points="6 9 12 15 18 9"/>
              </svg>
            </div>

            <transition name="expand">
              <div v-if="expandedSentences.has(idx)" class="sentence-detail">
                <div class="correction-row" v-if="item.originalText !== item.correctedText">
                  <span class="label">纠错后:</span>
                  <span class="corrected-text">{{ item.correctedText }}</span>
                </div>
                <div class="suggestion-row">
                  <span class="label">建议:</span>
                  <p class="suggestion-text">{{ item.suggestion }}</p>
                </div>
              </div>
            </transition>
          </div>
        </div>
      </div>
    </div>
  </transition>
</template>

<script setup lang="ts">
import { reactive } from 'vue'
import { useAppStore } from '@/stores/app'

const store = useAppStore()
const expandedSentences = reactive(new Set<number>())

function toggleSentence(idx: number) {
  if (expandedSentences.has(idx)) {
    expandedSentences.delete(idx)
  } else {
    expandedSentences.add(idx)
  }
}
</script>

<style lang="scss" scoped>
.correction-panel {
  position: fixed;
  right: 0;
  top: 56px;
  bottom: 0;
  width: 380px;
  background: var(--color-bg-primary);
  border-left: 1px solid var(--color-border);
  display: flex;
  flex-direction: column;
  z-index: 101;
  box-shadow: -4px 0 24px rgba(0, 0, 0, 0.06);
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid var(--color-border);
}

.panel-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-primary);
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0;
}

.close-btn {
  width: 28px;
  height: 28px;
  border: none;
  border-radius: 6px;
  background: transparent;
  color: var(--color-text-tertiary);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
  &:hover {
    background: var(--color-bg-hover);
    color: var(--color-text-primary);
  }
}

.panel-body {
  flex: 1;
  overflow-y: auto;
  padding: 16px 20px;
}

.sentence-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.sentence-item {
  border: 1px solid var(--color-border);
  border-radius: 8px;
  overflow: hidden;
}

.sentence-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  cursor: pointer;
  transition: background 0.15s;
  &:hover { background: var(--color-bg-hover); }

  .sentence-number {
    font-size: 11px;
    color: var(--color-text-tertiary);
    font-weight: 500;
  }
  .sentence-text {
    flex: 1;
    font-size: 13px;
    color: var(--color-text-primary);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  .badge {
    font-size: 11px;
    font-weight: 600;
    padding: 2px 8px;
    border-radius: 4px;
    flex-shrink: 0;

    &.badge-ok { background: #ecfdf5; color: #059669; }
    &.badge-fix { background: #fffbeb; color: #d97706; }
  }
  .expand-icon {
    flex-shrink: 0;
    color: var(--color-text-tertiary);
    transition: transform 0.2s;
    &.expanded { transform: rotate(180deg); }
  }
}

.sentence-detail {
  padding: 10px 12px 12px 28px;
  border-top: 1px solid var(--color-border);
  background: var(--color-bg-subtle);
}

.correction-row {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
  font-size: 12px;

  .label {
    color: var(--color-text-secondary);
    flex-shrink: 0;
  }
  .corrected-text {
    color: #059669;
    font-weight: 500;
  }
}

.suggestion-row {
  display: flex;
  gap: 8px;
  font-size: 12px;

  .label {
    color: var(--color-text-secondary);
    flex-shrink: 0;
  }
  .suggestion-text {
    margin: 0;
    color: var(--color-text-primary);
    line-height: 1.5;
  }
}

// 进入/离开动画
.panel-slide-enter-active,
.panel-slide-leave-active {
  transition: transform 0.3s ease, opacity 0.3s ease;
}
.panel-slide-enter-from,
.panel-slide-leave-to {
  transform: translateX(100%);
  opacity: 0;
}

.expand-enter-active,
.expand-leave-active {
  transition: all 0.2s ease;
  overflow: hidden;
}
.expand-enter-from,
.expand-leave-to {
  opacity: 0;
  max-height: 0;
}
.expand-enter-to,
.expand-leave-from {
  opacity: 1;
  max-height: 200px;
}
</style>
