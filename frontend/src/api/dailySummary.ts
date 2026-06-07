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
}

export interface EvaluationDetail {
  refText: string
  overallScore: number
  accuracyScore: number
  fluencyScore: number
  integrityScore: number
}

/**
 * 获取用户当天的口语总结
 */
export function getTodaySummary(userId: number) {
  return request.get<any, { code: number; data: DailySummaryResponse }>('/api/daily-summary', {
    params: { userId }
  })
}
