# 工业机器人远程运维"数据—知识双轮驱动"模拟平台

## 🚀 项目简介

本项目是一个**企业级工业机器人远程运维模拟平台**，采用"**数据—知识双轮驱动**"的设计理念，实现机器人状态监控、异常检测、故障预警和知识库驱动的智能运维闭环。

### ✨ 核心特性

- **🎯 智能仿真**: 多传感器数据生成、故障注入、离线回放
- **📊 实时监控**: 动态监控看板、趋势曲线、告警汇聚
- **⚠️ 异常检测**: 多算法异常检测、分级预警、告警抑制
- **🧠 知识库**: 案例库管理、规则引擎、智能处置建议
- **🔐 安全可靠**: JWT认证、RBAC权限、审计日志

## 🛠️ 技术栈

| 组件 | 技术栈 | 版本 |
|------|--------|------|
| **后端** | Java + Spring Boot | 17 + 3.x |
| **数据库** | PostgreSQL | 15 |
| **前端** | Vue.js + TypeScript | 3 + 5.x |
| **UI框架** | Element Plus | 2.x |
| **图表库** | ECharts | 5.x |
| **通信** | WebSocket + STOMP | - |
| **部署** | Docker + Docker Compose | - |

## 🎯 快速启动

### 📋 环境要求

- ✅ Docker & Docker Compose (推荐)
- ✅ Java 17 (开发环境可选)
- ✅ Node.js 18+ (开发环境可选)

### 🚀 一键部署

```bash
# 1. 克隆项目
git clone <repository-url>
cd ir-opsim-platform

# 2. 启动所有服务
docker compose up -d

# 3. 等待服务启动 (约2-3分钟)
docker compose logs -f

# 4. 访问应用
# 前端界面: http://localhost
# 后端API文档: http://localhost:8080/swagger-ui
# 数据库管理: http://localhost:5050 (可选)
```

### 👤 默认账号

| 用户名 | 密码 | 角色 |
|--------|------|------|
| `admin` | `admin123` | 系统管理员 |

## 📖 使用指南

### 1. 系统概览

启动后访问 `http://localhost`，使用默认账号登录：

- **Dashboard**: 系统概览、实时统计、机器人状态
- **机器人监控**: 选择机器人查看实时曲线和状态
- **告警中心**: 查看和管理所有告警信息
- **知识库**: 管理系统知识和规则（管理员权限）

### 2. 仿真演示

1. 进入Dashboard，查看当前机器人状态
2. 点击机器人卡片进入详情页
3. 在详情页可以看到实时监控曲线
4. 系统会自动检测异常并生成告警
5. 查看告警详情获取智能处置建议

### 3. 高级功能

- **故障注入**: 通过API注入模拟故障测试检测能力
- **知识库管理**: 添加案例和规则提升智能水平
- **审计日志**: 查看所有系统操作记录

## 🏗️ 项目架构

```
ir-opsim-platform/
├── backend/                 # Spring Boot后端服务
│   ├── src/main/java/com/example/iropsim/
│   │   ├── auth/           # 认证授权
│   │   ├── common/         # 公共组件
│   │   ├── config/         # 配置类
│   │   ├── controller/     # REST控制器
│   │   ├── entity/         # JPA实体
│   │   ├── repository/     # 数据访问层
│   │   ├── sim/            # 仿真引擎
│   │   ├── detection/      # 异常检测
│   │   ├── alarm/          # 告警管理
│   │   ├── kb/             # 知识库
│   │   ├── telemetry/      # 遥测数据
│   │   └── websocket/      # WebSocket服务
│   └── src/main/resources/ # 配置文件
├── frontend/               # Vue3前端应用
│   ├── src/
│   │   ├── api/           # API服务层
│   │   ├── components/    # 组件
│   │   ├── pages/         # 页面
│   │   ├── router/        # 路由配置
│   │   ├── stores/        # Pinia状态管理
│   │   └── types/         # TypeScript类型
│   ├── public/            # 静态资源
│   └── dist/              # 构建输出
├── docs/                  # 项目文档
├── docker-compose.yml     # 容器编排配置
└── README.md
```

## 🔧 开发环境

### 后端开发

```bash
cd backend
# 安装依赖
mvn clean install
# 启动服务
mvn spring-boot:run
```

### 前端开发

```bash
cd frontend
# 安装依赖
npm install
# 启动开发服务器
npm run dev
```

## 📚 API 文档

启动服务后，可通过以下地址访问：

- **Swagger UI**: http://localhost:8080/swagger-ui
- **OpenAPI 规范**: http://localhost:8080/v3/api-docs
- **健康检查**: http://localhost:8080/actuator/health

### 主要API接口

| 模块 | 接口路径 | 说明 |
|------|----------|------|
| 认证 | `/api/v1/auth/*` | 登录、用户信息 |
| 机器人 | `/api/v1/robots/*` | 机器人管理 |
| 遥测 | `/api/v1/robots/*/latest` | 传感器数据 |
| 仿真 | `/api/v1/sim/*` | 仿真运行管理 |
| 告警 | `/api/v1/alarms/*` | 告警查询和管理 |
| 知识库 | `/api/v1/kb/*` | 案例和规则管理 |

## 🧪 测试

### 单元测试

```bash
# 后端测试
cd backend
mvn test

# 前端测试
cd frontend
npm run test:unit
```

### 集成测试

```bash
# 使用Docker Compose进行集成测试
docker compose -f docker-compose.test.yml up --abort-on-container-exit
```

### 性能测试

```bash
# 安装k6后运行性能测试
cd scripts/k6
k6 run performance-test.js
```

## 🔍 故障排除

### 常见问题

1. **服务启动失败**
   ```bash
   # 查看服务日志
   docker compose logs backend
   docker compose logs frontend
   ```

2. **数据库连接问题**
   ```bash
   # 检查数据库状态
   docker compose exec postgres pg_isready -U iropsim -d iropsim
   ```

3. **前端无法访问后端**
   - 确认后端健康检查通过：`curl http://localhost:8080/actuator/health`
   - 检查防火墙和端口配置

4. **WebSocket连接失败**
   - 确认后端WebSocket端点 `/ws` 可用
   - 检查浏览器控制台错误信息

### 日志查看

```bash
# 查看所有服务日志
docker compose logs -f

# 查看特定服务日志
docker compose logs -f backend
docker compose logs -f frontend
```

## 🤝 贡献指南

1. Fork 项目
2. 创建功能分支：`git checkout -b feature/amazing-feature`
3. 提交更改：`git commit -m 'Add amazing feature'`
4. 推送分支：`git push origin feature/amazing-feature`
5. 创建 Pull Request

## 📄 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情

## 📞 联系我们

- 项目维护者：Development Team
- 技术支持：support@iropsim.com

---

**🎉 祝您使用愉快！如有问题请查看故障排除指南或提交Issue。**
