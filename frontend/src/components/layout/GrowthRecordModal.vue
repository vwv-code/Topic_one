<template>
  <Teleport to="body">
    <Transition name="modal-fade">
      <div v-if="visible" class="gr-overlay" @click.self="close">
        <div class="gr-modal">
          <!-- 头部 -->
          <div class="gr-header">
            <h2 class="gr-title">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <polyline points="22 12 18 12 15 21 9 3 6 12 2 12"/>
              </svg>
              成长记录
            </h2>
            <button class="gr-close" @click="close">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
                <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
              </svg>
            </button>
          </div>

          <!-- 加载 -->
          <div v-if="loading" class="gr-loading">
            <div class="gr-spinner"></div>
            <p>加载中...</p>
          </div>

          <!-- 无数据 -->
          <div v-else-if="!record || record.dataPoints.length === 0" class="gr-empty">
            <p>还没有练习记录，快去练一练吧！</p>
          </div>

          <!-- 内容 -->
          <template v-else>
            <!-- 汇总统计 -->
            <div class="gr-summary">
              <div class="gr-stat">
                <span class="gr-stat-val">{{ record.totalDays }}</span>
                <span class="gr-stat-label">练习天数</span>
              </div>
              <div class="gr-stat">
                <span class="gr-stat-val">{{ record.totalSentences }}</span>
                <span class="gr-stat-label">练习句数</span>
              </div>
              <div class="gr-stat">
                <span class="gr-stat-val">{{ formatScore(latestOverall) }}</span>
                <span class="gr-stat-label">最新综合分</span>
              </div>
              <div class="gr-stat">
                <span class="gr-stat-val">{{ trendLabel }}</span>
                <span class="gr-stat-label">趋势</span>
              </div>
            </div>

            <!-- 趋势图 -->
            <div class="gr-chart-section">
              <div class="gr-chart-legend">
                <span class="legend-item"><span class="legend-dot overall"></span>综合</span>
                <span class="legend-item"><span class="legend-dot accuracy"></span>准确度</span>
                <span class="legend-item"><span class="legend-dot fluency"></span>流利度</span>
                <span class="legend-item"><span class="legend-dot integrity"></span>完整度</span>
              </div>
              <svg class="gr-chart" :viewBox="`0 0 ${chartWidth} ${chartHeight}`" preserveAspectRatio="xMidYMid meet">
                <!-- 网格线 -->
                <line
                  v-for="i in 5"
                  :key="'grid-' + i"
                  :x1="paddingLeft" :y1="y(i * 20)" :x2="chartWidth - paddingRight" :y2="y(i * 20)"
                  stroke="var(--color-border)" stroke-dasharray="4 3" stroke-width="0.5"
                />
                <!-- Y轴标签 -->
                <text
                  v-for="i in 5"
                  :key="'ylabel-' + i"
                  :x="paddingLeft - 6" :y="y(i * 20) + 4"
                  text-anchor="end" font-size="9" fill="var(--color-text-tertiary)"
                >{{ i * 20 }}</text>

                <!-- X轴标签（日期） -->
                <text
                  v-for="(dp, i) in record.dataPoints"
                  :key="'xlabel-' + i"
                  :x="x(i)" :y="chartHeight - 4"
                  text-anchor="middle" font-size="9"
                  :fill="i === selectedIndex ? 'var(--color-accent)' : 'var(--color-text-tertiary)'"
                  :font-weight="i === selectedIndex ? 600 : 400"
                  class="x-label"
                >{{ formatDate(dp.date) }}</text>

                <!-- 折线：准确度 -->
                <polyline
                  :points="linePoints('avgAccuracyScore')"
                  fill="none" stroke="#60a5fa" stroke-width="1.8" stroke-linejoin="round" stroke-linecap="round"
                />
                <!-- 折线：流利度 -->
                <polyline
                  :points="linePoints('avgFluencyScore')"
                  fill="none" stroke="#f59e0b" stroke-width="1.8" stroke-linejoin="round" stroke-linecap="round"
                />
                <!-- 折线：完整度 -->
                <polyline
                  :points="linePoints('avgIntegrityScore')"
                  fill="none" stroke="#a78bfa" stroke-width="1.8" stroke-linejoin="round" stroke-linecap="round"
                />
                <!-- 折线：综合（加粗高亮） -->
                <polyline
                  :points="linePoints('avgOverallScore')"
                  fill="none" stroke="var(--color-accent)" stroke-width="2.5" stroke-linejoin="round" stroke-linecap="round"
                />

                <!-- 数据点 -->
                <template v-for="(dp, i) in record.dataPoints" :key="'dots-' + i">
                  <circle :cx="x(i)" :cy="y(dp.avgOverallScore)" r="3.5" fill="var(--color-accent)" stroke="#fff" stroke-width="1.5" />
                  <circle :cx="x(i)" :cy="y(dp.avgAccuracyScore)" r="2" fill="#60a5fa" />
                  <circle :cx="x(i)" :cy="y(dp.avgFluencyScore)" r="2" fill="#f59e0b" />
                  <circle :cx="x(i)" :cy="y(dp.avgIntegrityScore)" r="2" fill="#a78bfa" />
                </template>
              </svg>
            </div>

            <!-- 每日详情列表 -->
            <div class="gr-detail-list">
              <h3 class="gr-detail-title">每日详情</h3>
              <div
                v-for="(dp, i) in record.dataPoints"
                :key="i"
                class="gr-detail-item"
              >
                <div class="gr-detail-date">{{ dp.date }}</div>
                <div class="gr-detail-scores">
                  <span class="gr-detail-score overall">{{ formatScore(dp.avgOverallScore) }}</span>
                  <span class="gr-detail-score accuracy">{{ formatScore(dp.avgAccuracyScore) }}</span>
                  <span class="gr-detail-score fluency">{{ formatScore(dp.avgFluencyScore) }}</span>
                  <span class="gr-detail-score integrity">{{ formatScore(dp.avgIntegrityScore) }}</span>
                  <span class="gr-detail-count">{{ dp.evalCount }}句</span>
                </div>
              </div>
            </div>
          </template>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { getGrowthRecord, type GrowthRecordResponse } from '@/api/dailySummary'

const props = defineProps<{
  visible: boolean
  userId: number
}>()

const emit = defineEmits<{
  (e: 'close'): void
}>()

const loading = ref(false)
const record = ref<GrowthRecordResponse | null>(null)
const selectedIndex = ref(-1)

// 图表参数
const paddingLeft = 38
const paddingRight = 10
const paddingBottom = 22
const chartHeight = 240
const chartWidth = 600

const latestOverall = computed(() => {
  if (!record.value || record.value.dataPoints.length === 0) return 0
  return record.value.dataPoints[record.value.dataPoints.length - 1].avgOverallScore
})

const trendLabel = computed(() => {
  if (!record.value || record.value.dataPoints.length < 2) return '-'
  const pts = record.value.dataPoints
  const first = pts[0].avgOverallScore
  const last = pts[pts.length - 1].avgOverallScore
  const diff = last - first
  if (diff > 2) return '↑ 上升'
  if (diff < -2) return '↓ 下降'
  return '→ 平稳'
})

function formatScore(score: number) {
  return Math.round(score * 10) / 10
}

function formatDate(dateStr: string) {
  const parts = dateStr.split('-')
  return parts.length >= 3 ? `${parts[1]}/${parts[2]}` : dateStr
}

// 计算 X 坐标
function x(index: number): number {
  if (!record.value || record.value.dataPoints.length <= 1) {
    return paddingLeft + (chartWidth - paddingLeft - paddingRight) / 2
  }
  const count = record.value.dataPoints.length
  return paddingLeft + (index / (count - 1)) * (chartWidth - paddingLeft - paddingRight)
}

// 计算 Y 坐标（分数 0-100 → 像素）
function y(score: number | undefined): number {
  const s = score ?? 0
  const topY = 24
  const bottomY = chartHeight - paddingBottom
  return bottomY - (s / 100) * (bottomY - topY)
}

// 生成折线 points 字符串
function linePoints(field: keyof typeof record.value['dataPoints'][0]): string {
  if (!record.value) return ''
  return record.value.dataPoints
    .map((dp, i) => `${x(i)},${y(dp[field] as number)}`)
    .join(' ')
}

function close() {
  emit('close')
}

async function fetchGrowthRecord() {
  loading.value = true
  record.value = null
  try {
    const res = await getGrowthRecord(props.userId)
    if (res.code === 200 && res.data) {
      record.value = res.data
    }
  } catch (e) {
    console.error('获取成长记录失败:', e)
  } finally {
    loading.value = false
  }
}

watch(() => props.visible, (val) => {
  if (val) {
    fetchGrowthRecord()
  }
})
</script>

<style lang="scss" scoped>
.gr-overlay {
  position: fixed;
  inset: 0;
  z-index: 10000;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(17, 24, 39, 0.35);
  backdrop-filter: blur(4px);
}

.gr-modal {
  width: 680px;
  max-width: 94vw;
  max-height: 88vh;
  background: var(--color-bg-primary);
  border: 1px solid var(--color-border);
  border-radius: 16px;
  box-shadow: 0 24px 80px rgba(0, 0, 0, 0.12);
  overflow-y: auto;
  display: flex;
  flex-direction: column;
}

.gr-header {
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

.gr-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--color-text-primary);
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0;
}

.gr-close {
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

.gr-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 60px 0;
  gap: 16px;
  p { color: var(--color-text-tertiary); font-size: 14px; margin: 0; }
}

.gr-spinner {
  width: 32px;
  height: 32px;
  border: 3px solid var(--color-border);
  border-top-color: var(--color-accent);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin { to { transform: rotate(360deg); } }

.gr-empty {
  padding: 60px 0;
  text-align: center;
  p { color: var(--color-text-tertiary); font-size: 14px; }
}

// 汇总统计
.gr-summary {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr 1fr;
  gap: 12px;
  padding: 20px 24px;
  border-bottom: 1px solid var(--color-border);
}

.gr-stat {
  text-align: center;
  padding: 12px 8px;
  background: var(--color-bg-secondary);
  border-radius: 10px;
}

.gr-stat-val {
  display: block;
  font-size: 22px;
  font-weight: 700;
  color: var(--color-text-primary);
}

.gr-stat-label {
  font-size: 11px;
  color: var(--color-text-tertiary);
  margin-top: 2px;
}

// 图表
.gr-chart-section {
  padding: 16px 24px 0;
  border-bottom: 1px solid var(--color-border);
}

.gr-chart-legend {
  display: flex;
  gap: 16px;
  margin-bottom: 8px;
  justify-content: center;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  color: var(--color-text-secondary);
}

.legend-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  display: inline-block;
  &.overall { background: var(--color-accent); }
  &.accuracy { background: #60a5fa; }
  &.fluency { background: #f59e0b; }
  &.integrity { background: #a78bfa; }
}

.gr-chart {
  width: 100%;
  height: auto;
}

// 每日详情
.gr-detail-list {
  padding: 16px 24px 24px;
}

.gr-detail-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin: 0 0 12px;
}

.gr-detail-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 14px;
  border-radius: 8px;
  &:nth-child(even) { background: var(--color-bg-secondary); }
}

.gr-detail-date {
  font-size: 13px;
  color: var(--color-text-primary);
  font-weight: 500;
}

.gr-detail-scores {
  display: flex;
  gap: 12px;
  align-items: center;
}

.gr-detail-score {
  font-size: 12px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 4px;
  &.overall { background: #e0f2fe; color: #0284c7; }
  &.accuracy { background: #eff6ff; color: #2563eb; }
  &.fluency { background: #fffbeb; color: #d97706; }
  &.integrity { background: #f5f3ff; color: #7c3aed; }
}

.gr-detail-count {
  font-size: 11px;
  color: var(--color-text-tertiary);
}

// 过渡动画
.modal-fade-enter-active,
.modal-fade-leave-active {
  transition: opacity 0.2s ease;
  .gr-modal { transition: transform 0.2s ease; }
}

.modal-fade-enter-from,
.modal-fade-leave-to {
  opacity: 0;
  .gr-modal { transform: scale(0.95) translateY(10px); }
}
</style>
