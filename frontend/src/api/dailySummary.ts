import request from '@/api/request'

/** 每日总结响应 */
export interface DailySummaryResponse {
  summaryDate: string
  evalCount: number
  avgOverallScore: number
  avgAccuracyScore: number
  avgFluencyScore: number
  avgIntegrityScore: number
  summaryContent: string
  details: EvaluationDetail[]
  correctionDetails: CorrectionDetail[]
}

export interface EvaluationDetail {
  refText: string
  overallScore: number
  accuracyScore: number
  fluencyScore: number
  integrityScore: number
}

export interface CorrectionDetail {
  originalText: string
  correctedText: string
  suggestion: string
}

/** 成长记录响应 */
export interface GrowthRecordResponse {
  totalDays: number
  totalSentences: number
  dataPoints: GrowthDataPoint[]
}

export interface GrowthDataPoint {
  date: string
  evalCount: number
  avgOverallScore: number
  avgAccuracyScore: number
  avgFluencyScore: number
  avgIntegrityScore: number
}

/**
 * 获取用户当天的口语总结
 */
export function getTodaySummary(userId: number) {
  return request.get<any, { code: number; data: DailySummaryResponse }>('/api/daily-summary', {
    params: { userId }
  })
}

/**
 * 获取用户的成长记录（全部日期的数据）
 */
export function getGrowthRecord(userId: number) {
  return request.get<any, { code: number; data: GrowthRecordResponse }>('/api/daily-summary/history', {
    params: { userId }
  })
}
