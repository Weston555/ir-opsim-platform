export interface Robot {
  id: string
  name: string
  model: string
  jointCount: number
  description?: string
  status: RobotStatus
  createdAt: string
  updatedAt: string
}

export type RobotStatus = 'ONLINE' | 'OFFLINE' | 'MAINTENANCE' | 'ERROR'

export interface CreateRobotRequest {
  name: string
  model: string
  jointCount: number
  description?: string
}

export interface UpdateRobotRequest {
  name: string
  model: string
  jointCount: number
  description?: string
}

export interface UpdateRobotStatusRequest {
  status: RobotStatus
}

export interface TelemetryData {
  robotId: string
  jointSamples: Array<{
    jointIndex: number
    currentA: number
    vibrationRms: number
    temperatureC: number
    ts: string
  }>
  poseSample?: {
    x: number
    y: number
    z: number
    rx: number
    ry: number
    rz: number
    ts: string
  }
}