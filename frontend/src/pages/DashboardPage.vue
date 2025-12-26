<template>
  <div class="dashboard-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <h1>运维监控中心</h1>
      <div class="header-actions">
        <el-button
          v-if="authStore.hasRole('ADMIN')"
          type="info"
          size="small"
          @click="$router.push('/robots-management')"
        >
          <el-icon><Setting /></el-icon>
          机器人管理
        </el-button>
        <el-button
          v-if="authStore.hasAnyRole(['ADMIN', 'OPERATOR'])"
          type="primary"
          size="small"
          @click="$router.push('/fault-injection')"
        >
          <el-icon><Setting /></el-icon>
          故障注入控制台
        </el-button>
        <div class="user-info">
          <span>欢迎，{{ authStore.user?.username }}</span>
          <el-button type="text" @click="handleLogout">登出</el-button>
        </div>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-grid">
      <el-card class="stat-card">
        <div class="stat-content">
          <div class="stat-icon">
            <el-icon size="32" color="#409EFF"><Robot /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-number">{{ robots.length }}</div>
            <div class="stat-label">在线机器人</div>
          </div>
        </div>
      </el-card>

      <el-card class="stat-card">
        <div class="stat-content">
          <div class="stat-icon">
            <el-icon size="32" color="#F56C6C"><Warning /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-number">{{ alarmStore.openAlarmCount }}</div>
            <div class="stat-label">活跃告警</div>
          </div>
        </div>
      </el-card>

      <el-card class="stat-card">
        <div class="stat-content">
          <div class="stat-icon">
            <el-icon size="32" color="#E6A23C"><Bell /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-number">{{ alarmStore.warningAlarms.length }}</div>
            <div class="stat-label">警告告警</div>
          </div>
        </div>
      </el-card>

      <el-card class="stat-card">
        <div class="stat-content">
          <div class="stat-icon">
            <el-icon size="32" color="#F56C6C"><Error /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-number">{{ alarmStore.criticalAlarms.length }}</div>
            <div class="stat-label">严重告警</div>
          </div>
        </div>
      </el-card>
    </div>

    <!-- 主要内容区域 -->
    <div class="main-content">
      <!-- 机器人列表 -->
      <el-card class="robots-section">
        <template #header>
          <div class="section-header">
            <span>机器人状态</span>
            <el-button type="primary" size="small" @click="refreshRobots">
              <el-icon><Refresh /></el-icon>
              刷新
            </el-button>
          </div>
        </template>

        <div v-if="robotsLoading" class="loading">
          <el-icon class="is-loading">
            <Loading />
          </el-icon>
          加载中...
        </div>

        <div v-else-if="robots.length === 0" class="empty-state">
          <el-empty description="暂无机器人数据" />
        </div>

        <div v-else class="robots-grid">
          <el-card
            v-for="robot in robots"
            :key="robot.id"
            class="robot-card"
          >
            <div class="robot-header">
              <h3>{{ robot.name }}</h3>
              <el-tag>{{ robot.model }}</el-tag>
            </div>
            <div class="robot-info">
              <span>关节数: {{ robot.jointCount }}</span>
            </div>
            <div class="robot-actions">
              <el-button
                type="primary"
                size="small"
                @click.stop="injectFault(robot.id)"
                :loading="injectingFaults[robot.id]"
              >
                <el-icon><Warning /></el-icon>
                注入故障
              </el-button>
              <el-button
                type="default"
                size="small"
                @click="goToRobotDetail(robot.id)"
              >
                查看详情
              </el-button>
            </div>
          </el-card>
        </div>
      </el-card>

      <!-- 最新告警 -->
      <el-card class="alarms-section">
        <template #header>
          <div class="section-header">
            <span>最新告警</span>
            <el-button type="text" size="small" @click="$router.push('/alarms')">
              查看全部
            </el-button>
          </div>
        </template>

        <div v-if="alarmStore.loading" class="loading">
          <el-icon class="is-loading">
            <Loading />
          </el-icon>
          加载中...
        </div>

        <div v-else-if="alarmStore.alarms.length === 0" class="empty-state">
          <el-empty description="暂无告警信息" />
        </div>

        <div v-else class="alarms-list">
          <div
            v-for="alarm in alarmStore.alarms.slice(0, 5)"
            :key="alarm.id"
            class="alarm-item"
            @click="goToAlarmDetail(alarm.id)"
          >
            <div class="alarm-content">
              <div class="alarm-title">
                {{ alarm.alarmType }} - {{ alarm.robot.name }}
                <el-tag
                  :type="getSeverityType(alarm.severity)"
                  size="small"
                >
                  {{ alarm.severity }}
                </el-tag>
              </div>
              <div class="alarm-time">
                {{ formatTime(alarm.lastSeenTs) }}
              </div>
            </div>
            <div class="alarm-actions">
              <el-button
                v-if="alarm.status === 'OPEN'"
                type="primary"
                size="small"
                @click.stop="acknowledgeAlarm(alarm.id)"
              >
                确认
              </el-button>
            </div>
          </div>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
// icons are registered globally in main.ts; no local imports needed
import { Setting } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { useAlarmStore } from '@/stores/alarm'
import { simApi } from '@/api/sim'
import { robotApi } from '@/api/robot'
import type { Robot } from '@/types/robot'
import api from '@/api/auth'

const router = useRouter()
const authStore = useAuthStore()
const alarmStore = useAlarmStore()

// 状态
const robots = ref<Robot[]>([])
const robotsLoading = ref(false)
const injectingFaults = ref<Record<string, boolean>>({})

// 初始化
onMounted(async () => {
  await loadRobots()
  await alarmStore.loadAlarms()

  // 初始化WebSocket订阅
  alarmStore.initWebSocketSubscription()
})

// 加载机器人列表
const loadRobots = async () => {
  robotsLoading.value = true
  try {
    robots.value = await robotApi.getRobots()
  } catch (error: any) {
    console.error('Failed to load robots:', error)
    // 如果是后端错误，robotApi内部已做localStorage fallback，这里只显示用户友好的错误
    if (error?.response?.status !== 401 && error?.response?.status < 500) {
      ElMessage.error('加载机器人列表失败')
    }
  } finally {
    robotsLoading.value = false
  }
}

// 刷新机器人列表
const refreshRobots = () => {
  loadRobots()
}

// 注入故障
const injectFault = async (robotId: string) => {
  try {
    injectingFaults.value[robotId] = true

    // 首先检查是否有正在运行的仿真
    const simResponse = await api.get('/api/v1/sim/runs')
    const runningRuns = simResponse.data.data.filter((run: any) => run.status === 'RUNNING')

    let scenarioRunId: string

    if (runningRuns.length > 0) {
      // 使用现有的运行
      scenarioRunId = runningRuns[0].id
      ElMessage.info('使用现有的仿真运行')
    } else {
      // 获取可用场景
      const scenarios = await simApi.getScenarios()
      if (!scenarios.length) {
        ElMessage.error('未找到可用场景，请先在数据库初始化或创建场景')
        return
      }

      // 使用第一个可用场景（后续可做UI选择）
      const scenarioId = scenarios[0].id

      // 创建新的仿真运行
      const createResponse = await api.post('/api/v1/sim/runs', {
        scenarioId, // 使用真实的场景ID
        mode: 'REALTIME',
        rateHz: 1,
        seed: Date.now()
      })
      scenarioRunId = createResponse.data.data.id

      // 启动仿真
      await api.post(`/api/v1/sim/runs/${scenarioRunId}/start`)
      ElMessage.info('已启动新的仿真运行')
    }

    // 注入故障 - 过热故障，持续30秒
    const faultData = {
      faultType: 'OVERHEAT',
      startTs: new Date().toISOString(),
      endTs: new Date(Date.now() + 30000).toISOString(), // 30秒后结束
      params: {
        amplitude: 15.0, // 温度增加15°C
        jointIndex: 0 // 关节0
      }
    }

    await api.post(`/api/v1/sim/runs/${scenarioRunId}/faults`, faultData)

    ElMessage.success('故障注入成功！请查看告警信息')

    // 刷新告警数据
    await alarmStore.loadAlarms()

  } catch (error: any) {
    console.error('Failed to inject fault:', error)
    ElMessage.error(error?.response?.data?.message || '故障注入失败')
  } finally {
    injectingFaults.value[robotId] = false
  }
}

// 跳转到机器人详情
const goToRobotDetail = (robotId: string) => {
  router.push(`/robots/${robotId}`)
}

// 跳转到告警详情
const goToAlarmDetail = (alarmId: string) => {
  router.push(`/alarms/${alarmId}`)
}

// 确认告警
const acknowledgeAlarm = async (alarmId: string) => {
  try {
    await ElMessageBox.confirm('确定要确认这个告警吗？', '确认告警', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })

    await alarmStore.acknowledgeAlarm(alarmId)
    ElMessage.success('告警已确认')
  } catch (error) {
    if (error !== 'cancel') {
      console.error('Failed to acknowledge alarm:', error)
      ElMessage.error('确认告警失败')
    }
  }
}

// 获取告警严重程度对应的标签类型
const getSeverityType = (severity: string) => {
  switch (severity) {
    case 'CRITICAL': return 'danger'
    case 'WARN': return 'warning'
    case 'INFO': return 'info'
    default: return ''
  }
}

// 格式化时间
const formatTime = (timestamp: string) => {
  return new Date(timestamp).toLocaleString()
}

// 登出
const handleLogout = async () => {
  authStore.logout()
  // 使用路由跳转到登录页
  await router.push('/login')
}
</script>

<style scoped>
.dashboard-page {
  padding: 24px;
  max-width: 1400px;
  margin: 0 auto;
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  min-height: 100vh;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 32px;
  padding: 24px 0;
}

.page-header h1 {
  margin: 0;
  color: #1f2937;
  font-size: 28px;
  font-weight: 700;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
  background: rgba(255, 255, 255, 0.8);
  padding: 8px 16px;
  border-radius: 20px;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 20px;
  margin-bottom: 32px;
}

.stat-card {
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  border: 1px solid #f0f2f5;
}

.stat-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
  border-color: #d9d9d9;
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 8px 0;
}

.stat-icon {
  flex-shrink: 0;
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
}

.stat-info {
  flex: 1;
}

.stat-number {
  font-size: 36px;
  font-weight: 700;
  color: #1f2937;
  line-height: 1;
  margin-bottom: 4px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.stat-label {
  font-size: 14px;
  color: #6b7280;
  font-weight: 500;
  margin-top: 2px;
}

.main-content {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.section-header span {
  font-size: 18px;
  font-weight: 600;
  color: #1f2937;
}

.robots-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}

.robot-card {
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  border-radius: 12px;
  border: 1px solid #f0f2f5;
  overflow: hidden;
}

.robot-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
  border-color: #d9d9d9;
}

.robot-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
  padding: 16px 16px 0 16px;
}

.robot-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
  line-height: 1.4;
}

.robot-info {
  font-size: 13px;
  color: #6b7280;
  margin-bottom: 16px;
  padding: 0 16px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.robot-info::before {
  content: '';
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #10b981;
  flex-shrink: 0;
}

.robot-actions {
  display: flex;
  gap: 8px;
  padding: 0 16px 16px 16px;
}

.alarms-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.alarm-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border: 1px solid #f0f2f5;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  background: #ffffff;
}

.alarm-item:hover {
  background-color: #f8fafc;
  border-color: #e2e8f0;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  transform: translateX(2px);
}

.alarm-content {
  flex: 1;
}

.alarm-title {
  display: flex;
  align-items: center;
  gap: 12px;
  font-weight: 600;
  margin-bottom: 6px;
  color: #1f2937;
}

.alarm-title .el-tag {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 12px;
}

.alarm-time {
  font-size: 12px;
  color: #64748b;
  font-weight: 500;
}

.loading {
  text-align: center;
  padding: 40px;
  color: #909399;
}

.empty-state {
  padding: 40px;
}

@media (max-width: 768px) {
  .main-content {
    grid-template-columns: 1fr;
  }

  .stats-grid {
    grid-template-columns: 1fr;
  }
}
</style>
