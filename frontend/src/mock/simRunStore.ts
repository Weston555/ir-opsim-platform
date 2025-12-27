// frontend/src/mock/simRunStore.ts
import { getJSON, setJSON, nowIso, uid } from './storage'

export interface SimRun {
  id: string
  sceneName: string
  mode: 'REALTIME' | 'BATCH' | string
  status: 'RUNNING' | 'STOPPED' | 'FAILED' | string
  samplingHz: number
  createdAt: string
}

const KEY = 'simRuns_v1'
const EVT = 'simRuns:changed'

export function getAllRuns(): SimRun[] {
  return getJSON<SimRun[]>(KEY, [])
}

export function saveAllRuns(list: SimRun[]) {
  setJSON(KEY, list)
  window.dispatchEvent(new Event(EVT))
}

export function ensureDemoRun(): SimRun {
  const runs = getAllRuns()
  if (runs.length) return runs[0]

  const demo: SimRun = {
    id: `mock-run-${uid('demo')}`,
    sceneName: '论文演示场景',
    mode: 'REALTIME',
    status: 'RUNNING',
    samplingHz: 1,
    createdAt: nowIso(),
  }
  saveAllRuns([demo])
  return demo
}

export function onRunsChanged(handler: () => void) {
  window.addEventListener(EVT, handler)
  return () => window.removeEventListener(EVT, handler)
}
