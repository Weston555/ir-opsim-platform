// frontend/src/mock/telemetryMode.ts
const KEY = 'robotTelemetryMode_v1'

export function getRobotTelemetryMode(robotId: string): 'real' | 'mock' {
  try {
    const raw = localStorage.getItem(KEY)
    const map = raw ? JSON.parse(raw) : {}
    return map?.[robotId] === 'real' ? 'real' : 'mock' // 默认 mock（论文最稳）
  } catch {
    return 'mock'
  }
}

export function setRobotTelemetryMode(robotId: string, mode: 'real' | 'mock') {
  try {
    const raw = localStorage.getItem(KEY)
    const map = raw ? JSON.parse(raw) : {}
    map[robotId] = mode
    localStorage.setItem(KEY, JSON.stringify(map))
  } catch {}
}
