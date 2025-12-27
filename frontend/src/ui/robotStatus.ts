// frontend/src/ui/robotStatus.ts
export type RobotStatus = 'ONLINE' | 'OFFLINE' | 'MAINTENANCE' | string

export function getStatusUI(status: RobotStatus) {
  switch (status) {
    case 'ONLINE':
    case '在线':
      return { text: '在线', type: 'success' }
    case 'MAINTENANCE':
    case '维护':
      return { text: '维护', type: 'warning' }
    case 'OFFLINE':
    case '离线':
      return { text: '离线', type: 'info' }
    default:
      return { text: status || '未知', type: 'default' }
  }
}
