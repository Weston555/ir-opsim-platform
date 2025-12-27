// frontend/src/mock/faultTemplateStore.ts
import { getJSON, setJSON, nowIso, uid } from './storage'

export type Severity = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL'

export interface FaultTemplate {
  id: string
  name: string
  faultType: string
  severity: Severity
  durationSeconds: number
  enabled: boolean
  params: Record<string, any>
  createdAt: string
  updatedAt: string
  builtin?: boolean
}

const KEY = 'faultTemplates_v1'
const EVT = 'faultTemplates:changed'

export const BUILTIN_TEMPLATES: FaultTemplate[] = [
  {
    id: 'builtin-overheat-high',
    name: '过热示例',
    faultType: 'OVERHEAT',
    severity: 'HIGH',
    durationSeconds: 120,
    enabled: true,
    params: { deltaC: 25, rampSeconds: 20 },
    createdAt: nowIso(),
    updatedAt: nowIso(),
    builtin: true,
  },
  {
    id: 'builtin-high-vibration-medium',
    name: '高振动示例',
    faultType: 'HIGH_VIBRATION',
    severity: 'MEDIUM',
    durationSeconds: 120,
    enabled: true,
    params: { rmsDelta: 0.8 },
    createdAt: nowIso(),
    updatedAt: nowIso(),
    builtin: true,
  },
  {
    id: 'builtin-current-spike-high',
    name: '电流尖峰示例',
    faultType: 'CURRENT_SPIKE',
    severity: 'HIGH',
    durationSeconds: 60,
    enabled: true,
    params: { amplitude: 8, widthSeconds: 8 },
    createdAt: nowIso(),
    updatedAt: nowIso(),
    builtin: true,
  },
  {
    id: '传感器漂移示例',
    name: '传感器漂移示例',
    faultType: 'SENSOR_DRIFT',
    severity: 'LOW',
    durationSeconds: 180,
    enabled: true,
    params: { driftPerSec: 0.03 },
    createdAt: nowIso(),
    updatedAt: nowIso(),
    builtin: true,
  },
]

function normalize(list: FaultTemplate[]): FaultTemplate[] {
  // 去重（以 id）
  const map = new Map<string, FaultTemplate>()
  for (const t of list) map.set(t.id, t)
  return Array.from(map.values()).sort((a, b) => (a.builtin === b.builtin ? 0 : a.builtin ? -1 : 1))
}

export function getAllTemplates(): FaultTemplate[] {
  const saved = getJSON<FaultTemplate[]>(KEY, [])
  // 首次初始化：合并内置模板
  if (!saved.length) {
    const merged = normalize(BUILTIN_TEMPLATES)
    setJSON(KEY, merged)
    return merged
  }
  // 确保内置模板一直存在（用户误删也能自动补回）
  const merged = normalize([...BUILTIN_TEMPLATES, ...saved])
  if (merged.length !== saved.length) setJSON(KEY, merged)
  return merged
}

export function saveAllTemplates(list: FaultTemplate[]) {
  setJSON(KEY, normalize(list))
  window.dispatchEvent(new Event(EVT))
}

export function upsertTemplate(patch: Partial<FaultTemplate> & { id?: string }) {
  const list = getAllTemplates()
  const now = nowIso()

  if (patch.id) {
    const idx = list.findIndex(t => t.id === patch.id)
    if (idx >= 0) {
      list[idx] = { ...list[idx], ...patch, updatedAt: now }
      saveAllTemplates(list)
      return list[idx]
    }
  }

  const created: FaultTemplate = {
    id: patch.id ?? uid('tpl'),
    name: patch.name ?? '新建模板',
    faultType: patch.faultType ?? 'CUSTOM',
    severity: patch.severity ?? 'LOW',
    durationSeconds: patch.durationSeconds ?? 60,
    enabled: patch.enabled ?? true,
    params: patch.params ?? {},
    createdAt: now,
    updatedAt: now,
    builtin: false,
  }
  saveAllTemplates([...list, created])
  return created
}

export function deleteTemplate(id: string) {
  const list = getAllTemplates()
  // 内置模板建议不允许删除（只允许禁用），防止你论文演示时"空"
  const t = list.find(x => x.id === id)
  if (t?.builtin) {
    upsertTemplate({ id, enabled: false })
    return
  }
  saveAllTemplates(list.filter(t => t.id !== id))
}

export function onTemplatesChanged(handler: () => void) {
  window.addEventListener(EVT, handler)
  return () => window.removeEventListener(EVT, handler)
}
