<template>
  <div class="alarm-detail-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="breadcrumb">
        <el-breadcrumb separator="/">
          <el-breadcrumb-item @click="$router.push('/alarms')">告警中心</el-breadcrumb-item>
          <el-breadcrumb-item>告警详情</el-breadcrumb-item>
        </el-breadcrumb>
      </div>
      <div class="page-actions">
        <el-button @click="$router.back()">
          <el-icon><ArrowLeft /></el-icon>
          返回
        </el-button>
        <el-button
          v-if="alarm?.status === 'OPEN'"
          type="primary"
          @click="acknowledgeAlarm"
        >
          确认告警
        </el-button>
      </div>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-state">
      <el-icon class="is-loading">
        <Loading />
      </el-icon>
      <span>加载中...</span>
    </div>

    <!-- 错误状态 -->
    <div v-else-if="error" class="error-state">
      <el-alert
        title="加载失败"
        :description="error"
        type="error"
        show-icon
      />
      <el-button @click="loadData" style="margin-top: 16px;">重试</el-button>
    </div>

    <!-- 告警详情 -->
    <div v-else-if="alarm" class="alarm-content">
      <!-- 告警基本信息 -->
      <el-card class="alarm-card">
        <template #header>
          <div class="alarm-header">
            <span>告警信息</span>
            <div class="alarm-badges">
              <el-tag :type="getSeverityType(alarm.severity)">
                {{ alarm.severity }}
              </el-tag>
              <el-tag :type="getStatusType(alarm.status)">
                {{ getStatusText(alarm.status) }}
              </el-tag>
            </div>
          </div>
        </template>

        <div class="alarm-info-grid">
          <div class="info-item">
            <label>告警类型</label>
            <span>{{ getAlarmTypeText(alarm.alarmType) }}</span>
          </div>

          <div class="info-item">
            <label>机器人</label>
            <span>{{ alarm.robot.name }}</span>
          </div>

          <div class="info-item">
            <label>关节</label>
            <span>{{ alarm.jointIndex !== undefined ? `关节${alarm.jointIndex}` : '全局' }}</span>
          </div>

          <div class="info-item">
            <label>检测器</label>
            <span>{{ alarm.detector }}</span>
          </div>

          <div class="info-item">
            <label>异常分数</label>
            <span>{{ alarm.score.toFixed(2) }}</span>
          </div>

          <div class="info-item">
            <label>重复次数</label>
            <span>{{ alarm.count }}</span>
          </div>

          <div class="info-item">
            <label>首次发生</label>
            <span>{{ formatDateTime(alarm.firstSeenTs) }}</span>
          </div>

          <div class="info-item">
            <label>最后发生</label>
            <span>{{ formatDateTime(alarm.lastSeenTs) }}</span>
          </div>
        </div>
      </el-card>

      <!-- 检测证据 -->
      <el-card class="evidence-card">
        <template #header>
          <span>检测证据</span>
        </template>

        <div v-if="alarm.evidence" class="evidence-content">
          <pre>{{ JSON.stringify(alarm.evidence, null, 2) }}</pre>
        </div>
        <div v-else class="no-evidence">
          <el-empty description="暂无检测证据" />
        </div>
      </el-card>

      <!-- 知识库建议 -->
      <el-card class="recommendation-card">
        <template #header>
          <div class="recommendation-header">
            <span>处置建议</span>
            <el-tag v-if="recommendation" type="success">
              找到 {{ recommendation.matchedRules.length }} 条规则建议
            </el-tag>
          </div>
        </template>

        <div v-if="recommendationLoading" class="loading-state">
          <el-icon class="is-loading">
            <Loading />
          </el-icon>
          <span>加载建议中...</span>
        </div>

        <div v-else-if="recommendation">
          <!-- 规则建议 -->
          <div v-if="recommendation.matchedRules.length > 0" class="rules-section">
            <h3>匹配规则</h3>
            <div class="rules-list">
              <el-card
                v-for="rule in recommendation.matchedRules"
                :key="rule.rule.id"
                class="rule-card"
                shadow="hover"
              >
                <div class="rule-header">
                  <h4>{{ rule.rule.name }}</h4>
                  <el-tag type="info">优先级 {{ rule.rule.priority }}</el-tag>
                </div>
                <p class="rule-reason">{{ rule.reason }}</p>
                <div v-if="rule.rule.thenCase" class="rule-case">
                  <strong>建议案例:</strong> {{ rule.rule.thenCase.title }}
                </div>
              </el-card>
            </div>
          </div>

          <!-- 相关案例 -->
          <div v-if="recommendation.matchedCases.length > 0" class="cases-section">
            <h3>相关案例</h3>
            <div class="cases-list">
              <el-card
                v-for="matchedCase in recommendation.matchedCases"
                :key="matchedCase.kbCase.id"
                class="case-card"
                shadow="hover"
              >
                <div class="case-header">
                  <h4>{{ matchedCase.kbCase.title }}</h4>
                  <el-tag type="warning">{{ matchedCase.kbCase.faultType }}</el-tag>
                </div>
                <p class="case-description">{{ matchedCase.kbCase.rootCause }}</p>
                <p class="case-reason">{{ matchedCase.reason }}</p>
                <div v-if="matchedCase.kbCase.actions" class="case-actions">
                  <strong>处置步骤:</strong>
                  <ol>
                    <li v-for="action in matchedCase.kbCase.actions" :key="action">
                      {{ action }}
                    </li>
                  </ol>
                </div>
              </el-card>
            </div>
          </div>

          <!-- 解释 -->
          <div v-if="recommendation.explanation" class="explanation-section">
            <h3>详细解释</h3>
            <div class="explanation-content">
              {{ recommendation.explanation }}
            </div>
          </div>
        </div>

        <div v-else class="no-recommendation">
          <el-empty description="暂无相关建议" />
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
// icons are registered globally in main.ts; no local imports needed
import { useAlarmStore } from '@/stores/alarm'
import type { AlarmEvent, AlarmRecommendation } from '@/types/alarm'

const route = useRoute()
const router = useRouter()
const alarmStore = useAlarmStore()

// 状态
const alarmId = route.params.id as string
const loading = ref(false)
const error = ref('')
const recommendationLoading = ref(false)
const alarm = ref<AlarmEvent | null>(null)
const recommendation = ref<AlarmRecommendation | null>(null)

// 初始化
onMounted(() => {
  loadData()
})

// 加载数据
const loadData = async () => {
  loading.value = true
  error.value = ''

  try {
    // 加载告警详情
    alarm.value = await alarmStore.getAlarm(alarmId)

    // 加载建议
    await loadRecommendation()
  } catch (err: any) {
    error.value = err.message || '加载数据失败'
    ElMessage.error(error.value)
  } finally {
    loading.value = false
  }
}

// 加载建议
const loadRecommendation = async () => {
  if (!alarm.value) return

  recommendationLoading.value = true
  try {
    recommendation.value = await alarmStore.getAlarmRecommendation(alarmId)
  } catch (error) {
    console.error('Failed to load recommendation:', error)
    // 建议加载失败不影响页面显示
  } finally {
    recommendationLoading.value = false
  }
}

// 确认告警
const acknowledgeAlarm = async () => {
  if (!alarm.value) return

  try {
    const { value: comment } = await ElMessageBox.prompt(
      '请输入确认备注（可选）',
      '确认告警',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        inputPattern: /.*/,
        inputErrorMessage: '请输入有效的备注',
      }
    )

    await alarmStore.acknowledgeAlarm(alarmId, comment)
    ElMessage.success('告警已确认')

    // 重新加载数据
    await loadData()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('Failed to acknowledge alarm:', error)
      ElMessage.error('确认告警失败')
    }
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
.alarm-detail-page {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.page-actions {
  display: flex;
  gap: 12px;
}

.alarm-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.alarm-card, .evidence-card, .recommendation-card {
  margin-bottom: 0;
}

.alarm-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.alarm-badges {
  display: flex;
  gap: 8px;
}

.alarm-info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 16px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.info-item label {
  font-weight: bold;
  color: #606266;
  font-size: 14px;
}

.info-item span {
  color: #303133;
}

.evidence-content pre {
  background: #f6f8fa;
  padding: 16px;
  border-radius: 4px;
  overflow-x: auto;
  font-size: 12px;
  line-height: 1.4;
}

.recommendation-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.rules-section, .cases-section, .explanation-section {
  margin-bottom: 24px;
}

.rules-section h3, .cases-section h3, .explanation-section h3 {
  margin: 0 0 16px 0;
  color: #303133;
  font-size: 16px;
}

.rules-list, .cases-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.rule-card, .case-card {
  margin: 0;
}

.rule-header, .case-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.rule-header h4, .case-header h4 {
  margin: 0;
  color: #303133;
}

.rule-reason, .case-description, .case-reason {
  margin: 8px 0;
  color: #606266;
  line-height: 1.5;
}

.rule-case, .case-actions {
  margin-top: 8px;
  padding: 8px;
  background: #f6f8fa;
  border-radius: 4px;
  font-size: 14px;
}

.case-actions ol {
  margin: 8px 0 0 0;
  padding-left: 20px;
}

.case-actions li {
  margin-bottom: 4px;
}

.explanation-content {
  background: #f6f8fa;
  padding: 16px;
  border-radius: 4px;
  line-height: 1.6;
  white-space: pre-line;
}

.loading-state, .error-state, .no-evidence, .no-recommendation {
  text-align: center;
  padding: 40px 20px;
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

  .alarm-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .alarm-info-grid {
    grid-template-columns: 1fr;
  }

  .recommendation-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
}
</style>
