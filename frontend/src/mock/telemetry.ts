export type MockPoint = { ts: string; value: number }

export type MockSeriesGenerator = {
  getSeries: () => MockPoint[]
  start: () => void
  stop: () => void
  setSeed: (seed: number) => void
  updateParams: (params: Partial<MockGeneratorOptions>) => void
}

export interface MockGeneratorOptions {
  metric: string
  startValue: number
  min: number
  max: number
  noise: number
  trend: number
  intervalMs: number
  maxPoints: number
  seed?: number
  period?: number // 周期性（秒）
}

// Simple seeded random number generator (mulberry32)
class SeededRandom {
  private state: number
  constructor(seed: number = Math.random() * 2**32) {
    this.state = seed >>> 0
  }

  next() {
    let t = this.state += 0x6D2B79F5
    t = Math.imul(t ^ t >>> 15, t | 1)
    t ^= t + Math.imul(t ^ t >>> 7, t | 61)
    return ((t ^ t >>> 14) >>> 0) / 4294967296
  }

  gaussian() {
    // Box-Muller transform for gaussian distribution
    const u1 = this.next()
    const u2 = this.next()
    return Math.sqrt(-2 * Math.log(u1)) * Math.cos(2 * Math.PI * u2)
  }
}

export function createMockSeriesGenerator(opts: MockGeneratorOptions): MockSeriesGenerator {
  const {
    startValue, min, max, noise, trend, intervalMs, maxPoints,
    seed = Date.now(), period = 300 // 5 minutes default period
  } = opts

  let series: MockPoint[] = []
  let value = startValue
  let timer: number | null = null
  let startTime = Date.now()
  let rng = new SeededRandom(seed)

  const clamp = (v: number) => Math.max(min, Math.min(max, v))

  const step = () => {
    const now = Date.now()
    const elapsed = (now - startTime) / 1000 // seconds

    // Trend term: pull towards midpoint
    const mid = (min + max) / 2
    const trendDelta = (mid - value) * trend

    // Periodic component (sinusoidal)
    const periodic = Math.sin(2 * Math.PI * elapsed / period) * 0.1

    // Random gaussian noise
    const noiseDelta = rng.gaussian() * noise

    const delta = trendDelta + periodic + noiseDelta
    value = clamp(value + delta)

    const point: MockPoint = {
      ts: new Date(now).toISOString(),
      value: Number(value.toFixed(4))
    }

    series.push(point)
    if (series.length > maxPoints) {
      series = series.slice(series.length - maxPoints)
    }
  }

  const start = () => {
    if (timer !== null) return
    startTime = Date.now()
    series = [] // reset on restart

    // Prefill history (up to 2 minutes worth)
    const prefillCount = Math.min(Math.floor(120000 / intervalMs), maxPoints)
    for (let i = 0; i < prefillCount; i++) {
      step()
    }

    timer = setInterval(step, intervalMs) as unknown as number
  }

  const stop = () => {
    if (timer !== null) {
      clearInterval(timer)
      timer = null
    }
  }

  const getSeries = () => series.slice()

  const setSeed = (newSeed: number) => {
    rng = new SeededRandom(newSeed)
  }

  const updateParams = (params: Partial<MockGeneratorOptions>) => {
    Object.assign(opts, params)
  }

  return { getSeries, start, stop, setSeed, updateParams }
}


