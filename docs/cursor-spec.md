# 工业机器人远程运维“数据—知识双轮驱动”模拟平台：Cursor 全项目开发指令书（Markdown）

# 工业机器人远程运维“数据—知识双轮驱动”模拟平台：Cursor 全项目开发指令书（Markdown）

> 本文档用于直接交给 Cursor 作为“项目级任务说明书/实施蓝图”，按顺序生成完整工程：包含后端（Java/Spring Boot）、前端（HTML/CSS/JS，建议 Vue）、数据库（PostgreSQL）、实时推送（WebSocket）、容器化部署（Docker Compose）、测试与文档。
> 
> 
> 平台范围严格遵循开题报告边界：**仅软件/仿真与回放，不接入真实产线与真实控制器，不做真实机器人控制**；核心闭环为 **“模拟 → 监控 → 预警 → 知识 → 联动评测”**，并内嵌 **认证鉴权、最小权限、加密、限流、审计日志** 等工程基线要求。
> 

---

## 1. 项目目标与交付物

### 1.1 目标（MVP + 可扩展）

必须实现四大模块并打通闭环：

1. **模拟数据生成/回放**：多传感器（电流、振动、温度、位姿等）模拟 + 噪声/故障注入；支持离线回放与准实时推送
2. **远程监控**：监控看板、趋势曲线、对象检索、告警汇聚
3. **故障预警**：异常检测、分级预警、告警去重/抑制、证据聚合
4. **知识库管理**：案例/规则 CRUD、检索、解释型建议输出（“告警→解释→案例→建议”）

非功能：容器化一键启动、可审计、安全基线、功能/性能/体验测试。

### 1.2 最终交付物（代码 + 文档）

- 源码：前后端 + 数据库迁移脚本 + docker-compose
- OpenAPI 接口文档（自动生成）
- 数据库设计（表结构 + 关键索引说明）
- 关键流程说明（模拟→入库→检测→告警→知识建议→前端展示）
- 测试报告模板 + 基础自动化脚本（单测/集成/压力）

---

## 2. 技术栈与总体架构（按开题报告约束落地）

### 2.1 技术栈（建议默认）

- 后端：Java 17 + Spring Boot 3.x（单体应用即可）
    - Spring Web、Spring Validation
    - Spring Data JPA（或 MyBatis 二选一，默认 JPA）
    - Spring Security + JWT
    - WebSocket（STOMP/SockJS 或原生 WS）
    - Flyway（数据库迁移）
    - Actuator（健康检查/指标）
- 数据库：PostgreSQL 15+
- 前端：Vue 3 + Vite + TypeScript（也可纯 HTML/JS，但建议 Vue）
    - UI：Element Plus
    - 图表：ECharts
    - HTTP：axios
- 部署：Docker + Docker Compose（**一键启动**）
- 测试：JUnit5 + Testcontainers（后端）；Playwright（可选前端端到端）；k6/JMeter（性能）

### 2.2 分层架构（必须体现）

- 数据层：仿真数据管道 + Postgres 存储（时序索引/分区）
- 服务层：检测/预警、知识检索与推理、事件编排（告警去重/抑制）
- 接口层：REST 为主，必要时 WebSocket 实时推送；OpenAPI
- 展示层：监控大屏/管理台（告警流 + 详情 + 知识建议）

---

## 3. 仓库结构（Cursor 必须按此生成）

建议 Monorepo：

```
ir-opsim-platform/
  README.md
  docker-compose.yml
  .env.example
  docs/
    architecture.md
    api.md
    db.md
    runbook.md
    test-report-template.md
  backend/
    pom.xml
    src/main/java/com/example/iropsim/...
    src/main/resources/
      application.yml
      db/migration/   # Flyway SQL
  frontend/
    package.json
    vite.config.ts
    src/
      main.ts
      router/
      stores/
      api/
      pages/
      components/
  scripts/
    seed-demo-data.sh
    k6/

```

---

## 4. 核心数据模型（数据库表必须先定，再写代码）

> 设计原则：
> 
> - **时序数据高频写入**：按 `robot_id + ts` 索引；可选按月/按天分区（MVP 可先不分区，但至少索引齐全）
> - **告警事件要可追溯**：去重键、首次/最后出现时间、计数、证据、检测器与分数
> - **知识库结构化**：案例/规则、症状—原因—处置建议，支持检索与版本

### 4.1 业务主表（MVP 必备）

1. `robot`
- `id` UUID PK
- `name` varchar
- `model` varchar
- `joint_count` int default 6
- `created_at` timestamptz
1. `scenario`
- `id` UUID PK
- `name` varchar
- `description` text
- `base_params` jsonb（额定电流/温度范围/噪声水平等）
1. `scenario_run`
- `id` UUID PK
- `scenario_id` UUID FK
- `mode` varchar（REALTIME/REPLAY）
- `seed` bigint（保证可复现）
- `rate_hz` int（采样频率）
- `status` varchar（CREATED/RUNNING/STOPPED/FINISHED）
- `started_at` / `ended_at` timestamptz
1. `joint_sample`（关节级：电流/振动/温度）
- `id` bigserial PK
- `ts` timestamptz not null
- `robot_id` UUID
- `joint_index` int
- `current_a` double precision
- `vibration_rms` double precision
- `temperature_c` double precision
- `scenario_run_id` UUID
- `label` varchar（NORMAL / FAULT_*）
- 索引：`(robot_id, ts)`, `(scenario_run_id, ts)`, `(robot_id, joint_index, ts)`
1. `pose_sample`（机器人位姿，可选但建议做）
- `id` bigserial
- `ts`
- `robot_id`
- `x,y,z,rx,ry,rz` double precision
- `scenario_run_id`, `label`
- 索引：`(robot_id, ts)`
1. `fault_injection`
- `id` UUID
- `scenario_run_id` UUID
- `fault_type` varchar（例如 OVERHEAT / HIGH_VIBRATION / CURRENT_SPIKE / SENSOR_DRIFT）
- `start_ts`, `end_ts` timestamptz
- `params` jsonb（幅值、漂移率、噪声、作用关节等）
1. `alarm_event`
- `id` UUID
- `first_seen_ts`, `last_seen_ts` timestamptz
- `robot_id` UUID
- `joint_index` int nullable（全局告警可空）
- `alarm_type` varchar（例如 TEMP_ANOMALY / VIB_ANOMALY / CURRENT_ANOMALY / POSE_ANOMALY）
- `severity` varchar（INFO/WARN/CRITICAL）
- `status` varchar（OPEN/ACKED/CLOSED）
- `dedup_key` varchar（去重键：robot+joint+type+window）
- `count` int（重复次数）
- `detector` varchar（THRESHOLD/Z_SCORE/IFOREST...）
- `score` double precision
- `evidence` jsonb（窗口统计、阈值、异常点等）
- `scenario_run_id` UUID
- 索引：`(status, last_seen_ts desc)`, `(robot_id, last_seen_ts desc)`, `unique(dedup_key)`（可选）
1. `alarm_ack`
- `id` UUID
- `alarm_id` UUID
- `ack_by` UUID（user id）
- `ack_ts` timestamptz
- `comment` text
1. `kb_case`
- `id` UUID
- `title` varchar
- `fault_type` varchar
- `symptoms` jsonb（症状结构化：指标、阈值、模式）
- `root_cause` text
- `actions` jsonb（建议步骤列表）
- `tags` text[]
- `version` int
- `created_by` UUID
- `created_at`, `updated_at`
1. `kb_rule`（规则驱动：告警→匹配→建议）
- `id` UUID
- `name` varchar
- `enabled` boolean
- `priority` int
- `when_expr` jsonb（建议用 JSONLogic 风格表达式）
- `then_case_id` UUID
- `created_at`, `updated_at`
1. `app_user` / `app_role` / `user_role`（RBAC）
- 用户、角色、多对多
1. `audit_log`
- `id` bigserial
- `ts` timestamptz
- `actor_user_id` UUID nullable
- `action` varchar（LOGIN/CREATE_CASE/ACK_ALARM/...）
- `resource` varchar
- `resource_id` varchar
- `ip` varchar
- `user_agent` varchar
- `detail` jsonb

> Cursor 要求：先写 Flyway 迁移 SQL（V1__init.sql），再写实体与 API，避免“先写代码后补数据库”导致返工。
> 

---

## 5. 后端服务设计（Spring Boot 单体，模块化分包）

### 5.1 包结构（必须按领域分层）

```
backend/src/main/java/com/example/iropsim/
  IROpsimApplication.java
  config/          # security, cors, websocket, jackson, rate-limit
  common/          # error, response wrapper, utils
  auth/            # jwt, login, user, role
  sim/             # simulation & replay
  telemetry/       # samples query
  detection/       # detectors, windowing, scoring
  alarm/           # dedup, suppression, ack, queries
  kb/              # cases/rules/search/recommendation
  audit/           # audit logging

```

### 5.2 模拟数据生成与回放（Sim 模块）

**必须具备：**

- 可配置采样频率 `rate_hz`、机器人数量、关节数、噪声水平
- 故障注入：指定关节、时间范围、故障类型与幅值
- 两种运行模式：
    - REALTIME：按频率持续生成并入库 + 推送
    - REPLAY：从历史 `scenario_run` 或 CSV/JSON 回放（可倍速）

**建议实现接口：**

- `SimulationEngine`：start/stop/status
- `ScenarioRunner`：加载 scenario + fault_injection → 产生样本流
- `SamplePublisher`：把样本发给 detection + websocket
- `ReplayService`：回放 runId

### 5.3 异常检测与预警（Detection 模块）

**MVP 必须实现 2 种：**

1. 阈值法（可解释）：固定阈值/动态阈值
2. 统计法：滑动窗口 z-score / EWMA（可解释）

**可选扩展：**

- Isolation Forest（用 Java ML 库实现，或先留接口）

**统一检测器接口：**

- `Detector<TSample>`：`DetectionResult detect(Window<TSample> w)`
- `DetectionResult`：`isAnomaly, score, severity, evidence`

**窗口化：**

- `WindowBuffer`：按 `(robot_id, joint_index)` 维度维护最近 N 秒样本

### 5.4 告警事件管理（Alarm 模块）

必须实现：

- 分级：INFO/WARN/CRITICAL（基于 score 或超限比例）
- 去重：同类型告警在时间窗口内合并，更新 `last_seen_ts` 与 `count`
- 抑制：短时间告警风暴时可按规则抑制（例如同 robot 同类告警 N 次/分钟）
- ACK：支持确认与备注
- 全链路可追溯：alarm evidence、detector、关联 scenario_run

### 5.5 知识库与建议（KB 模块）

必须实现：

- Case CRUD + 搜索（按 fault_type/tags/关键字）
- Rule CRUD：规则表达式（JSON）匹配 alarm_event
- Recommendation：告警详情页可返回
    - 命中规则
    - 匹配案例
    - 结构化处置建议（steps）
    - 解释（evidence 摘要）

> 注意：MVP 不要引入重型规则引擎（如 Drools）以免工程负担过大；优先 JSON 规则表达式 + 自研轻量解释器。
> 

### 5.6 安全机制（必须内置）

- JWT 登录
- RBAC：ADMIN / OPERATOR / VIEWER
- 最小权限：不同角色不同 API 权限
- 审计日志：对登录、数据修改、ACK 等关键动作写 `audit_log`
- 限流：对写接口做简单 token bucket（可用拦截器 + 内存计数，或基于 Bucket4j）
- 传输加密：本地可 HTTP；生产建议 Nginx TLS（文档写明）

---

## 6. API 设计（REST + WebSocket）

### 6.1 REST（/api/v1）

**Auth**

- `POST /auth/login` → JWT
- `GET /auth/me`

**Sim**

- `POST /sim/runs` 创建 run（scenarioId, mode, seed, rateHz）
- `POST /sim/runs/{id}/start`
- `POST /sim/runs/{id}/stop`
- `GET /sim/runs/{id}` 状态
- `POST /sim/runs/{id}/faults` 添加故障注入

**Telemetry**

- `GET /robots` 列表
- `GET /robots/{id}/latest` 最新指标（各关节）
- `GET /robots/{id}/joints/{jointIndex}/series?metric=current_a&from=&to=&step=...`
- `GET /robots/{id}/pose/series?...`

**Alarms**

- `GET /alarms?status=&severity=&robotId=&from=&to=`
- `GET /alarms/{id}`
- `POST /alarms/{id}/ack`（comment）
- `POST /alarms/{id}/close`（可选）

**KB**

- `GET /kb/cases?keyword=&faultType=&tag=`
- `POST /kb/cases`
- `PUT /kb/cases/{id}`
- `DELETE /kb/cases/{id}`
- `GET /kb/rules`
- `POST /kb/rules`
- `POST /kb/rules/{id}/test`（输入 alarm JSON，返回是否命中）

**Audit**

- `GET /audit?from=&to=&actor=&action=`

> Cursor 要求：使用 Springdoc OpenAPI 自动生成文档，并在 /swagger-ui 可访问。
> 

### 6.2 WebSocket（实时推送）

- WS endpoint：`/ws`
- Topic（示例）：
    - `/topic/robots/{robotId}/latest` 推最新采样点
    - `/topic/alarms` 推新告警/更新
    - `/topic/sim/runs/{runId}/status` 推仿真状态

---

## 7. 前端页面与组件清单（Vue3）

### 7.1 页面（pages）

1. `Login`：登录获取 JWT
2. `Dashboard`：全局概览（运行中的 run、告警数量、机器人列表）
3. `RobotDetail`：
    - 关节选择
    - 电流/振动/温度趋势图（ECharts）
    - 最新值卡片
4. `AlarmCenter`：告警列表（筛选/排序/ACK）
5. `AlarmDetail`：告警证据 + 推荐处置建议（知识库输出）
6. `KnowledgeCases`：案例 CRUD（表单、标签）
7. `KnowledgeRules`：规则 CRUD + Test 工具
8. `System`：用户/角色（MVP 可只读或只做 Admin 的最简管理）
9. `AuditLog`：审计日志查询

### 7.2 组件（components）

- `MetricChart.vue`：统一折线图组件
- `AlarmTable.vue`
- `RecommendationPanel.vue`
- `RunControlPanel.vue`：启动/停止/注入故障

### 7.3 前端工程要求

- axios 拦截器自动带 JWT
- 路由守卫（未登录跳转 Login）
- WebSocket 订阅（切换 robot 自动取消旧订阅）

---

## 8. Docker Compose（一键启动是硬指标）

`docker-compose.yml` 必须包含：

- `postgres`（持久化 volume）
- `backend`（暴露 8080）
- `frontend`（nginx 或 vite preview，建议 nginx，暴露 80）
- 可选：`pgadmin`（便于演示）

后端必须支持环境变量配置（`SPRING_DATASOURCE_URL` 等），并提供 `healthcheck`：

- `GET /actuator/health`

---

## 9. 测试与验收标准（DoD）

### 9.1 功能测试（必须）

- 仿真 run 启动后：前端能看到实时曲线刷新
- 注入故障后：在合理时间窗口内产生告警（阈值/统计检测）
- 告警可 ACK，状态变更写入审计日志
- 告警详情能返回知识建议（至少命中 1 条规则或检索到 1 个案例）

### 9.2 性能测试（至少给出可运行脚本）

- 目标示例：单机 1~5 个机器人、6 关节、1~5Hz 数据写入，系统持续运行 30 分钟无异常
- k6/JMeter：压测告警列表查询、时序查询接口

### 9.3 安全与可审计（必须）

- 未登录访问写接口：401
- VIEWER 不能创建/删除 KB
- 关键操作有 audit_log 记录（login、create/update/delete case、ack alarm）

---

## 10. Cursor 逐步生成代码的“可复制提示词”（按顺序执行）

> 说明：下面每一步都给 Cursor 一段提示词，让它在你的工作区生成/修改代码。你要做的是：严格按顺序执行，确保每步可编译可运行，再进入下一步。
> 

---

### Step 0：初始化仓库与基础文件

将以下提示词发给 Cursor：

```
请在当前工作区创建一个 monorepo：ir-opsim-platform，包含 backend（Spring Boot 3 + Java17 + Maven）、frontend（Vue3+Vite+TS）、docs、scripts。生成 README.md、.env.example、docker-compose.yml 的空壳，以及标准 .gitignore、.editorconfig。不要生成业务代码，先把目录结构搭好。

```

验收：目录结构与基础文件完整。

---

### Step 1：后端骨架（可启动 + Actuator + OpenAPI）

```
在 backend 中创建 Spring Boot 3.x 项目（Java17，Maven）。加入依赖：spring-boot-starter-web、validation、data-jpa、security、websocket、actuator、postgresql、flyway、springdoc-openapi-starter-webmvc-ui、lombok。提供 application.yml（支持环境变量覆盖）。实现 /actuator/health 可用；Swagger UI 可用；提供统一异常返回结构（error code/message）。

```

验收：`mvn test` 通过；`mvn spring-boot:run` 启动成功；`/swagger-ui` 可访问。

---

### Step 2：数据库迁移（Flyway）+ 实体映射

```
根据以下表设计写 Flyway 迁移：robot、scenario、scenario_run、joint_sample、pose_sample、fault_injection、alarm_event、alarm_ack、kb_case、kb_rule、app_user、app_role、user_role、audit_log。为 joint_sample、pose_sample、alarm_event 创建关键索引。然后用 JPA 实体与 Repository 映射这些表。注意 joint_sample/pose_sample 是高频表，查询需要按 robot_id + ts 范围高效。

```

验收：应用启动时自动建表；可通过简单 Repository 保存/查询。

---

### Step 3：认证鉴权（JWT + RBAC）+ 审计日志

```
实现 JWT 登录与 RBAC：角色 ADMIN/OPERATOR/VIEWER。提供 POST /api/v1/auth/login 与 GET /api/v1/auth/me。写一个初始化数据：启动时若无用户则创建 admin/admin123（仅开发环境）。对关键接口加权限控制。实现审计日志 AuditLogService：登录、创建/修改/删除 KB、ACK 告警等动作写入 audit_log（包含 actor、ip、user_agent、detail）。

```

验收：未登录 401；不同角色权限生效；审计表有记录。

---

### Step 4：仿真引擎（REALTIME）—生成并入库

```
实现仿真模块 sim：POST /api/v1/sim/runs 创建 scenario_run；POST /api/v1/sim/runs/{id}/start 启动一个后台任务（ScheduledExecutorService），按 rate_hz 生成 joint_sample（电流/振动/温度）与 pose_sample（位姿）并写入数据库。必须支持 seed 使数据可复现。实现 stop 接口停止任务。实现 fault_injection：可对某 joint 在时间窗口内注入故障（例如温度升高、振动升高、电流尖峰、传感漂移）。

```

验收：启动 run 后数据库持续写入样本；停止后写入停止。

---

### Step 5：实时推送（WebSocket）+ 最新值接口

```
实现 WebSocket /ws，并在生成每个样本时向 topic 推送：/topic/robots/{robotId}/latest 与 /topic/sim/runs/{runId}/status。实现 GET /api/v1/robots 与 GET /api/v1/robots/{id}/latest（返回每个关节最新电流/振动/温度与 pose 最新值）。注意并发与序列化性能。

```

验收：WebSocket 客户端能收到实时数据；latest 接口返回正确。

---

### Step 6：异常检测（阈值 + z-score）→ 告警事件（去重/分级）

```
实现 detection 模块：按 robot_id+joint_index 维护滑动窗口（例如最近 60 秒）。实现两种 Detector：THRESHOLD（可配置阈值）与 Z_SCORE（窗口均值方差）。当异常触发，生成/更新 alarm_event：按 dedup_key 去重，更新 last_seen_ts 与 count；按 score 映射 severity；写入 evidence（阈值、z 值、窗口统计）。实现告警推送 /topic/alarms（新建/更新都推）。

```

验收：注入故障后出现告警；告警会合并计数；前端能实时看到告警更新（后续 Step 9 才做前端）。

---

### Step 7：告警 API（列表/详情/ACK）+ 抑制策略（最小可用）

```
实现 /api/v1/alarms：列表查询（支持 status/severity/robotId/time range）、详情查询、ACK（写 alarm_ack 并更新 alarm_event status）。实现一个最小抑制策略：同 robot 同 alarm_type 在 60 秒内超过 N 次更新时，标记 evidence.suppressed=true（或写 suppression 字段），并在推送中体现。

```

验收：告警可查询、可 ACK，审计日志记录 ACK。

---

### Step 8：知识库（Case/Rule）+ 建议生成（Recommendation）

```
实现 KB：/api/v1/kb/cases 与 /api/v1/kb/rules 全套 CRUD。规则表达式采用 JSON（类似 JSONLogic）：当告警满足 when_expr 条件时，返回 then_case_id。实现 RecommendationService：输入 alarmId，输出命中规则、匹配案例详情、actions 建议步骤、解释（基于 alarm evidence）。提供 GET /api/v1/alarms/{id}/recommendation。

```

验收：创建规则与案例后，告警详情能返回建议。

---

### Step 9：前端骨架（登录 + Dashboard + WebSocket 订阅）

```
在 frontend 创建 Vue3+Vite+TS 工程，引入 element-plus、axios、pinia、echarts、stompjs+sockjs（或原生 ws）。实现登录页、JWT 存储、路由守卫。实现 Dashboard：展示 robots 列表与当前 run 状态，并订阅 /topic/alarms 显示最新告警计数。

```

验收：能登录、能看到机器人列表、能实时看到告警计数变化。

---

### Step 10：监控曲线 + 告警中心 + 知识建议页

```
实现 RobotDetail：订阅 /topic/robots/{robotId}/latest，绘制电流/振动/温度趋势（ECharts，保留最近 N 分钟点）。实现 AlarmCenter（筛选、ACK）。实现 AlarmDetail：展示 evidence，并调用 recommendation 接口展示处置建议（步骤列表）。

```

验收：曲线实时刷新；告警可 ACK；详情能看到建议。

---

### Step 11：Docker Compose 联调

```
完善 docker-compose：postgres + backend + frontend（nginx）。后端通过环境变量连 postgres；前端反代到后端 /api 与 /ws。提供一条命令 docker compose up -d 即可运行。补充 README 运行说明与默认账号。

```

验收：全新机器拉取后能一键启动并演示闭环。

---

### Step 12：测试与压测脚本

```
为后端增加：Detector 单元测试、告警去重逻辑测试、KB 规则匹配测试；增加 Testcontainers 集成测试（至少覆盖 alarms 列表与 ack）。在 scripts/k6 写一个基础压测脚本：时序查询与告警列表查询。输出 docs/test-report-template.md。

```

验收：测试可运行；压测脚本可执行。

---

## 11. 代码规范与工程约束（Cursor 必须遵守）

- 所有 REST 返回统一结构：`{ code, message, data, traceId }`
- 所有时间统一 `timestamptz`，API 统一 ISO8601
- 高风险操作（删除 KB、ACK 告警）必须写审计
- 样本写入与检测解耦：生成样本后通过内存队列/发布器分发（MVP 用内存即可）
- 任何“可选扩展”（Isolation Forest、分区、全文检索优化）必须留接口，不阻塞 MVP

---

## 12. 你接下来怎么用这份文档

1. 将本 Markdown 保存为 `docs/cursor-spec.md`
2. 按 **Step 0 → Step 12** 顺序，把每一步提示词粘贴给 Cursor 执行
3. 每步完成后先本地运行验证，再进入下一步（避免累计错误）

---

如果你希望我进一步把 **阈值策略、z-score 窗口参数、告警 dedup_key 规则、规则表达式 JSON 模板、以及前端 ECharts 数据结构**也一并写成“可直接复制的配置与样例”，我可以在下一条消息直接补齐一份 `docs/examples.md`（包含可运行的示例 scenario 与故障注入模板）。