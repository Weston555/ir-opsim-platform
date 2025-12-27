// frontend/src/mock/faults.ts
export interface ScenarioRun {
  id: string
  scenario: { id?: string; name: string }
  mode: string
  status: string
  rateHz: number
  createdAt: string
  startedAt?: string
  endedAt?: string
  seed?: number
}

export interface FaultTemplate {
  id: string
  name: string
  description: string
  faultType: string
  params: Record<string, any> | string
  durationSeconds: number
  severity: string
  tags: string[]
  enabled: boolean
  createdBy?: string
  createdAt?: string
  updatedAt?: string
}

export interface FaultInjectionRecord {
  id: string
  // 兼容 FaultInjectionPage 现有筛选逻辑：保留 scenarioRun
  scenarioRun: ScenarioRun
  // 为"按机器人注入"新增：robotId 可选
  robotId?: string

  faultType: string
  startTs: string
  endTs: string
  params: any
  createdAt: string

  // 方便回溯/展示：模板信息可选
  templateId?: string
  templateName?: string
}

export const BUILT_IN_FAULT_TEMPLATES: FaultTemplate[] = [
  {
    id: 'builtin-overheat',
    name: '过热示例',
    description: '模拟关节过热故障',
    faultType: 'OVERHEAT',
    params: { amplitude: 10 },
    durationSeconds: 60,
    severity: 'HIGH',
    tags: ['builtin'],
    enabled: true,
    createdBy: 'system',
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString()
  },
  {
    id: 'builtin-vibration',
    name: '高振动示例',
    description: '模拟高振动故障',
    faultType: 'HIGH_VIBRATION',
    params: { amplitude: 0.5 },
    durationSeconds: 120,
    severity: 'MEDIUM',
    tags: ['builtin'],
    enabled: true,
    createdBy: 'system',
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString()
  },
  {
    id: 'builtin-current-spike',
    name: '电流尖峰示例',
    description: '模拟电流突增故障',
    faultType: 'CURRENT_SPIKE',
    params: { amplitude: 3.0 },
    durationSeconds: 30,
    severity: 'HIGH',
    tags: ['builtin'],
    enabled: true,
    createdBy: 'system',
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString()
  },
  {
    id: 'builtin-drift',
    name: '传感器漂移示例',
    description: '模拟传感器线性漂移故障',
    faultType: 'SENSOR_DRIFT',
    params: { drift_rate: 0.001 },
    durationSeconds: 300,
    severity: 'LOW',
    tags: ['builtin'],
    enabled: true,
    createdBy: 'system',
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString()
  }
]

export const BUILT_IN_RUNS: ScenarioRun[] = [
  {
    id: 'mock-run-1',
    scenario: { id: 'mock-scenario-1', name: '论文演示场景' },
    mode: 'REALTIME',
    status: 'RUNNING',
    seed: 1,
    rateHz: 1,
    createdAt: new Date().toISOString()
  }
]

export const MOCK_FAULT_INJECTIONS_KEY = 'mockFaultInjections_v1'
export const MOCK_RUNS_KEY = 'mockSimRuns_v1'

export function readMockRuns(): ScenarioRun[] {
  try {
    const raw = localStorage.getItem(MOCK_RUNS_KEY)
    return raw ? JSON.parse(raw) : []
  } catch {
    return []
  }
}

export function writeMockRuns(runs: ScenarioRun[]) {
  try {
    localStorage.setItem(MOCK_RUNS_KEY, JSON.stringify(runs))
  } catch {}
}

export function ensureDemoRun(): ScenarioRun {
  const runs = readMockRuns()
  const existing = runs.find(r => r.id === BUILT_IN_RUNS[0].id)
  if (existing) return existing
  const next = [BUILT_IN_RUNS[0], ...runs]
  writeMockRuns(next)
  return BUILT_IN_RUNS[0]
}

export function readMockFaultInjections(): FaultInjectionRecord[] {
  try {
    const raw = localStorage.getItem(MOCK_FAULT_INJECTIONS_KEY)
    const arr = raw ? JSON.parse(raw) : []
    return Array.isArray(arr) ? arr : []
  } catch {
    return []
  }
}

export function writeMockFaultInjections(injections: FaultInjectionRecord[]) {
  try {
    localStorage.setItem(MOCK_FAULT_INJECTIONS_KEY, JSON.stringify(injections))
  } catch {}
}

export function appendMockFaultInjections(newOnes: FaultInjectionRecord[]) {
  const cur = readMockFaultInjections()
  cur.push(...newOnes)
  writeMockFaultInjections(cur)
}

// 批量调度：按模板顺序从 startTs 开始注入，每条之间 gapSec 间隔
export function buildBatchFromTemplates(args: {
  run: ScenarioRun
  robotId?: string
  templates: FaultTemplate[]
  startTs: string
  gapSec: number
}): FaultInjectionRecord[] {
  const { run, robotId, templates, startTs, gapSec } = args
  let cursor = new Date(startTs).getTime()

  return templates.map(t => {
    const st = new Date(cursor).toISOString()
    const et = new Date(cursor + t.durationSeconds * 1000).toISOString()
    cursor = cursor + t.durationSeconds * 1000 + gapSec * 1000

    let params: any = t.params
    if (typeof params === 'string') {
      try { params = JSON.parse(params) } catch { params = {} }
    }

    return {
      id: `local-${Date.now()}-${Math.random().toString(16).slice(2)}`,
      scenarioRun: run,
      robotId,
      faultType: t.faultType,
      startTs: st,
      endTs: et,
      params,
      createdAt: new Date().toISOString(),
      templateId: t.id,
      templateName: t.name
    }
  })
}
