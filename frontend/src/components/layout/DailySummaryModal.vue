<template>
  <Teleport to="body">
    <Transition name="modal-fade">
      <div v-if="visible" class="ds-overlay" @click.self="close">
        <div class="ds-modal">
          <!-- 头部 -->
          <div class="ds-header">
            <h2 class="ds-title">每日口语总结</h2>
            <span class="ds-date">{{ summary?.summaryDate || '' }}</span>
            <button class="ds-close" @click="close">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
                <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
              </svg>
            </button>
          </div>

          <!-- 加载中 -->
          <div v-if="loading" class="ds-loading">
            <div class="ds-spinner"></div>
            <p>正在生成总结...</p>
          </div>

          <!-- 内容 -->
          <template v-else-if="summary">
            <!-- 评分仪表盘 -->
            <div class="ds-scoreboard">
              <div class="ds-score-item main-score">
                <svg class="ds-score-ring" viewBox="0 0 100 100">
                  <circle class="ring-bg" cx="50" cy="50" r="42"/>
                  <circle class="ring-fill" cx="50" cy="50" r="42"
                    :stroke-dasharray="ringDash(summary.avgOverallScore)"
                    stroke-dashoffset="0"/>
                </svg>
                <div class="ds-score-inner">
                  <span class="ds-score-val">{{ formatScore(summary.avgOverallScore) }}</span>
                  <span class="ds-score-label">综合分</span>
                </div>
              </div>
              <div class="ds-score-grid">
                <div class="ds-score-cell">
                  <span class="ds-cell-val">{{ formatScore(summary.avgAccuracyScore) }}</span>
                  <span class="ds-cell-label">准确度</span>
                </div>
                <div class="ds-score-cell">
                  <span class="ds-cell-val">{{ formatScore(summary.avgFluencyScore) }}</span>
                  <span class="ds-cell-label">流利度</span>
                </div>
                <div class="ds-score-cell">
                  <span class="ds-cell-val">{{ formatScore(summary.avgIntegrityScore) }}</span>
                  <span class="ds-cell-label">完整度</span>
                </div>
                <div class="ds-score-cell">
                  <span class="ds-cell-val">{{ summary.evalCount }}</span>
                  <span class="ds-cell-label">练习句数</span>
                </div>
              </div>
            </div>

            <!-- LLM 评语 -->
            <div class="ds-comment">
              <div class="ds-comment-label">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
                  <path d="M12 20h9"/><path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z"/>
                </svg>
                AI 口语老师点评
              </div>
              <p class="ds-comment-text">{{ summary.summaryContent }}</p>
            </div>

            <!-- 逐句详情 -->
            <div v-if="summary.details && summary.details.length > 0" class="ds-details">
              <h3 class="ds-details-title">逐句评测详情</h3>
              <div class="ds-detail-list">
                <div
                  v-for="(item, index) in summary.details"
                  :key="index"
                  class="ds-detail-item"
                >
                  <div class="ds-detail-text">
                    <span class="ds-detail-idx">{{ index + 1 }}</span>
                    <span class="ds-detail-content">{{ item.refText }}</span>
                  </div>
                  <div class="ds-detail-bars">
                    <div class="ds-bar-row">
                      <span class="ds-bar-label">综合</span>
                      <div class="ds-bar-track">
                        <div class="ds-bar-fill" :style="{ width: item.overallScore + '%' }"
                          :class="barColorClass(item.overallScore)"></div>
                      </div>
                      <span class="ds-bar-val">{{ formatScore(item.overallScore) }}</span>
                    </div>
                    <div class="ds-bar-row">
                      <span class="ds-bar-label">准确</span>
                      <div class="ds-bar-track">
                        <div class="ds-bar-fill" :style="{ width: item.accuracyScore + '%' }"
                          :class="barColorClass(item.accuracyScore)"></div>
                      </div>
                      <span class="ds-bar-val">{{ formatScore(item.accuracyScore) }}</span>
                    </div>
                    <div class="ds-bar-row">
                      <span class="ds-bar-label">流利</span>
                      <div class="ds-bar-track">
                        <div class="ds-bar-fill" :style="{ width: item.fluencyScore + '%' }"
                          :class="barColorClass(item.fluencyScore)"></div>
                      </div>
                      <span class="ds-bar-val">{{ formatScore(item.fluencyScore) }}</span>
                    </div>
                    <div class="ds-bar-row">
                      <span class="ds-bar-label">完整</span>
                      <div class="ds-bar-track">
                        <div class="ds-bar-fill" :style="{ width: item.integrityScore + '%' }"
                          :class="barColorClass(item.integrityScore)"></div>
                      </div>
                      <span class="ds-bar-val">{{ formatScore(item.integrityScore) }}</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <!-- 表达纠错详情 -->
            <div v-if="summary.correctionDetails && summary.correctionDetails.length > 0" class="ds-corrections">
              <h3 class="ds-details-title">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
                  <path d="M12 20h9"/><path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z"/>
                </svg>
                表达纠错
              </h3>
              <div class="ds-correction-list">
                <div
                  v-for="(item, index) in summary.correctionDetails"
                  :key="index"
                  class="ds-correction-item"
                >
                  <div class="ds-correction-original">
                    <span class="ds-correction-idx">{{ index + 1 }}</span>
                    <span class="ds-correction-text">{{ item.originalText }}</span>
                  </div>
                  <div v-if="item.correctedText && item.correctedText !== item.originalText" class="ds-correction-fixed">
                    <span class="ds-correction-arrow">→</span>
                    <span class="ds-correction-fixed-text">{{ item.correctedText }}</span>
                  </div>
                  <div v-if="item.suggestion" class="ds-correction-suggestion">
                    {{ item.suggestion }}
                  </div>
                </div>
              </div>
            </div>

            <!-- 无数据 -->
            <div v-else class="ds-empty">
              <p>今天还没有练习数据</p>
            </div>
          </template>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { getTodaySummary, type DailySummaryResponse } from '@/api/dailySummary'

const props = defineProps<{
  visible: boolean
  userId: number
}>()

const emit = defineEmits<{
  (e: 'close'): void
}>()

const loading = ref(false)
const summary = ref<DailySummaryResponse | null>(null)

function formatScore(score: number) {
  return Math.round(score * 10) / 10
}

function ringDash(score: number) {
  const circumference = 2 * Math.PI * 42
  const offset = circumference * (1 - score / 100)
  return `${circumference - offset} ${circumference}`
}

function barColorClass(score: number) {
  if (score >= 80) return 'bar-green'
  if (score >= 60) return 'bar-yellow'
  return 'bar-red'
}

function close() {
  emit('close')
}

async function fetchSummary() {
  loading.value = true
  summary.value = null
  try {
    const res = await getTodaySummary(props.userId)
    if (res.code === 200 && res.data) {
      summary.value = res.data
    }
  } catch (e) {
    console.error('获取每日总结失败:', e)
  } finally {
    loading.value = false
  }
}

watch(() => props.visible, (val) => {
  if (val) {
    fetchSummary()
  }
})
</script>

<style lang="scss" scoped>
.ds-overlay {
  position: fixed;
  inset: 0;
  z-index: 10000;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(17, 24, 39, 0.35);
  backdrop-filter: blur(4px);
}

.ds-modal {
  width: 580px;
  max-width: 92vw;
  max-height: 85vh;
  background: var(--color-bg-primary);
  border: 1px solid var(--color-border);
  border-radius: 16px;
  box-shadow: 0 24px 80px rgba(0, 0, 0, 0.12);
  overflow-y: auto;
  display: flex;
  flex-direction: column;
}

.ds-header {
  padding: 20px 24px 16px;
  display: flex;
  align-items: center;
  gap: 10px;
  border-bottom: 1px solid var(--color-border);
  position: sticky;
  top: 0;
  background: var(--color-bg-primary);
  z-index: 2;
}

.ds-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin: 0;
}

.ds-date {
  font-size: 13px;
  color: var(--color-text-tertiary);
}

.ds-close {
  margin-left: auto;
  width: 32px;
  height: 32px;
  border: none;
  background: transparent;
  color: var(--color-text-tertiary);
  cursor: pointer;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s;

  &:hover {
    background: var(--color-bg-hover);
    color: var(--color-text-primary);
  }
}

// 加载
.ds-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 0;
  gap: 16px;

  p {
    color: var(--color-text-tertiary);
    font-size: 14px;
    margin: 0;
  }
}

.ds-spinner {
  width: 32px;
  height: 32px;
  border: 3px solid var(--color-border);
  border-top-color: var(--color-accent);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

// 评分仪表盘
.ds-scoreboard {
  padding: 24px;
  display: flex;
  gap: 24px;
  align-items: center;
  border-bottom: 1px solid var(--color-border);
}

.ds-score-item.main-score {
  width: 110px;
  height: 110px;
  flex-shrink: 0;
  position: relative;
}

.ds-score-ring {
  width: 100%;
  height: 100%;
  transform: rotate(-90deg);

  .ring-bg {
    fill: none;
    stroke: var(--color-border-hover);
    stroke-width: 6;
  }

  .ring-fill {
    fill: none;
    stroke: var(--color-accent);
    stroke-width: 6;
    stroke-linecap: round;
    transition: stroke-dasharray 0.6s ease;
  }
}

.ds-score-inner {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.ds-score-val {
  font-size: 28px;
  font-weight: 700;
  color: var(--color-text-primary);
  line-height: 1;
}

.ds-score-label {
  font-size: 12px;
  color: var(--color-text-tertiary);
  margin-top: 4px;
}

.ds-score-grid {
  flex: 1;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.ds-score-cell {
  padding: 12px;
  background: var(--color-bg-secondary);
  border-radius: 10px;
  text-align: center;

  .ds-cell-val {
    display: block;
    font-size: 22px;
    font-weight: 600;
    color: var(--color-text-primary);
  }

  .ds-cell-label {
    font-size: 12px;
    color: var(--color-text-tertiary);
    margin-top: 2px;
  }
}

// AI 评语
.ds-comment {
  padding: 20px 24px;
  border-bottom: 1px solid var(--color-border);
}

.ds-comment-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 600;
  color: var(--color-accent);
  margin-bottom: 10px;
}

.ds-comment-text {
  font-size: 14px;
  color: var(--color-text-secondary);
  line-height: 1.7;
  margin: 0;
  white-space: pre-line;
}

// 逐句详情
.ds-details {
  padding: 20px 24px;
}

.ds-details-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin: 0 0 14px;
}

.ds-detail-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.ds-detail-item {
  padding: 14px;
  background: var(--color-bg-secondary);
  border-radius: 10px;
}

.ds-detail-text {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-bottom: 10px;
}

.ds-detail-idx {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: var(--color-accent-light);
  color: var(--color-accent);
  font-size: 11px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  margin-top: 1px;
}

.ds-detail-content {
  font-size: 13.5px;
  color: var(--color-text-primary);
  line-height: 1.5;
  word-break: break-all;
}

.ds-detail-bars {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.ds-bar-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.ds-bar-label {
  font-size: 11px;
  color: var(--color-text-tertiary);
  width: 26px;
  flex-shrink: 0;
  text-align: right;
}

.ds-bar-track {
  flex: 1;
  height: 6px;
  background: var(--color-border-hover);
  border-radius: 3px;
  overflow: hidden;
}

.ds-bar-fill {
  height: 100%;
  border-radius: 3px;
  transition: width 0.4s ease;

  &.bar-green { background: #22c55e; }
  &.bar-yellow { background: #eab308; }
  &.bar-red { background: #ef4444; }
}

.ds-bar-val {
  font-size: 11px;
  color: var(--color-text-secondary);
  width: 32px;
  flex-shrink: 0;
}

// 表达纠错
.ds-corrections {
  padding: 20px 24px;
  border-top: 1px solid var(--color-border);
}

.ds-correction-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 14px;
}

.ds-correction-item {
  padding: 14px;
  background: #fdf8f3;
  border: 1px solid #fde68a;
  border-radius: 10px;
}

.ds-correction-original {
  display: flex;
  align-items: flex-start;
  gap: 8px;
}

.ds-correction-idx {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: #fef3c7;
  color: #b45309;
  font-size: 11px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  margin-top: 1px;
}

.ds-correction-text {
  font-size: 13.5px;
  color: var(--color-text-primary);
  line-height: 1.5;
  word-break: break-all;
}

.ds-correction-fixed {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  margin-top: 8px;
  padding-left: 28px;
}

.ds-correction-arrow {
  color: #059669;
  font-weight: 700;
  font-size: 14px;
  flex-shrink: 0;
}

.ds-correction-fixed-text {
  font-size: 13px;
  color: #059669;
  font-weight: 500;
  line-height: 1.5;
  word-break: break-all;
}

.ds-correction-suggestion {
  margin-top: 6px;
  padding-left: 28px;
  font-size: 12px;
  color: var(--color-text-secondary);
  line-height: 1.5;
}

// 空数据
.ds-empty {
  padding: 40px;
  text-align: center;

  p {
    color: var(--color-text-tertiary);
    font-size: 14px;
  }
}

// 过渡动画
.modal-fade-enter-active,
.modal-fade-leave-active {
  transition: opacity 0.2s ease;

  .ds-modal {
    transition: transform 0.2s ease;
  }
}

.modal-fade-enter-from,
.modal-fade-leave-to {
  opacity: 0;

  .ds-modal {
    transform: scale(0.95) translateY(10px);
  }
}
</style>
