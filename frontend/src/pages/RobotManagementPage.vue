<template>
  <div class="robot-management-page">
    <div class="page-header">
      <h1>机器人管理</h1>
      <p>管理工业机器人的配置和状态</p>
    </div>

    <div class="content">
      <!-- 工具栏 -->
      <el-card class="toolbar-card">
        <div class="toolbar">
          <div class="search-section">
            <el-input
              v-model="searchModel"
              placeholder="按型号搜索机器人"
              clearable
              style="width: 300px"
              @input="handleSearch"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
          </div>
          <div class="actions">
            <el-button type="primary" @click="openCreateDialog">
              <el-icon><Plus /></el-icon>
              添加机器人
            </el-button>
          </div>
        </div>
      </el-card>

      <!-- 机器人列表 -->
      <el-card class="robots-card">
        <template #header>
          <div class="card-header">
            <span>机器人列表</span>
            <el-button type="text" size="small" @click="loadRobots">
              <el-icon><Refresh /></el-icon>
              刷新
            </el-button>
          </div>
        </template>

        <el-table
          :data="robots"
          style="width: 100%"
          v-loading="loading"
          :pagination="pagination"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        >
          <el-table-column prop="name" label="名称" width="150">
            <template #default="scope">
              <div class="robot-name">
                <el-icon :color="getStatusColor(scope.row.status)"><Monitor /></el-icon>
                {{ scope.row.name }}
              </div>
            </template>
          </el-table-column>

          <el-table-column prop="model" label="型号" width="120"></el-table-column>

          <el-table-column prop="jointCount" label="关节数" width="100" align="center"></el-table-column>

          <el-table-column prop="status" label="状态" width="120">
            <template #default="scope">
              <el-tag :type="getStatusType(scope.row.status)">
                {{ getStatusText(scope.row.status) }}
              </el-tag>
            </template>
          </el-table-column>

          <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip></el-table-column>

          <el-table-column prop="createdAt" label="创建时间" width="180">
            <template #default="scope">
              {{ formatDate(scope.row.createdAt) }}
            </template>
          </el-table-column>

          <el-table-column label="操作" width="250" fixed="right">
            <template #default="scope">
              <el-button
                type="primary"
                size="small"
                @click="viewRobot(scope.row)"
              >
                查看
              </el-button>
              <el-button
                type="warning"
                size="small"
                @click="editRobot(scope.row)"
              >
                编辑
              </el-button>
              <el-dropdown @command="(command) => handleStatusChange(scope.row.id, command)" trigger="click">
                <el-button type="info" size="small">
                  状态 <el-icon class="el-icon--right"><ArrowDown /></el-icon>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="ONLINE">上线</el-dropdown-item>
                    <el-dropdown-item command="OFFLINE">下线</el-dropdown-item>
                    <el-dropdown-item command="MAINTENANCE">维护</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
              <el-button
                type="danger"
                size="small"
                @click="deleteRobot(scope.row)"
              >
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <!-- 分页 -->
        <div class="pagination">
          <el-pagination
            v-model:current-page="pagination.currentPage"
            v-model:page-size="pagination.pageSize"
            :page-sizes="[10, 20, 50, 100]"
            :total="pagination.total"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
          />
        </div>
      </el-card>
    </div>

    <!-- 创建/编辑机器人对话框 -->
    <el-dialog
      :title="dialogTitle"
      v-model="showDialog"
      width="600px"
      :close-on-click-modal="false"
    >
      <el-form
        :model="robotForm"
        :rules="robotRules"
        ref="robotFormRef"
        label-width="120px"
      >
        <el-form-item label="机器人名称" prop="name">
          <el-input
            v-model="robotForm.name"
            placeholder="请输入机器人名称"
          />
        </el-form-item>

        <el-form-item label="型号" prop="model">
          <el-input
            v-model="robotForm.model"
            placeholder="请输入机器人型号"
          />
        </el-form-item>

        <el-form-item label="关节数" prop="jointCount">
          <el-input-number
            v-model="robotForm.jointCount"
            :min="1"
            :max="12"
            :step="1"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="描述" prop="description">
          <el-input
            v-model="robotForm.description"
            type="textarea"
            :rows="3"
            placeholder="请输入机器人描述"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="showDialog = false">取消</el-button>
          <el-button
            type="primary"
            @click="submitRobot"
            :loading="submitting"
          >
            {{ isEditing ? '更新' : '创建' }}
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance } from 'element-plus'
import { Search, Plus, Refresh, ArrowDown, Monitor } from '@element-plus/icons-vue'
import { robotApi } from '@/api/robot'
import type { Robot } from '@/types/robot'

// 响应式数据
const loading = ref(false)
const submitting = ref(false)
const showDialog = ref(false)
const showCreateDialog = ref(false)
const isEditing = ref(false)
const searchModel = ref('')
const robots = ref<Robot[]>([])

// 分页
const pagination = reactive({
  currentPage: 1,
  pageSize: 10,
  total: 0
})

// 表单
const robotFormRef = ref<FormInstance>()
const robotForm = reactive({
  id: '',
  name: '',
  model: '',
  jointCount: 6,
  description: ''
})

// 表单验证规则
const robotRules = {
  name: [
    { required: true, message: '请输入机器人名称', trigger: 'blur' },
    { min: 2, max: 50, message: '名称长度应为2-50个字符', trigger: 'blur' }
  ],
  model: [
    { required: true, message: '请输入机器人型号', trigger: 'blur' },
    { min: 1, max: 50, message: '型号长度应为1-50个字符', trigger: 'blur' }
  ],
  jointCount: [
    { required: true, message: '请选择关节数', trigger: 'change' }
  ]
}

// 计算属性
const dialogTitle = computed(() => isEditing.value ? '编辑机器人' : '添加机器人')

// 方法
const loadRobots = async () => {
  loading.value = true
  try {
    const response = await robotApi.getRobots({
      page: pagination.currentPage - 1,
      size: pagination.pageSize,
      model: searchModel.value || undefined
    })

    robots.value = response.data.content
    pagination.total = response.data.totalElements
  } catch (error) {
    ElMessage.error('加载机器人列表失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.currentPage = 1
  loadRobots()
}

const handleSizeChange = (size: number) => {
  pagination.pageSize = size
  pagination.currentPage = 1
  loadRobots()
}

const handleCurrentChange = (page: number) => {
  pagination.currentPage = page
  loadRobots()
}

const openCreateDialog = () => {
  isEditing.value = false
  resetForm()
  showDialog.value = true
}

const editRobot = (robot: Robot) => {
  isEditing.value = true
  robotForm.id = robot.id
  robotForm.name = robot.name
  robotForm.model = robot.model
  robotForm.jointCount = robot.jointCount
  robotForm.description = robot.description || ''
  showDialog.value = true
}

const viewRobot = (robot: Robot) => {
  // 跳转到机器人详情页
  router.push(`/robots/${robot.id}`)
}

const submitRobot = async () => {
  if (!robotFormRef.value) return

  try {
    await robotFormRef.value.validate()
  } catch (error) {
    return
  }

  submitting.value = true
  try {
    if (isEditing.value) {
      await robotApi.updateRobot(robotForm.id, robotForm)
      ElMessage.success('机器人更新成功')
    } else {
      await robotApi.createRobot(robotForm)
      ElMessage.success('机器人创建成功')
    }

    showDialog.value = false
    loadRobots()
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message || '操作失败')
  } finally {
    submitting.value = false
  }
}

const deleteRobot = async (robot: Robot) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除机器人 "${robot.name}" 吗？此操作不可撤销。`,
      '确认删除',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning',
      }
    )

    await robotApi.deleteRobot(robot.id)
    ElMessage.success('机器人删除成功')
    loadRobots()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const handleStatusChange = async (robotId: string, status: string) => {
  try {
    await robotApi.updateRobotStatus(robotId, { status })
    ElMessage.success('状态更新成功')
    loadRobots()
  } catch (error) {
    ElMessage.error('状态更新失败')
  }
}

const resetForm = () => {
  robotForm.id = ''
  robotForm.name = ''
  robotForm.model = ''
  robotForm.jointCount = 6
  robotForm.description = ''
  robotFormRef.value?.clearValidate()
}

const getStatusType = (status: string) => {
  switch (status) {
    case 'ONLINE': return 'success'
    case 'OFFLINE': return 'info'
    case 'MAINTENANCE': return 'warning'
    case 'ERROR': return 'danger'
    default: return 'info'
  }
}

const getStatusText = (status: string) => {
  switch (status) {
    case 'ONLINE': return '在线'
    case 'OFFLINE': return '离线'
    case 'MAINTENANCE': return '维护'
    case 'ERROR': return '故障'
    default: return status
  }
}

const getStatusColor = (status: string) => {
  switch (status) {
    case 'ONLINE': return '#67C23A'
    case 'OFFLINE': return '#909399'
    case 'MAINTENANCE': return '#E6A23C'
    case 'ERROR': return '#F56C6C'
    default: return '#909399'
  }
}

const formatDate = (dateString: string) => {
  return new Date(dateString).toLocaleString('zh-CN')
}

// 生命周期
onMounted(() => {
  loadRobots()
})
</script>

<style scoped>
.robot-management-page {
  padding: 20px;
}

.page-header {
  margin-bottom: 30px;
  text-align: center;
}

.page-header h1 {
  font-size: 28px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
}

.page-header p {
  color: #909399;
  font-size: 16px;
}

.content {
  max-width: 1400px;
  margin: 0 auto;
}

.toolbar-card, .robots-card {
  margin-bottom: 20px;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.search-section {
  display: flex;
  gap: 16px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.robot-name {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 500;
}

.robot-name .el-icon {
  font-size: 16px;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}

@media (max-width: 768px) {
  .toolbar {
    flex-direction: column;
    gap: 16px;
    align-items: stretch;
  }

  .search-section {
    flex-direction: column;
    gap: 12px;
  }

  .actions {
    align-self: flex-end;
  }
}
</style>
