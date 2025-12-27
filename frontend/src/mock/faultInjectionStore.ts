// frontend/src/mock/faultInjectionStore.ts
import { getJSON, setJSON, nowIso, uid } from './storage'
import type { FaultTemplate } from './faultTemplateStore'
import { getAllTemplates } from './faultTemplateStore'

export interface FaultInjection {
  id: string
  runId: string
  robotId: string
  templateId: string
  templateSnapshot: FaultTemplate
  startTs: number
  endTs: number
  createdAt: string
}

const KEY = 'faultInjections_v1'
const EVT = 'faultInjections:changed'

export function getAllInjections(): FaultInjection[] {
  return getJSON<FaultInjection[]>(KEY, [])
}

export function saveAllInjections(list: FaultInjection[]) {
  setJSON(KEY, list)
  window.dispatchEvent(new Event(EVT))
}

export function injectFaults(params: {
  runId: string
  robotId: string
  templateIds: string[]
  intervalSeconds?: number
}) {
  const { runId, robotId, templateIds, intervalSeconds = 0 } = params
  const templates = getAllTemplates()
  const templateMap = new Map(templates.map(t => [t.id, t]))

  const now = Date.now()
  const list = getAllInjections()
  const created: FaultInjection[] = []

  templateIds.forEach((tid, idx) => {
    const tpl = templateMap.get(tid)
    if (!tpl) return
    const start = now + idx * intervalSeconds * 1000
    const end = start + (tpl.durationSeconds ?? 60) * 1000

    created.push({
      id: uid('inj'),
      runId,
      robotId,
      templateId: tid,
      templateSnapshot: { ...tpl }, // 固化参数快照：后续模板被编辑也不影响已注入记录
      startTs: start,
      endTs: end,
      createdAt: nowIso(),
    })
  })

  saveAllInjections([...list, ...created])
  return created
}

export function listInjectionsByRun(runId: string) {
  return getAllInjections()
    .filter(x => x.runId === runId)
    .sort((a, b) => b.createdAt.localeCompare(a.createdAt))
}

export function listActiveInjections(params: { robotId: string; nowTs?: number }) {
  const nowTs = params.nowTs ?? Date.now()
  return getAllInjections().filter(x => x.robotId === params.robotId && nowTs >= x.startTs && nowTs <= x.endTs)
}

export function getInjectionStatus(x: FaultInjection, nowTs = Date.now()) {
  if (nowTs < x.startTs) return 'PENDING'
  if (nowTs > x.endTs) return 'EXPIRED'
  return 'ACTIVE'
}

export function onInjectionsChanged(handler: () => void) {
  window.addEventListener(EVT, handler)
  return () => window.removeEventListener(EVT, handler)
}
