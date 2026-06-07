<template>
  <transition name="panel-slide">
    <div v-if="store.pronunciationPanelVisible && store.pronunciationResults.length > 0" class="pronunciation-panel">
      <div class="panel-header">
        <h3 class="panel-title">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M12 2L2 7l10 5 10-5-10-5z"/>
            <path d="M2 17l10 5 10-5"/>
            <path d="M2 12l10 5 10-5"/>
          </svg>
          发音评测结果
        </h3>
        <button class="close-btn" @click="store.pronunciationPanelVisible = false">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
          </svg>
        </button>
      </div>

      <div class="panel-body">
        <!-- 综合评分 -->
        <div class="overall-score">
          <div class="score-circle" :class="scoreLevel(avgOverall)">
            <span class="score-value">{{ avgOverall }}</span>
            <span class="score-label">综合</span>
          </div>
          <div class="score-details">
            <div class="score-row">
              <span class="label">发音准确度</span>
              <div class="bar-wrap"><div class="bar" :style="{ width: avgAccuracy + '%' }" :class="barColor(avgAccuracy)"></div></div>
              <span class="value">{{ avgAccuracy }}</span>
            </div>
            <div class="score-row">
              <span class="label">流利度</span>
              <div class="bar-wrap"><div class="bar" :style="{ width: avgFluency + '%' }" :class="barColor(avgFluency)"></div></div>
              <span class="value">{{ avgFluency }}</span>
            </div>
            <div class="score-row">
              <span class="label">完整度</span>
              <div class="bar-wrap"><div class="bar" :style="{ width: avgIntegrity + '%' }" :class="barColor(avgIntegrity)"></div></div>
              <span class="value">{{ avgIntegrity }}</span>
            </div>
          </div>
        </div>

        <!-- 逐句详情 -->
        <div class="sentence-list">
          <div
            v-for="(item, idx) in store.pronunciationResults"
            :key="idx"
            class="sentence-item"
          >
            <div class="sentence-header" @click="toggleSentence(idx)">
              <span class="sentence-number">#{{ idx + 1 }}</span>
              <span class="sentence-text">{{ item.refText }}</span>
              <span class="sentence-score" :class="scoreLevel(item.overallScore)">{{ Math.round(item.overallScore) }}</span>
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
                <!-- 句子维度 -->
                <div class="dimension-tags" v-if="item.sentenceDetail">
                  <span class="tag">重音 {{ item.sentenceDetail.stressScore ?? '-' }}</span>
                  <span class="tag">语调 {{ item.sentenceDetail.toneScore ?? '-' }}</span>
                  <span class="tag">意群 {{ item.sentenceDetail.senseScore ?? '-' }}</span>
                  <span class="tag">语速 {{ speedLabel(item.speed) }}</span>
                </div>

                <!-- 单词评分 -->
                <div class="word-list" v-if="item.wordDetails && item.wordDetails.length > 0">
                  <div
                    v-for="word in item.wordDetails"
                    :key="word.word + word.startMs"
                    class="word-item"
                    :class="{ 'low-score': word.score < 60 }"
                  >
                    <span class="word-text">{{ word.word }}</span>
                    <span class="word-score">{{ Math.round(word.score) }}</span>

                    <!-- 音素详情（错误时高亮） -->
                    <span class="phoneme-list" v-if="word.phonemes && word.phonemes.length > 0">
                      <span
                        v-for="ph in word.phonemes"
                        :key="ph.phoneme"
                        class="phoneme-chip"
                        :class="{ error: ph.hasError, good: ph.score >= 80 && !ph.hasError }"
                      >
                        {{ ph.phoneme }}
                      </span>
                    </span>
                  </div>
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
import { computed, reactive } from 'vue'
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

const avgOverall = computed(() => {
  const results = store.pronunciationResults
  if (!results.length) return 0
  const sum = results.reduce((a, r) => a + r.overallScore, 0)
  return Math.round(sum / results.length)
})

const avgAccuracy = computed(() => {
  const results = store.pronunciationResults
  if (!results.length) return 0
  const sum = results.reduce((a, r) => a + (r.accuracyScore || 0), 0)
  return Math.round(sum / results.length)
})

const avgFluency = computed(() => {
  const results = store.pronunciationResults
  if (!results.length) return 0
  const sum = results.reduce((a, r) => a + (r.fluencyScore || 0), 0)
  return Math.round(sum / results.length)
})

const avgIntegrity = computed(() => {
  const results = store.pronunciationResults
  if (!results.length) return 0
  const sum = results.reduce((a, r) => a + (r.integrityScore || 0), 0)
  return Math.round(sum / results.length)
})

function scoreLevel(score: number): string {
  if (score >= 80) return 'good'
  if (score >= 60) return 'medium'
  return 'poor'
}

function barColor(score: number): string {
  if (score >= 80) return 'bar-good'
  if (score >= 60) return 'bar-medium'
  return 'bar-poor'
}

function speedLabel(speed: number): string {
  if (speed === 0) return '慢'
  if (speed === 2) return '快'
  return '正常'
}
</script>

<style lang="scss" scoped>
.pronunciation-panel {
  position: fixed;
  right: 0;
  top: 56px;
  bottom: 0;
  width: 380px;
  background: var(--color-bg-primary);
  border-left: 1px solid var(--color-border);
  display: flex;
  flex-direction: column;
  z-index: 100;
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

// ===== 综合评分 =====
.overall-score {
  display: flex;
  gap: 20px;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--color-border);
}

.score-circle {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;

  &.good { background: #ecfdf5; color: #059669; }
  &.medium { background: #fffbeb; color: #d97706; }
  &.poor { background: #fef2f2; color: #dc2626; }

  .score-value {
    font-size: 22px;
    font-weight: 700;
  }
  .score-label {
    font-size: 11px;
    opacity: 0.7;
  }
}

.score-details {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.score-row {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;

  .label {
    width: 56px;
    color: var(--color-text-secondary);
    flex-shrink: 0;
  }
  .bar-wrap {
    flex: 1;
    height: 6px;
    background: var(--color-bg-hover);
    border-radius: 3px;
    overflow: hidden;
  }
  .bar {
    height: 100%;
    border-radius: 3px;
    transition: width 0.5s ease;

    &.bar-good { background: #10b981; }
    &.bar-medium { background: #f59e0b; }
    &.bar-poor { background: #ef4444; }
  }
  .value {
    width: 26px;
    text-align: right;
    font-weight: 600;
    color: var(--color-text-primary);
  }
}

// ===== 逐句列表 =====
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
  .sentence-score {
    font-size: 13px;
    font-weight: 700;
    padding: 2px 8px;
    border-radius: 4px;

    &.good { background: #ecfdf5; color: #059669; }
    &.medium { background: #fffbeb; color: #d97706; }
    &.poor { background: #fef2f2; color: #dc2626; }
  }
  .expand-icon {
    color: var(--color-text-tertiary);
    transition: transform 0.2s;
    &.expanded { transform: rotate(180deg); }
  }
}

.sentence-detail {
  padding: 8px 12px 12px;
  border-top: 1px solid var(--color-border);
  background: var(--color-bg-secondary);
}

.dimension-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-bottom: 10px;
}

.tag {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 4px;
  background: var(--color-bg-primary);
  color: var(--color-text-secondary);
  border: 1px solid var(--color-border);
}

.word-list {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.word-item {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 12px;
  background: var(--color-bg-primary);
  border: 1px solid var(--color-border);

  &.low-score {
    border-color: #fecaca;
    background: #fef2f2;
  }

  .word-text {
    font-weight: 500;
    color: var(--color-text-primary);
  }
  .word-score {
    font-weight: 600;
    font-size: 11px;
    color: var(--color-text-secondary);
  }
}

.phoneme-list {
  display: flex;
  gap: 2px;
  margin-left: 4px;
}

.phoneme-chip {
  padding: 0 3px;
  border-radius: 3px;
  font-size: 11px;
  color: var(--color-text-tertiary);
  background: rgba(0,0,0,0.04);

  &.error {
    background: #fee2e2;
    color: #dc2626;
    font-weight: 600;
  }
  &.good {
    background: #d1fae5;
    color: #059669;
  }
}

// ===== 动画 =====
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
  transition: all 0.25s ease;
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
  max-height: 600px;
}
</style>
