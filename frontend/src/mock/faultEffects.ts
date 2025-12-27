// frontend/src/mock/faultEffects.ts
import type { FaultInjection } from './faultInjectionStore'

export interface TelemetryFrame {
  current: number
  vibration: number
  temperature: number
}

function clamp(v: number, min: number, max: number) {
  return Math.max(min, Math.min(max, v))
}

export function applyFaultEffects(base: TelemetryFrame, active: FaultInjection[], nowTs: number): TelemetryFrame {
  let { current, vibration, temperature } = base

  for (const inj of active) {
    const tpl = inj.templateSnapshot
    const dur = Math.max(1, inj.endTs - inj.startTs)
    const t01 = clamp((nowTs - inj.startTs) / dur, 0, 1)

    switch (tpl.faultType) {
      case 'OVERHEAT': {
        const delta = Number(tpl.params?.deltaC ?? 20)
        const ramp = Math.max(1, Number(tpl.params?.rampSeconds ?? 15))
        const k = clamp((nowTs - inj.startTs) / (ramp * 1000), 0, 1)
        temperature += delta * (1 - Math.exp(-3 * k))
        current += 0.6 * k
        break
      }
      case 'HIGH_VIBRATION': {
        const rms = Number(tpl.params?.rmsDelta ?? 0.6)
        vibration += rms
        break
      }
      case 'CURRENT_SPIKE': {
        const amp = Number(tpl.params?.amplitude ?? 6)
        const w = Math.max(0.05, Number(tpl.params?.widthSeconds ?? 6))
        const sigma = w / 6
        // 峰值在中间
        const spike = amp * Math.exp(-Math.pow((t01 - 0.5) / sigma, 2))
        current += spike
        break
      }
      case 'SENSOR_DRIFT': {
        const drift = Number(tpl.params?.driftPerSec ?? 0.02)
        temperature += drift * ((nowTs - inj.startTs) / 1000)
        break
      }
      default:
        // CUSTOM：允许模板 params 写 { temperatureDelta, vibrationDelta, currentDelta }
        temperature += Number(tpl.params?.temperatureDelta ?? 0)
        vibration += Number(tpl.params?.vibrationDelta ?? 0)
        current += Number(tpl.params?.currentDelta ?? 0)
        break
    }
  }

  return {
    current,
    vibration,
    temperature,
  }
}
