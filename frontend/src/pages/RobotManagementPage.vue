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

        <!-- 机器人数据表格 - 支持增删改查操作 -->
        <el-table
          :data="robots"
          style="width: 100%"
          v-loading="loading"
          empty-text="暂无机器人数据，请点击上方添加按钮创建机器人"
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
              <el-tag :type="getStatusType(scope.row.status)" effect="dark">
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
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance } from 'element-plus'
import { Search, Plus, Refresh, ArrowDown, Monitor } from '@element-plus/icons-vue'
import { robotApi } from '@/api/robot'
import type { Robot } from '@/types/robot'

// ============ 响应式数据定义 ============
// 页面加载状态管理
const loading = ref(false)          // 表格数据加载状态
const submitting = ref(false)       // 表单提交状态
const showDialog = ref(false)       // 弹窗显示状态
const showCreateDialog = ref(false) // 创建弹窗状态（暂时未使用）
const isEditing = ref(false)        // 编辑模式标识
const searchModel = ref('')         // 搜索关键词
const robots = ref<Robot[]>([])     // 机器人列表数据

// 分页状态管理 - 支持大数据量的分页展示
const pagination = reactive({
  currentPage: 1,  // 当前页码
  pageSize: 10,    // 每页显示数量
  total: 0         // 总数据量
})

// 表单数据管理 - 用于创建和编辑机器人
const robotFormRef = ref<FormInstance>()
const robotForm = reactive({
  id: '',           // 机器人ID（编辑时使用）
  name: '',         // 机器人名称
  model: '',        // 机器人型号
  jointCount: 6,    // 关节数量（默认6关节）
  description: ''   // 机器人描述
})

// 表单验证规则 - 确保数据质量和完整性
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

// 计算属性 - 动态生成弹窗标题
const dialogTitle = computed(() => isEditing.value ? '编辑机器人' : '添加机器人')

// Vue Router实例 - 用于页面导航
const router = useRouter()

// ============ 核心业务方法 ============

/**
 * 加载机器人列表数据
 * 实现分页查询和搜索过滤功能，支持根据型号进行模糊搜索
 * 这是用户界面与后端API交互的核心方法
 */
const loadRobots = async () => {
  loading.value = true
  try {
    // 获取完整机器人列表（robotApi.getRobots已做分页参数传递和归一化处理）
    const allRobots = await robotApi.getRobots({
      page: 0,  // 获取所有数据，由前端分页
      size: 1000, // 设置大值获取所有数据
      model: searchModel.value || undefined
    })

    // 前端分页处理
    const startIndex = (pagination.currentPage - 1) * pagination.pageSize
    const endIndex = startIndex + pagination.pageSize

    // 更新分页信息
    pagination.total = allRobots.length
    robots.value = allRobots.slice(startIndex, endIndex)

    // 成功加载后给出视觉反馈（可选）
    if (robots.value.length === 0 && searchModel.value) {
      ElMessage.info('未找到匹配的机器人')
    }
  } catch (error: any) {
    // 错误处理 - 网络错误或服务异常
    console.error('Failed to load robots:', error)
    // 如果是后端错误，robotApi内部已做localStorage fallback，这里只显示用户友好的错误
    if (error?.response?.status !== 401 && error?.response?.status < 500) {
      ElMessage.error('加载机器人列表失败，请检查网络连接')
    }
  } finally {
    // 无论成功还是失败，都要重置加载状态
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

/**
 * 打开创建机器人弹窗
 * 重置表单状态，准备创建新的机器人记录
 */
const openCreateDialog = () => {
  isEditing.value = false  // 设置为创建模式
  resetForm()              // 清空表单数据
  showDialog.value = true  // 显示弹窗
}

/**
 * 打开编辑机器人弹窗
 * 将选中的机器人数据填充到表单中，进行编辑操作
 * @param robot 要编辑的机器人对象
 */
const editRobot = (robot: Robot) => {
  isEditing.value = true   // 设置为编辑模式
  // 将机器人数据填充到表单
  robotForm.id = robot.id
  robotForm.name = robot.name
  robotForm.model = robot.model
  robotForm.jointCount = robot.jointCount
  robotForm.description = robot.description || ''
  showDialog.value = true  // 显示弹窗
}

const viewRobot = (robot: Robot) => {
  // 跳转到机器人详情页
  router.push(`/robots/${robot.id}`)
}

/**
 * 提交机器人表单数据
 * 处理机器人创建和更新操作的核心方法
 * 包含表单验证、API调用、状态管理和错误处理
 */
const submitRobot = async () => {
  // 表单引用检查
  if (!robotFormRef.value) return

  // 客户端表单验证 - 防止无效数据提交
  try {
    await robotFormRef.value.validate()
  } catch (error) {
    // 验证失败，中断提交
    return
  }

  // 设置提交状态，禁用重复提交
  submitting.value = true

  try {
    if (isEditing.value) {
      // 编辑模式：调用更新API
      const updatedRobot = await robotApi.updateRobot(robotForm.id, robotForm)
      ElMessage.success('机器人更新成功')

      // 立即更新表格中的数据
      const robotIndex = robots.value.findIndex(r => r.id === robotForm.id)
      if (robotIndex >= 0) {
        robots.value[robotIndex] = updatedRobot
      }
    } else {
      // 创建模式：调用创建API
      await robotApi.createRobot(robotForm)
      ElMessage.success('机器人创建成功')
    }

    // 操作成功后的清理工作
    showDialog.value = false  // 关闭弹窗
    loadRobots()              // 刷新列表数据（确保全局同步）
  } catch (error: any) {
    // 错误处理 - 显示后端返回的错误信息
    ElMessage.error(error?.response?.data?.message || '操作失败')
    console.error('Robot operation failed:', error)
  } finally {
    // 无论成功还是失败，都要重置提交状态
    submitting.value = false
  }
}

/**
 * 删除指定的机器人
 * 包含用户确认、API调用和状态更新的完整删除流程
 * 使用Element Plus的MessageBox组件提供友好的确认界面
 * @param robot 要删除的机器人对象
 */
const deleteRobot = async (robot: Robot) => {
  try {
    // 用户确认对话框 - 防止误操作
    await ElMessageBox.confirm(
      `确定要删除机器人 "${robot.name}" 吗？此操作不可撤销。`,
      '确认删除',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning',  // 警告样式突出危险操作
      }
    )

    // 用户确认后执行删除操作
    await robotApi.deleteRobot(robot.id)

    // 删除成功反馈
    ElMessage.success('机器人删除成功')

    // 刷新列表数据
    loadRobots()

  } catch (error) {
    // 处理用户取消操作的情况
    if (error !== 'cancel') {
      // 网络错误或其他异常
      ElMessage.error('删除失败，请重试')
      console.error('Robot deletion failed:', error)
    }
    // 如果是用户取消，不显示错误信息
  }
}

const handleStatusChange = async (robotId: string, status: string) => {
  try {
    await robotApi.updateRobotStatus(robotId, { status })
    ElMessage.success('状态更新成功')

    // 立即更新表格中的状态，避免reload造成的延迟感
    const robotIndex = robots.value.findIndex(r => r.id === robotId)
    if (robotIndex >= 0) {
      robots.value[robotIndex] = { ...robots.value[robotIndex], status: status as any }
    }

    // 同时触发全局更新（确保其他页面同步）
    // loadRobots() 会触发，但我们已经手动更新了UI，所以可以省略或延迟调用
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
    default: return status || '未知'
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
  color: var(--el-text-color-primary);
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
