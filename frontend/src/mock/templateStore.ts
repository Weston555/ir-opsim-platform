// frontend/src/mock/templateStore.ts
import type { FaultTemplate } from './faults'

const KEY = 'faultTemplates_v1'

const DEFAULT_TEMPLATES: FaultTemplate[] = [
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

export function loadTemplates(): FaultTemplate[] {
  try {
    const raw = localStorage.getItem(KEY)
    if (raw) {
      const templates = JSON.parse(raw)
      if (Array.isArray(templates) && templates.length > 0) {
        return templates
      }
    }
    // 初始化默认模板
    saveTemplates(DEFAULT_TEMPLATES)
    return DEFAULT_TEMPLATES
  } catch {
    return DEFAULT_TEMPLATES
  }
}

export function saveTemplates(templates: FaultTemplate[]) {
  try {
    localStorage.setItem(KEY, JSON.stringify(templates))
    window.dispatchEvent(new Event('fault-templates-updated'))
  } catch {}
}

export function upsertTemplate(template: FaultTemplate) {
  const templates = loadTemplates()
  const index = templates.findIndex(t => t.id === template.id)

  if (index >= 0) {
    templates[index] = { ...template, updatedAt: new Date().toISOString() }
  } else {
    templates.push({
      ...template,
      id: template.id || `template-${Date.now()}`,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString()
    })
  }

  saveTemplates(templates)
}

export function deleteTemplate(id: string) {
  const templates = loadTemplates().filter(t => t.id !== id)
  saveTemplates(templates)
}

export function resetTemplates() {
  saveTemplates(DEFAULT_TEMPLATES)
}
