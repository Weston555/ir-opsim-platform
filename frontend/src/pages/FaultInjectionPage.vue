<template>
  <div class="fault-injection-page">
    <div class="page-header">
      <h1>故障注入控制台</h1>
      <p>配置和注入故障用于测试异常检测算法</p>
    </div>

    <div class="content">
      <el-tabs v-model="activeTab">
        <!-- 故障注入标签页 -->
        <el-tab-pane label="故障注入" name="injection">
      <!-- 仿真运行选择 -->
      <el-card class="scenario-card">
        <template #header>
          <div class="card-header">
            <span>仿真运行选择</span>
            <el-button type="primary" @click="refreshRuns">刷新</el-button>
          </div>
        </template>

        <el-table :data="scenarioRuns" style="width: 100%" v-loading="loading">
          <el-table-column prop="id" label="运行ID" width="200">
            <template #default="scope">
              {{ scope.row.id.substring(0, 8) }}...
            </template>
          </el-table-column>
          <el-table-column prop="scenario.name" label="场景名称" width="150"></el-table-column>
          <el-table-column prop="mode" label="模式" width="100">
            <template #default="scope">
              <el-tag :type="scope.row.mode === 'REALTIME' ? 'success' : 'warning'">
                {{ scope.row.mode }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="100">
            <template #default="scope">
              <el-tag :type="getStatusType(scope.row.status)">
                {{ scope.row.status }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="rateHz" label="采样率" width="100">
            <template #default="scope">
              {{ scope.row.rateHz }} Hz
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="创建时间" width="180">
            <template #default="scope">
              {{ formatDate(scope.row.createdAt) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="350">
            <template #default="scope">
              <el-button
                type="primary"
                size="small"
                @click="selectRun(scope.row)"
                :disabled="selectedRun?.id === scope.row.id"
              >
                {{ selectedRun?.id === scope.row.id ? '已选择' : '选择' }}
              </el-button>
              <el-button
                v-if="scope.row.status === 'STOPPED' && scope.row.mode === 'REALTIME'"
                type="success"
                size="small"
                @click="startReplay(scope.row)"
                :loading="replayingRuns[scope.row.id]"
              >
                回放
              </el-button>
              <el-button
                v-if="isReplaying(scope.row.id)"
                type="warning"
                size="small"
                @click="stopReplay(scope.row.id)"
              >
                停止回放
              </el-button>
              <el-dropdown @command="(command) => handleExport({ action: command, runId: scope.row.id })" trigger="click">
                <el-button type="info" size="small">
                  导出数据 <el-icon class="el-icon--right"><ArrowDown /></el-icon>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="csv">导出CSV</el-dropdown-item>
                    <el-dropdown-item command="json">导出JSON</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <!-- 故障注入配置 -->
      <el-card class="injection-card" v-if="selectedRun">
        <template #header>
          <div class="card-header">
            <span>故障注入配置</span>
            <el-button type="success" @click="injectFault" :loading="injecting">
              注入故障
            </el-button>
          </div>
        </template>

        <!-- 故障模板选择 -->
        <el-form :model="faultForm" :rules="faultRules" ref="faultFormRef" label-width="120px">
          <el-row :gutter="20">
            <el-col :span="24">
              <el-form-item label="故障模板" prop="templateIds">
                <el-select
                  v-model="faultForm.templateIds"
                  placeholder="选择预设故障模板（可多选）"
                  filterable
                  clearable
                  multiple
                  collapse-tags
                  style="width: 100%"
                  @change="onTemplateChange"
                >
                  <el-option
                    v-for="template in faultTemplates"
                    :key="template.id"
                    :label="`${template.name} - ${getFaultTypeLabel(template.faultType)} - ${template.severity}`"
                    :value="template.id"
                  >
                    <div style="display: flex; justify-content: space-between; align-items: center; width: 100%">
                      <span>{{ template.name }}</span>
                      <div style="display: flex; gap: 8px">
                        <el-tag size="small" :type="getFaultTypeColor(template.faultType)">
                          {{ getFaultTypeLabel(template.faultType) }}
                        </el-tag>
                        <el-tag size="small" :type="getSeverityType(template.severity)">
                          {{ template.severity }}
                        </el-tag>
                      </div>
                    </div>
                    <div style="font-size: 12px; color: #909399; margin-top: 4px">
                      {{ template.description }}
                    </div>
                  </el-option>
                </el-select>
              </el-form-item>

              <el-form-item v-if="faultForm.templateIds?.length > 1" label="批量间隔(s)" prop="batchGapSec">
                <el-input-number
                  v-model="faultForm.batchGapSec"
                  :min="0"
                  :max="300"
                  :step="1"
                  style="width: 100%"
                  placeholder="故障间的间隔秒数"
                />
              </el-form-item>
            </el-col>
          </el-row>

          <el-divider>或手动配置故障参数</el-divider>

          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="故障类型" prop="faultType">
                <el-select v-model="faultForm.faultType" placeholder="选择故障类型" style="width: 100%" @change="onFaultTypeChange">
                  <el-option label="过热" value="OVERHEAT"></el-option>
                  <el-option label="高振动" value="HIGH_VIBRATION"></el-option>
                  <el-option label="电流尖峰" value="CURRENT_SPIKE"></el-option>
                  <el-option label="传感器漂移" value="SENSOR_DRIFT"></el-option>
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="开始时间" prop="startTime">
                <el-date-picker
                  v-model="faultForm.startTime"
                  type="datetime"
                  placeholder="选择开始时间"
                  format="YYYY-MM-DD HH:mm:ss"
                  value-format="YYYY-MM-DDTHH:mm:ss"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="结束时间" prop="endTime">
                <el-date-picker
                  v-model="faultForm.endTime"
                  type="datetime"
                  placeholder="选择结束时间"
                  format="YYYY-MM-DD HH:mm:ss"
                  value-format="YYYY-MM-DDTHH:mm:ss"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="幅值" prop="amplitude">
                <el-input-number
                  v-model="faultForm.amplitude"
                  :min="0.1"
                  :max="10.0"
                  :step="0.1"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="关节索引" prop="jointIndex">
                <el-input-number
                  v-model="faultForm.jointIndex"
                  :min="0"
                  :max="5"
                  :step="1"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="漂移率" prop="driftRate" v-if="faultForm.faultType === 'SENSOR_DRIFT'">
                <el-input-number
                  v-model="faultForm.driftRate"
                  :min="0.001"
                  :max="0.1"
                  :step="0.001"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>
          </el-row>
        </el-form>

        <!-- 故障参数说明 -->
        <el-alert
          title="故障参数说明"
          :description="getFaultDescription(faultForm.faultType)"
          type="info"
          show-icon
          style="margin-top: 20px"
        />
      </el-card>

      <!-- 故障注入时间轴可视化 -->
      <el-card class="timeline-card" v-if="selectedRun">
        <template #header>
          <div class="card-header">
            <span>故障注入时间轴</span>
            <el-button type="info" @click="loadInjectedFaults" size="small">刷新</el-button>
          </div>
        </template>

        <div class="timeline-container">
          <div class="current-time-indicator" :style="{ left: getCurrentTimePosition() + '%' }">
            <div class="time-label">{{ formatTime(new Date().toISOString()) }}</div>
            <div class="time-pointer"></div>
          </div>

          <div v-for="fault in injectedFaults" :key="fault.id" class="fault-timeline-item">
            <div class="fault-label">
              <el-tag :type="getFaultTypeColor(fault.faultType)" size="small">
                {{ getFaultTypeLabel(fault.faultType) }}
              </el-tag>
            </div>
            <div class="fault-bar" :style="getFaultBarStyle(fault)"></div>
          </div>
        </div>

        <div class="timeline-legend">
          <div class="legend-item">
            <div class="legend-color overheat"></div>
            <span>过热</span>
          </div>
          <div class="legend-item">
            <div class="legend-color vibration"></div>
            <span>高振动</span>
          </div>
          <div class="legend-item">
            <div class="legend-color current"></div>
            <span>电流尖峰</span>
          </div>
          <div class="legend-item">
            <div class="legend-color drift"></div>
            <span>传感器漂移</span>
          </div>
        </div>
      </el-card>

      <!-- 已注入的故障列表 -->
      <el-card class="injected-faults-card" v-if="selectedRun">
        <template #header>
          <div class="card-header">
            <span>已注入的故障详情</span>
            <el-button type="info" @click="loadInjectedFaults" size="small">刷新</el-button>
          </div>
        </template>

        <el-table :data="injectedFaults" style="width: 100%" v-loading="loadingFaults">
          <el-table-column prop="faultType" label="故障类型" width="120">
            <template #default="scope">
              <el-tag :type="getFaultTypeColor(scope.row.faultType)">
                {{ getFaultTypeLabel(scope.row.faultType) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="startTs" label="开始时间" width="180">
            <template #default="scope">
              {{ formatDate(scope.row.startTs) }}
            </template>
          </el-table-column>
          <el-table-column prop="endTs" label="结束时间" width="180">
            <template #default="scope">
              {{ formatDate(scope.row.endTs) }}
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="创建时间" width="180">
            <template #default="scope">
              {{ formatDate(scope.row.createdAt) }}
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="scope">
              <el-tag :type="isFaultActive(scope.row) ? 'danger' : 'success'">
                {{ isFaultActive(scope.row) ? '激活中' : '已结束' }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
        </el-tab-pane>

        <!-- 模板管理标签页 -->
        <el-tab-pane label="模板管理" name="templates">
          <el-card>
            <template #header>
              <div class="card-header">
                <span>故障模板管理</span>
                <el-button type="primary" @click="openTemplateDialog()">新增模板</el-button>
              </div>
            </template>

            <el-table :data="faultTemplates" style="width: 100%">
              <el-table-column prop="name" label="模板名称" width="150"></el-table-column>
              <el-table-column prop="faultType" label="故障类型" width="120">
                <template #default="scope">
                  <el-tag :type="getFaultTypeColor(scope.row.faultType)">
                    {{ getFaultTypeLabel(scope.row.faultType) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="severity" label="严重程度" width="100">
                <template #default="scope">
                  <el-tag :type="getSeverityType(scope.row.severity)">
                    {{ scope.row.severity }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="durationSeconds" label="持续时间" width="100">
                <template #default="scope">
                  {{ scope.row.durationSeconds }}s
                </template>
              </el-table-column>
              <el-table-column prop="enabled" label="启用状态" width="100">
                <template #default="scope">
                  <el-tag :type="scope.row.enabled ? 'success' : 'info'">
                    {{ scope.row.enabled ? '启用' : '禁用' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="createdAt" label="创建时间" width="180">
                <template #default="scope">
                  {{ formatDate(scope.row.createdAt) }}
                </template>
              </el-table-column>
              <el-table-column label="操作" width="150">
                <template #default="scope">
                  <el-button type="primary" size="small" @click="openTemplateDialog(scope.row)">编辑</el-button>
                  <el-button type="danger" size="small" @click="deleteTemplateById(scope.row.id)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-card>
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- 模板编辑弹窗 -->
    <el-dialog
      v-model="templateDialogVisible"
      :title="editingTemplateId ? '编辑模板' : '新增模板'"
      width="600px"
    >
      <el-form
        ref="templateFormRef"
        :model="templateForm"
        :rules="templateRules"
        label-width="120px"
      >
        <el-form-item label="模板名称" prop="name">
          <el-input v-model="templateForm.name" placeholder="输入模板名称" />
        </el-form-item>

        <el-form-item label="故障类型" prop="faultType">
          <el-select v-model="templateForm.faultType" placeholder="选择故障类型" style="width: 100%">
            <el-option label="过热" value="OVERHEAT"></el-option>
            <el-option label="高振动" value="HIGH_VIBRATION"></el-option>
            <el-option label="电流尖峰" value="CURRENT_SPIKE"></el-option>
            <el-option label="传感器漂移" value="SENSOR_DRIFT"></el-option>
          </el-select>
        </el-form-item>

        <el-form-item label="严重程度" prop="severity">
          <el-select v-model="templateForm.severity" placeholder="选择严重程度" style="width: 100%">
            <el-option label="低" value="LOW"></el-option>
            <el-option label="中" value="MEDIUM"></el-option>
            <el-option label="高" value="HIGH"></el-option>
            <el-option label="严重" value="CRITICAL"></el-option>
          </el-select>
        </el-form-item>

        <el-form-item label="持续时间(s)" prop="durationSeconds">
          <el-input-number
            v-model="templateForm.durationSeconds"
            :min="1"
            :max="3600"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="参数配置" prop="params">
          <el-input
            v-model="templateParamsJson"
            type="textarea"
            :rows="4"
            placeholder='输入JSON格式的参数，例如：{"amplitude": 10.0}'
          />
          <div class="form-tip">参数需要是有效的JSON格式</div>
        </el-form-item>

        <el-form-item label="描述">
          <el-input
            v-model="templateForm.description"
            type="textarea"
            :rows="2"
            placeholder="输入模板描述"
          />
        </el-form-item>

        <el-form-item label="启用状态">
          <el-switch v-model="templateForm.enabled" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="templateDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveTemplate">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance } from 'element-plus'
import { ArrowDown } from '@element-plus/icons-vue'
import { simApi } from '@/api/sim'
import api from '@/api/auth'
import {
  BUILT_IN_FAULT_TEMPLATES,
  BUILT_IN_RUNS,
  ensureDemoRun,
  readMockFaultInjections,
  appendMockFaultInjections,
  buildBatchFromTemplates,
  type FaultTemplate
} from '@/mock/faults'
import {
  loadTemplates,
  saveTemplates,
  upsertTemplate,
  deleteTemplate
} from '@/mock/templateStore'

interface ScenarioRun {
  id: string
  scenario: {
    name: string
  }
  mode: string
  status: string
  rateHz: number
  createdAt: string
  startedAt?: string
  endedAt?: string
}

interface FaultInjection {
  id: number
  faultType: string
  startTs: string
  endTs: string
  params: any
  createdAt: string
}


interface FaultForm {
  templateIds: string[]
  batchGapSec: number
  faultType: string
  startTime: string
  endTime: string
  amplitude: number
  jointIndex: number
  driftRate: number
}

// 响应式数据
const loading = ref(false)
const loadingFaults = ref(false)
const injecting = ref(false)
const scenarioRuns = ref<ScenarioRun[]>([])
const injectedFaults = ref<FaultInjection[]>([])
const selectedRun = ref<ScenarioRun | null>(null)
const replayingRuns = ref<Record<string, boolean>>({})
const faultTemplates = ref<FaultTemplate[]>([])

// 模板管理状态
const activeTab = ref('injection')
const templateDialogVisible = ref(false)
const templateForm = reactive<Partial<FaultTemplate>>({
  name: '',
  description: '',
  faultType: 'OVERHEAT',
  params: { amplitude: 1.0 },
  durationSeconds: 60,
  severity: 'MEDIUM',
  tags: [],
  enabled: true
})
const editingTemplateId = ref<string | null>(null)

const faultFormRef = ref<FormInstance>()
const templateFormRef = ref<FormInstance>()

const faultForm = reactive<FaultForm>({
  templateIds: [],
  batchGapSec: 5,
  faultType: 'OVERHEAT',
  startTime: '',
  endTime: '',
  amplitude: 1.0,
  jointIndex: 0,
  driftRate: 0.01
})

const faultRules = {
  templateIds: [{ required: true, message: '请至少选择一个模板', trigger: 'change' }],
  faultType: [{ required: true, message: '请选择故障类型', trigger: 'change' }],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  endTime: [{ required: true, message: '请选择结束时间', trigger: 'change' }],
  amplitude: [{ required: true, message: '请输入幅值', trigger: 'blur' }]
}

const templateRules = {
  name: [{ required: true, message: '请输入模板名称', trigger: 'blur' }],
  faultType: [{ required: true, message: '请选择故障类型', trigger: 'change' }],
  severity: [{ required: true, message: '请选择严重程度', trigger: 'change' }],
  durationSeconds: [{ required: true, message: '请输入持续时间', trigger: 'blur' }],
  params: [{ required: true, message: '请输入参数配置', trigger: 'blur' }]
}

// 模板参数JSON转换
const templateParamsJson = computed({
  get: () => {
    try {
      return JSON.stringify(templateForm.params, null, 2)
    } catch {
      return '{}'
    }
  },
  set: (value: string) => {
    try {
      templateForm.params = JSON.parse(value)
    } catch {
      templateForm.params = {}
    }
  }
})

// 方法
const refreshRuns = async () => {
  loading.value = true
  try {
    // 调用API获取仿真运行列表
    scenarioRuns.value = await simApi.getScenarioRuns()
  } catch (error: any) {
    console.warn('Backend failed, falling back to built-in demo runs:', error?.response?.status || error?.message)
    // 后端失败时使用内置演示运行
    scenarioRuns.value = builtInRuns
    ElMessage.warning('仿真运行后端不可用，已切换到演示运行')
  } finally {
    loading.value = false
  }
}

// 加载故障模板
const loadFaultTemplates = async () => {
  try {
    // 先尝试从后端加载
    const backendTemplates = await simApi.getFaultTemplates()
    if (backendTemplates && backendTemplates.length > 0) {
      faultTemplates.value = backendTemplates
      // 同步到本地store
      saveTemplates(backendTemplates)
    } else {
      throw new Error('No templates from backend')
    }
  } catch (error: any) {
    console.warn('Backend templates failed, using local templates:', error?.response?.status || error?.message)

    // 使用本地模板store
    const localTemplates = loadTemplates()
    faultTemplates.value = localTemplates

    if (localTemplates.length === 0) {
      ElMessage.warning('模板加载失败，已初始化默认模板')
    }
  }
}

// 模板选择事件处理
const onTemplateChange = (templateIds: string[]) => {
  if (!templateIds || templateIds.length === 0) {
    // 清除模板选择，恢复手动配置
    resetFaultForm()
    return
  }

  // 单选：沿用原"自动填充"逻辑（用第一条）
  if (templateIds.length === 1) {
    const template = faultTemplates.value.find(t => t.id === templateIds[0])
    if (template) {
      // 自动填充表单
      faultForm.faultType = template.faultType
      const params = typeof template.params === 'string' ? JSON.parse(template.params) : template.params
      faultForm.amplitude = params.amplitude || 1.0
      faultForm.jointIndex = params.jointIndex || 0
      faultForm.driftRate = params.driftRate || 0.01

      // 计算结束时间
      const startTime = new Date()
      const endTime = new Date(startTime.getTime() + template.durationSeconds * 1000)
      faultForm.startTime = startTime.toISOString().slice(0, 16)
      faultForm.endTime = endTime.toISOString().slice(0, 16)

      ElMessage.info(`已加载故障模板: ${template.name}`)
    }
    return
  }

  // 多选：自动给 start/end（end=最后一条结束），供 UI 显示
  const startTime = new Date()
  let cursor = startTime.getTime()
  for (const id of templateIds) {
    const template = faultTemplates.value.find(t => t.id === id)
    if (!template) continue
    cursor = cursor + template.durationSeconds * 1000 + faultForm.batchGapSec * 1000
  }
  faultForm.startTime = startTime.toISOString().slice(0, 16)
  faultForm.endTime = new Date(cursor).toISOString().slice(0, 16)

  ElMessage.info(`已选择 ${templateIds.length} 个故障模板（批量模式）`)
}

// 故障类型变更事件处理
const onFaultTypeChange = () => {
  // 清除模板选择
  faultForm.templateId = undefined
}

// 重置故障表单
const resetFaultForm = () => {
  faultForm.templateId = undefined
  faultForm.faultType = 'OVERHEAT'
  faultForm.startTime = ''
  faultForm.endTime = ''
  faultForm.amplitude = 1.0
  faultForm.jointIndex = 0
  faultForm.driftRate = 0.01
}

const selectRun = (run: ScenarioRun) => {
  selectedRun.value = run
  loadInjectedFaults()
}

const injectFault = async () => {
  if (!selectedRun.value) return

  try {
    await faultFormRef.value?.validate()
  } catch (error) {
    return
  }

  injecting.value = true
  try {
    const robotId = route.query.robotId ? String(route.query.robotId) : undefined
    const startTs = faultForm.startTime ? new Date(faultForm.startTime).toISOString() : new Date().toISOString()

    // 模板批量
    if (faultForm.templateIds && faultForm.templateIds.length > 0) {
      const run = selectedRun.value.id.startsWith('mock-') ? selectedRun.value : selectedRun.value
      const templates = faultForm.templateIds
        .map(id => faultTemplates.value.find(t => t.id === id))
        .filter(Boolean)

      const records = buildBatchFromTemplates({
        run,
        robotId,
        templates,
        startTs,
        gapSec: faultForm.batchGapSec ?? 5
      })

      // mock-run 或后端失败：直接落本地
      if (selectedRun.value.id.startsWith('mock-')) {
        appendMockFaultInjections(records)
        ElMessage.success(`已批量注入 ${records.length} 条故障（演示模式）`)
        loadInjectedFaults()
        return
      }

      // 后端可用：逐条调用；任一失败则整体降级本地
      try {
        for (const r of records) {
          await simApi.addFaultInjection(selectedRun.value.id, {
            faultType: r.faultType,
            startTs: r.startTs,
            endTs: r.endTs,
            params: r.params,
            robotId
          })
        }
        ElMessage.success(`已批量注入 ${records.length} 条故障`)
        loadInjectedFaults()
        return
      } catch (e) {
        appendMockFaultInjections(records)
        ElMessage.warning(`后端不可用，已批量注入 ${records.length} 条故障（演示模式）`)
        loadInjectedFaults()
        return
      }
    }

    // 否则走原本的"手动配置注入"逻辑
    let request: any

    if (faultForm.templateId) {
      // 使用模板参数
      const template = faultTemplates.value.find(t => t.id === faultForm.templateId)
      if (template) {
        // 解析模板参数（可能是JSON字符串）
        let templateParams = template.params
        if (typeof templateParams === 'string') {
          try {
            templateParams = JSON.parse(templateParams)
          } catch (e) {
            console.error('Failed to parse template params:', e)
            templateParams = {}
          }
        }

        request = {
          faultType: template.faultType,
          startTs: new Date(faultForm.startTime).toISOString(),
          endTs: new Date(faultForm.endTime).toISOString(),
          params: templateParams
        }
      }
    } else {
      // 使用手动配置的参数
      const params: Record<string, any> = {
        amplitude: faultForm.amplitude,
        jointIndex: faultForm.jointIndex
      }

      if (faultForm.faultType === 'SENSOR_DRIFT') {
        params.driftRate = faultForm.driftRate
      }

      request = {
        faultType: faultForm.faultType,
        startTs: new Date(faultForm.startTime).toISOString(),
        endTs: new Date(faultForm.endTime).toISOString(),
        params: params
      }
    }

    // 如果是mock run（演示模式），直接写入本地存储
    if (selectedRun.value.id.startsWith('mock-')) {
      const mockInjection: FaultInjection = {
        id: `local-${Date.now()}`,
        scenarioRun: selectedRun.value,
        robotId,
        faultType: request.faultType,
        startTs: request.startTs,
        endTs: request.endTs,
        params: request.params,
        createdAt: new Date().toISOString()
      }

      appendMockFaultInjections([mockInjection])
      ElMessage.success('已在本地演示模式写入故障注入记录')
      loadInjectedFaults()
    } else {
      // 正常API调用
      await simApi.addFaultInjection(selectedRun.value.id, request)
      ElMessage.success('故障注入成功')
      loadInjectedFaults()
    }
  } catch (error: any) {
    // 如果API调用失败，也尝试写入本地演示记录
    console.warn('Fault injection API failed, trying local fallback:', error?.response?.status || error?.message)

    const mockInjection: FaultInjection = {
      id: `fallback-${Date.now()}`,
      scenarioRun: selectedRun.value,
      faultType: request.faultType,
      startTs: request.startTs,
      endTs: request.endTs,
      params: request.params,
      createdAt: new Date().toISOString()
    }

    const currentInjections = readMockFaultInjections()
    currentInjections.push(mockInjection)
    writeMockFaultInjections(currentInjections)

    ElMessage.warning('后端不可用，已在本地演示模式写入故障注入记录')
    loadInjectedFaults()
  } finally {
    injecting.value = false
  }
}

const loadInjectedFaults = async () => {
  if (!selectedRun.value) return

  loadingFaults.value = true
  try {
    // 调用API获取已注入的故障列表
    const response = await api.get(`/api/v1/sim/runs/${selectedRun.value.id}/faults`)
    injectedFaults.value = response.data.data || []
  } catch (error: any) {
    console.warn('Backend failed, falling back to localStorage for injected faults:', error?.response?.status || error?.message)

    // 从localStorage读取本地演示记录，并过滤当前run/robot
    const robotId = route.query.robotId ? String(route.query.robotId) : undefined
    const allMockInjections = readMockFaultInjections()
    injectedFaults.value = allMockInjections.filter(injection => {
      const sameRun = injection.scenarioRun?.id === selectedRun.value.id
      const sameRobot = !robotId || injection.robotId === robotId
      return sameRun && sameRobot
    })

    // 只在不是mock run时显示错误（避免演示模式下重复提示）
    if (!selectedRun.value.id.startsWith('mock-')) {
      ElMessage.warning('后端不可用，已加载本地演示记录')
    }
  } finally {
    loadingFaults.value = false
  }
}

// 模板管理函数
const loadTemplatesForManagement = () => {
  faultTemplates.value = loadTemplates()
}

const openTemplateDialog = (template?: FaultTemplate) => {
  if (template) {
    editingTemplateId.value = template.id
    Object.assign(templateForm, {
      ...template,
      params: typeof template.params === 'string' ? JSON.parse(template.params) : template.params
    })
  } else {
    editingTemplateId.value = null
    Object.assign(templateForm, {
      name: '',
      description: '',
      faultType: 'OVERHEAT',
      params: { amplitude: 1.0 },
      durationSeconds: 60,
      severity: 'MEDIUM',
      tags: [],
      enabled: true
    })
  }
  templateDialogVisible.value = true
}

const saveTemplate = async () => {
  if (!templateFormRef.value) return

  try {
    await templateFormRef.value.validate()

    const template: FaultTemplate = {
      id: editingTemplateId.value || `template-${Date.now()}`,
      name: templateForm.name!,
      description: templateForm.description || '',
      faultType: templateForm.faultType!,
      params: templateForm.params!,
      durationSeconds: templateForm.durationSeconds!,
      severity: templateForm.severity!,
      tags: templateForm.tags || [],
      enabled: templateForm.enabled ?? true
    }

    upsertTemplate(template)
    loadTemplatesForManagement()

    ElMessage.success(editingTemplateId.value ? '模板更新成功' : '模板创建成功')
    templateDialogVisible.value = false

    // 通知其他页面更新
    window.dispatchEvent(new Event('fault-templates-updated'))
  } catch (error) {
    ElMessage.error('保存失败')
  }
}

const deleteTemplateById = (id: string) => {
  ElMessageBox.confirm('确定删除此模板？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    deleteTemplate(id)
    loadTemplatesForManagement()
    ElMessage.success('模板删除成功')
    window.dispatchEvent(new Event('fault-templates-updated'))
  })
}

const getStatusType = (status: string) => {
  switch (status) {
    case 'RUNNING': return 'success'
    case 'STOPPED': return 'warning'
    case 'FINISHED': return 'info'
    default: return ''
  }
}

const getFaultTypeColor = (faultType: string) => {
  switch (faultType) {
    case 'OVERHEAT': return 'danger'
    case 'HIGH_VIBRATION': return 'warning'
    case 'CURRENT_SPIKE': return 'danger'
    case 'SENSOR_DRIFT': return 'info'
    default: return ''
  }
}

const getSeverityType = (severity: string) => {
  switch (severity) {
    case 'LOW': return 'info'
    case 'MEDIUM': return 'warning'
    case 'HIGH': return 'danger'
    case 'CRITICAL': return 'danger'
    default: return 'info'
  }
}

const getFaultTypeLabel = (faultType: string) => {
  switch (faultType) {
    case 'OVERHEAT': return '过热'
    case 'HIGH_VIBRATION': return '高振动'
    case 'CURRENT_SPIKE': return '电流尖峰'
    case 'SENSOR_DRIFT': return '传感器漂移'
    default: return faultType
  }
}

const getFaultDescription = (faultType: string) => {
  switch (faultType) {
    case 'OVERHEAT':
      return '温度缓慢上升，幅值表示温度增加的度数'
    case 'HIGH_VIBRATION':
      return '振动RMS值突然增加，幅值表示增加的倍数'
    case 'CURRENT_SPIKE':
      return '电流出现尖峰，幅值表示尖峰的安培数'
    case 'SENSOR_DRIFT':
      return '传感器逐渐漂移，幅值表示漂移强度，漂移率表示每秒的变化速度'
    default:
      return '请选择故障类型查看说明'
  }
}

const isFaultActive = (fault: FaultInjection) => {
  const now = new Date()
  const start = new Date(fault.startTs)
  const end = new Date(fault.endTs)
  return now >= start && now <= end
}

const formatDate = (dateString: string) => {
  return new Date(dateString).toLocaleString('zh-CN')
}

// 回放相关方法
const startReplay = async (run: ScenarioRun) => {
  try {
    replayingRuns.value[run.id] = true
    await simApi.startReplay(run.id, 1.0) // 1倍速回放
    ElMessage.success('回放开始')

    // 刷新运行列表
    await refreshRuns()
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message || '回放启动失败')
  } finally {
    replayingRuns.value[run.id] = false
  }
}

const stopReplay = async (runId: string) => {
  try {
    await simApi.stopReplay(runId)
    ElMessage.success('回放停止')

    // 刷新运行列表
    await refreshRuns()
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message || '回放停止失败')
  }
}

const isReplaying = (runId: string) => {
  return scenarioRuns.value.find(run => run.id === runId)?.status === 'RUNNING' &&
         replayingRuns.value[runId]
}

// 时间轴相关方法
const getCurrentTimePosition = () => {
  if (!selectedRun.value) return 0

  const runStart = new Date(selectedRun.value.startedAt || selectedRun.value.createdAt).getTime()
  const runEnd = new Date(selectedRun.value.endedAt || Date.now()).getTime()
  const now = Date.now()

  if (now < runStart) return 0
  if (now > runEnd) return 100

  return ((now - runStart) / (runEnd - runStart)) * 100
}

const getFaultBarStyle = (fault: FaultInjection) => {
  if (!selectedRun.value) return {}

  const runStart = new Date(selectedRun.value.startedAt || selectedRun.value.createdAt).getTime()
  const runEnd = new Date(selectedRun.value.endedAt || Date.now()).getTime()
  const faultStart = new Date(fault.startTs).getTime()
  const faultEnd = new Date(fault.endTs).getTime()

  const startPercent = Math.max(0, ((faultStart - runStart) / (runEnd - runStart)) * 100)
  const endPercent = Math.min(100, ((faultEnd - runStart) / (runEnd - runStart)) * 100)
  const widthPercent = Math.max(1, endPercent - startPercent)

  return {
    left: startPercent + '%',
    width: widthPercent + '%',
    backgroundColor: getFaultTypeColorHex(fault.faultType)
  }
}

const getFaultTypeColorHex = (faultType: string) => {
  switch (faultType) {
    case 'OVERHEAT': return '#F56C6C'
    case 'HIGH_VIBRATION': return '#E6A23C'
    case 'CURRENT_SPIKE': return '#F56C6C'
    case 'SENSOR_DRIFT': return '#909399'
    default: return '#C0C4CC'
  }
}

// 导出数据处理
const handleExport = async (command: any) => {
  const { action, runId } = command

  try {
    let url: string
    let filename: string

    if (action === 'csv') {
      url = `/api/v1/sim/runs/${runId}/export/csv`
      filename = `evaluation_report_${runId}.csv`
    } else if (action === 'json') {
      url = `/api/v1/sim/runs/${runId}/export/json`
      filename = `evaluation_report_${runId}.json`
    } else {
      return
    }

    // 使用API下载文件
    const response = await api.get(url, {
      responseType: 'blob'
    })

    // 创建下载链接
    const blob = new Blob([response.data])
    const downloadUrl = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = downloadUrl
    link.download = filename
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(downloadUrl)

    ElMessage.success(`${action.toUpperCase()}数据导出成功`)

  } catch (error: any) {
    console.error('Export failed:', error)
    ElMessage.error(error?.response?.data?.message || '数据导出失败')
  }
}

// 生命周期
onMounted(async () => {
  // runs：后端失败就内置演示；同时确保 mock-run 写入本地 runs（供 Dashboard 复用）
  try {
    await refreshRuns()
  } finally {
    // 如果后端失败，refreshRuns 内已经 fallback；这里额外保证 demo run 存在
    ensureDemoRun()
  }

  await loadFaultTemplates()
  loadTemplatesForManagement()

  const qRunId = route.query.runId ? String(route.query.runId) : ''
  if (qRunId) {
    const found = scenarioRuns.value.find(r => r.id === qRunId)
    if (found) selectRun(found)
  }
})
</script>

<style scoped>
.fault-injection-page {
  padding: 24px;
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  min-height: 100vh;
}

.page-header {
  margin-bottom: 40px;
  text-align: center;
}

.page-header h1 {
  font-size: 32px;
  font-weight: 700;
  color: #1f2937;
  margin-bottom: 12px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.page-header p {
  color: #64748b;
  font-size: 16px;
  font-weight: 500;
}

.content {
  max-width: 1400px;
  margin: 0 auto;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f2f5;
}

.card-header span {
  font-size: 18px;
  font-weight: 600;
  color: #1f2937;
}

.scenario-card, .injection-card, .timeline-card, .injected-faults-card {
  margin-bottom: 24px;
  border-radius: 12px;
  border: 1px solid #f0f2f5;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.scenario-card:hover, .injection-card:hover, .timeline-card:hover, .injected-faults-card:hover {
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
  border-color: #e2e8f0;
}

.injection-card {
  margin-bottom: 20px;
}

.timeline-card {
  margin-bottom: 20px;
}

.timeline-container {
  position: relative;
  height: 240px;
  background: linear-gradient(135deg, #ffffff 0%, #f8fafc 100%);
  border-radius: 12px;
  padding: 24px;
  margin-bottom: 24px;
  border: 1px solid #f0f2f5;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.04);
}

.current-time-indicator {
  position: absolute;
  top: 0;
  bottom: 0;
  width: 2px;
  background: #409EFF;
  z-index: 10;
}

.time-label {
  position: absolute;
  top: -25px;
  left: 50%;
  transform: translateX(-50%);
  background: #409EFF;
  color: white;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  white-space: nowrap;
}

.time-pointer {
  position: absolute;
  top: -5px;
  left: -4px;
  width: 0;
  height: 0;
  border-left: 4px solid transparent;
  border-right: 4px solid transparent;
  border-top: 5px solid #409EFF;
}

.fault-timeline-item {
  position: relative;
  height: 40px;
  margin-bottom: 16px;
  display: flex;
  align-items: center;
}

.fault-label {
  width: 140px;
  flex-shrink: 0;
  text-align: right;
  margin-right: 16px;
  font-weight: 600;
  color: #374151;
  font-size: 14px;
}

.fault-bar {
  height: 24px;
  border-radius: 12px;
  position: relative;
  opacity: 0.9;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.fault-bar:hover {
  opacity: 1;
  transform: scaleY(1.1);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.timeline-legend {
  display: flex;
  gap: 20px;
  justify-content: center;
  flex-wrap: wrap;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
}

.legend-color {
  width: 16px;
  height: 16px;
  border-radius: 2px;
}

.legend-color.overheat {
  background: #F56C6C;
}

.legend-color.vibration {
  background: #E6A23C;
}

.legend-color.current {
  background: #F56C6C;
}

.legend-color.drift {
  background: #909399;
}

.injected-faults-card {
  margin-bottom: 20px;
}
</style>
