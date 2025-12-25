<template>
  <div class="dashboard-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <h1>运维监控中心</h1>
      <div class="user-info">
        <span>欢迎，{{ authStore.user?.username }}</span>
        <el-button type="text" @click="handleLogout">登出</el-button>
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
            @click="goToRobotDetail(robot.id)"
          >
            <div class="robot-header">
              <h3>{{ robot.name }}</h3>
              <el-tag>{{ robot.model }}</el-tag>
            </div>
            <div class="robot-info">
              <span>关节数: {{ robot.jointCount }}</span>
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
import {
  Robot,
  Warning,
  Bell,
  Error,
  Refresh,
  Loading
} from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { useAlarmStore } from '@/stores/alarm'
import api from '@/api/auth'

const router = useRouter()
const authStore = useAuthStore()
const alarmStore = useAlarmStore()

// 状态
const robots = ref<any[]>([])
const robotsLoading = ref(false)

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
    const response = await api.get('/api/v1/robots')
    robots.value = response.data.data
  } catch (error) {
    console.error('Failed to load robots:', error)
    ElMessage.error('加载机器人列表失败')
  } finally {
    robotsLoading.value = false
  }
}

// 刷新机器人列表
const refreshRobots = () => {
  loadRobots()
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
const handleLogout = () => {
  authStore.logout()
}
</script>

<style scoped>
.dashboard-page {
  padding: 20px;
  max-width: 1400px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.page-header h1 {
  margin: 0;
  color: #303133;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 16px;
  margin-bottom: 24px;
}

.stat-card {
  cursor: pointer;
  transition: transform 0.2s;
}

.stat-card:hover {
  transform: translateY(-2px);
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-info {
  flex: 1;
}

.stat-number {
  font-size: 32px;
  font-weight: bold;
  color: #303133;
  line-height: 1;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-top: 4px;
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
}

.robots-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 12px;
}

.robot-card {
  cursor: pointer;
  transition: all 0.2s;
}

.robot-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.robot-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.robot-header h3 {
  margin: 0;
  font-size: 16px;
}

.robot-info {
  font-size: 12px;
  color: #909399;
}

.alarms-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.alarm-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s;
}

.alarm-item:hover {
  background-color: #f5f7fa;
  border-color: #c0c4cc;
}

.alarm-content {
  flex: 1;
}

.alarm-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 500;
  margin-bottom: 4px;
}

.alarm-time {
  font-size: 12px;
  color: #909399;
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
