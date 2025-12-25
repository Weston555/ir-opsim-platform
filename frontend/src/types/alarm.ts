// 告警事件
export interface AlarmEvent {
  id: string
  firstSeenTs: string
  lastSeenTs: string
  robot: {
    id: string
    name: string
    model: string
  }
  jointIndex?: number
  alarmType: 'TEMP_ANOMALY' | 'VIB_ANOMALY' | 'CURRENT_ANOMALY' | 'POSE_ANOMALY'
  severity: 'INFO' | 'WARN' | 'CRITICAL'
  status: 'OPEN' | 'ACKED' | 'CLOSED'
  dedupKey: string
  count: number
  detector: string
  score: number
  evidence?: any
  scenarioRun?: {
    id: string
    name?: string
  }
  createdAt: string
  updatedAt: string
}

// 告警查询参数
export interface AlarmQuery {
  page?: number
  size?: number
  status?: 'OPEN' | 'ACKED' | 'CLOSED'
  severity?: 'INFO' | 'WARN' | 'CRITICAL'
  robotId?: string
  from?: Date
  to?: Date
}

// 告警建议
export interface AlarmRecommendation {
  matchedRules: MatchedRule[]
  matchedCases: MatchedCase[]
  explanation: string
}

// 匹配的规则
export interface MatchedRule {
  rule: {
    id: string
    name: string
    priority: number
  }
  reason: string
}

// 匹配的案例
export interface MatchedCase {
  kbCase: {
    id: string
    title: string
    faultType: string
    rootCause: string
  }
  reason: string
}
