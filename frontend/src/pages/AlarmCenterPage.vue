<template>
  <div class="alarm-center-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <h1>告警中心</h1>
      <div class="header-actions">
        <el-button type="primary" @click="refreshAlarms">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </div>
    </div>

    <!-- 筛选器 -->
    <el-card class="filter-card">
      <el-form :inline="true" :model="filters" class="filter-form">
        <el-form-item label="状态">
          <el-select v-model="filters.status" placeholder="全部状态" clearable>
            <el-option label="未确认" value="OPEN" />
            <el-option label="已确认" value="ACKED" />
            <el-option label="已关闭" value="CLOSED" />
          </el-select>
        </el-form-item>

        <el-form-item label="级别">
          <el-select v-model="filters.severity" placeholder="全部级别" clearable>
            <el-option label="信息" value="INFO" />
            <el-option label="警告" value="WARN" />
            <el-option label="严重" value="CRITICAL" />
          </el-select>
        </el-form-item>

        <el-form-item label="机器人">
          <el-select v-model="filters.robotId" placeholder="全部机器人" clearable filterable>
            <el-option
              v-for="robot in robots"
              :key="robot.id"
              :label="robot.name"
              :value="robot.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="时间范围">
          <el-date-picker
            v-model="dateRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DDTHH:mm:ss"
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="applyFilters">查询</el-button>
          <el-button @click="resetFilters">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 告警列表 -->
    <el-card class="alarms-card">
      <template #header>
        <div class="alarms-header">
          <span>告警列表 ({{ totalAlarms }}条)</span>
          <div class="alarms-stats">
            <el-tag type="danger">严重: {{ criticalCount }}</el-tag>
            <el-tag type="warning">警告: {{ warningCount }}</el-tag>
            <el-tag type="info">信息: {{ infoCount }}</el-tag>
          </div>
        </div>
      </template>

      <!-- 加载状态 -->
      <div v-if="alarmStore.loading" class="loading-state">
        <el-icon class="is-loading">
          <Loading />
        </el-icon>
        <span>加载中...</span>
      </div>

      <!-- 空状态 -->
      <div v-else-if="alarms.length === 0" class="empty-state">
        <el-empty description="暂无告警信息" />
      </div>

      <!-- 告警表格 -->
      <div v-else>
        <el-table
          :data="alarms"
          stripe
          style="width: 100%"
          @selection-change="handleSelectionChange"
        >
          <el-table-column type="selection" width="55" />

          <el-table-column prop="alarmType" label="告警类型" width="120">
            <template #default="scope">
              <el-tag :type="getAlarmTypeTag(scope.row.alarmType)">
                {{ getAlarmTypeText(scope.row.alarmType) }}
              </el-tag>
            </template>
          </el-table-column>

          <el-table-column prop="severity" label="级别" width="80">
            <template #default="scope">
              <el-tag :type="getSeverityType(scope.row.severity)">
                {{ scope.row.severity }}
              </el-tag>
            </template>
          </el-table-column>

          <el-table-column prop="robot.name" label="机器人" width="120" />

          <el-table-column prop="jointIndex" label="关节" width="80">
            <template #default="scope">
              {{ scope.row.jointIndex !== undefined ? `关节${scope.row.jointIndex}` : '全局' }}
            </template>
          </el-table-column>

          <el-table-column prop="status" label="状态" width="80">
            <template #default="scope">
              <el-tag :type="getStatusType(scope.row.status)">
                {{ getStatusText(scope.row.status) }}
              </el-tag>
            </template>
          </el-table-column>

          <el-table-column prop="count" label="次数" width="80" />

          <el-table-column prop="detector" label="检测器" width="100" />

          <el-table-column prop="score" label="分数" width="80">
            <template #default="scope">
              {{ scope.row.score.toFixed(2) }}
            </template>
          </el-table-column>

          <el-table-column prop="lastSeenTs" label="最后发生" width="160">
            <template #default="scope">
              {{ formatDateTime(scope.row.lastSeenTs) }}
            </template>
          </el-table-column>

          <el-table-column label="操作" width="150">
            <template #default="scope">
              <div class="action-buttons">
                <el-button
                  size="small"
                  type="primary"
                  @click="viewAlarmDetail(scope.row.id)"
                >
                  查看
                </el-button>
                <el-button
                  v-if="scope.row.status === 'OPEN'"
                  size="small"
                  @click="acknowledgeAlarm(scope.row.id)"
                >
                  确认
                </el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>

        <!-- 分页 -->
        <div class="pagination-wrapper">
          <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :page-sizes="[10, 20, 50, 100]"
            :total="totalAlarms"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
          />
        </div>
      </div>
    </el-card>

    <!-- 批量操作 -->
    <div v-if="selectedAlarms.length > 0" class="bulk-actions">
      <el-alert
        title="已选择 {{ selectedAlarms.length }} 条告警"
        type="info"
        show-icon
        :closable="false"
      />
      <div class="action-buttons">
        <el-button @click="acknowledgeSelectedAlarms">
          批量确认
        </el-button>
        <el-button @click="clearSelection">取消选择</el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Loading } from '@element-plus/icons-vue'
import { useAlarmStore } from '@/stores/alarm'
import { useRobotStore } from '@/stores/robot'
import type { AlarmEvent } from '@/types/alarm'
import type { Robot } from '@/types/robot'

const router = useRouter()
const alarmStore = useAlarmStore()
const robotStore = useRobotStore()

// 状态
const currentPage = ref(1)
const pageSize = ref(20)
const totalAlarms = ref(0)
const selectedAlarms = ref<AlarmEvent[]>([])
const dateRange = ref<[string, string] | null>(null)

// 筛选器
const filters = reactive({
  status: '',
  severity: '',
  robotId: '',
})

// 计算属性
const alarms = computed(() => alarmStore.alarms)
const robots = computed(() => robotStore.robots)
const criticalCount = computed(() => alarmStore.criticalAlarms.length)
const warningCount = computed(() => alarmStore.warningAlarms.length)
const infoCount = computed(() => alarmStore.infoAlarms.length)

// 初始化
onMounted(async () => {
  await loadRobots()
  await loadAlarms()

  // 初始化WebSocket订阅
  alarmStore.initWebSocketSubscription()
})

// 加载机器人列表
const loadRobots = async () => {
  try {
    await robotStore.loadRobots()
  } catch (error) {
    console.error('Failed to load robots:', error)
  }
}

// 加载告警列表
const loadAlarms = async () => {
  try {
    const query = {
      page: currentPage.value - 1, // 后端分页从0开始
      size: pageSize.value,
      status: filters.status || undefined,
      severity: filters.severity || undefined,
      robotId: filters.robotId || undefined,
      from: dateRange.value ? new Date(dateRange.value[0]) : undefined,
      to: dateRange.value ? new Date(dateRange.value[1]) : undefined,
    }

    const result = await alarmStore.loadAlarms(query)
    totalAlarms.value = result.totalElements || result.length
  } catch (error) {
    console.error('Failed to load alarms:', error)
    ElMessage.error('加载告警列表失败')
  }
}

// 刷新告警列表
const refreshAlarms = () => {
  loadAlarms()
}

// 应用筛选器
const applyFilters = () => {
  currentPage.value = 1
  loadAlarms()
}

// 重置筛选器
const resetFilters = () => {
  Object.keys(filters).forEach(key => {
    (filters as any)[key] = ''
  })
  dateRange.value = null
  currentPage.value = 1
  loadAlarms()
}

// 查看告警详情
const viewAlarmDetail = (alarmId: string) => {
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

// 批量确认告警
const acknowledgeSelectedAlarms = async () => {
  try {
    await ElMessageBox.confirm(
      `确定要确认选中的 ${selectedAlarms.value.length} 条告警吗？`,
      '批量确认告警',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
    )

    for (const alarm of selectedAlarms.value) {
      if (alarm.status === 'OPEN') {
        await alarmStore.acknowledgeAlarm(alarm.id)
      }
    }

    ElMessage.success('批量确认完成')
    selectedAlarms.value = []
  } catch (error) {
    if (error !== 'cancel') {
      console.error('Failed to acknowledge selected alarms:', error)
      ElMessage.error('批量确认失败')
    }
  }
}

// 清除选择
const clearSelection = () => {
  selectedAlarms.value = []
}

// 处理选择变化
const handleSelectionChange = (selection: AlarmEvent[]) => {
  selectedAlarms.value = selection
}

// 处理页码变化
const handleCurrentChange = (page: number) => {
  currentPage.value = page
  loadAlarms()
}

// 处理每页大小变化
const handleSizeChange = (size: number) => {
  pageSize.value = size
  currentPage.value = 1
  loadAlarms()
}

// 获取告警类型标签样式
const getAlarmTypeTag = (alarmType: string) => {
  switch (alarmType) {
    case 'CURRENT_ANOMALY': return 'primary'
    case 'VIB_ANOMALY': return 'warning'
    case 'TEMP_ANOMALY': return 'danger'
    default: return ''
  }
}

// 获取告警类型文本
const getAlarmTypeText = (alarmType: string) => {
  switch (alarmType) {
    case 'CURRENT_ANOMALY': return '电流异常'
    case 'VIB_ANOMALY': return '振动异常'
    case 'TEMP_ANOMALY': return '温度异常'
    case 'POSE_ANOMALY': return '位姿异常'
    default: return alarmType
  }
}

// 获取严重程度标签样式
const getSeverityType = (severity: string) => {
  switch (severity) {
    case 'CRITICAL': return 'danger'
    case 'WARN': return 'warning'
    case 'INFO': return 'info'
    default: return ''
  }
}

// 获取状态标签样式
const getStatusType = (status: string) => {
  switch (status) {
    case 'OPEN': return 'warning'
    case 'ACKED': return 'success'
    case 'CLOSED': return 'info'
    default: return ''
  }
}

// 获取状态文本
const getStatusText = (status: string) => {
  switch (status) {
    case 'OPEN': return '未确认'
    case 'ACKED': return '已确认'
    case 'CLOSED': return '已关闭'
    default: return status
  }
}

// 格式化日期时间
const formatDateTime = (timestamp: string) => {
  return new Date(timestamp).toLocaleString('zh-CN')
}
</script>

<style scoped>
.alarm-center-page {
  padding: 20px;
  max-width: 1400px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-header h1 {
  margin: 0;
  color: #303133;
}

.filter-card {
  margin-bottom: 20px;
}

.filter-form {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
}

.alarms-card {
  margin-bottom: 20px;
}

.alarms-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.alarms-stats {
  display: flex;
  gap: 8px;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

.bulk-actions {
  position: fixed;
  bottom: 20px;
  right: 20px;
  background: white;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  padding: 16px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  z-index: 1000;
  min-width: 300px;
}

.bulk-actions .action-buttons {
  margin-top: 12px;
  display: flex;
  gap: 8px;
  justify-content: flex-end;
}

.action-buttons {
  display: flex;
  gap: 8px;
}

.loading-state, .empty-state {
  text-align: center;
  padding: 60px 20px;
}

.loading-state {
  color: #909399;
}

@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }

  .filter-form {
    flex-direction: column;
  }

  .alarms-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .bulk-actions {
    position: static;
    margin-top: 20px;
  }
}
</style>
