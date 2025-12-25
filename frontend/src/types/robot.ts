// 机器人
export interface Robot {
  id: string
  name: string
  model: string
  jointCount: number
  createdAt: string
}

// 关节样本数据
export interface JointSample {
  id: string
  ts: string
  robot: Robot
  jointIndex: number
  currentA: number
  vibrationRms: number
  temperatureC: number
  scenarioRun?: {
    id: string
    name?: string
  }
  label?: string
}

// 位姿样本数据
export interface PoseSample {
  id: string
  ts: string
  robot: Robot
  x: number
  y: number
  z: number
  rx: number
  ry: number
  rz: number
  scenarioRun?: {
    id: string
    name?: string
  }
  label?: string
}

// 遥测数据
export interface TelemetryData {
  jointSamples: JointSample[]
  poseSample: PoseSample
}
