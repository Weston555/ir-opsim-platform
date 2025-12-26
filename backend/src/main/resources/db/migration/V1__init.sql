-- Industrial Robot Operation Simulation Platform Database Schema
-- Version: 1.0.0

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Robot table
CREATE TABLE robot (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    model VARCHAR(255) NOT NULL,
    joint_count INTEGER NOT NULL DEFAULT 6,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Scenario table
CREATE TABLE scenario (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    base_params JSONB,
    UNIQUE(name)
);

-- Scenario run table
CREATE TABLE scenario_run (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    scenario_id UUID NOT NULL REFERENCES scenario(id),
    mode VARCHAR(50) NOT NULL CHECK (mode IN ('REALTIME', 'REPLAY')),
    seed BIGINT NOT NULL,
    rate_hz INTEGER NOT NULL DEFAULT 1,
    status VARCHAR(50) NOT NULL DEFAULT 'CREATED' CHECK (status IN ('CREATED', 'RUNNING', 'STOPPED', 'FINISHED')),
    started_at TIMESTAMPTZ,
    ended_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Joint sample table (high-frequency sensor data)
CREATE TABLE joint_sample (
    id BIGSERIAL PRIMARY KEY,
    ts TIMESTAMPTZ NOT NULL,
    robot_id UUID NOT NULL REFERENCES robot(id),
    joint_index INTEGER NOT NULL CHECK (joint_index >= 0),
    current_a DOUBLE PRECISION,
    vibration_rms DOUBLE PRECISION,
    temperature_c DOUBLE PRECISION,
    scenario_run_id UUID REFERENCES scenario_run(id),
    label VARCHAR(50) CHECK (label IS NULL OR label IN ('NORMAL', 'FAULT_OVERHEAT', 'FAULT_HIGH_VIBRATION', 'FAULT_CURRENT_SPIKE', 'FAULT_SENSOR_DRIFT'))
);

-- Pose sample table (robot pose data)
CREATE TABLE pose_sample (
    id BIGSERIAL PRIMARY KEY,
    ts TIMESTAMPTZ NOT NULL,
    robot_id UUID NOT NULL REFERENCES robot(id),
    x DOUBLE PRECISION,
    y DOUBLE PRECISION,
    z DOUBLE PRECISION,
    rx DOUBLE PRECISION,
    ry DOUBLE PRECISION,
    rz DOUBLE PRECISION,
    scenario_run_id UUID REFERENCES scenario_run(id),
    label VARCHAR(50) CHECK (label IS NULL OR label IN ('NORMAL', 'FAULT_OVERHEAT', 'FAULT_HIGH_VIBRATION', 'FAULT_CURRENT_SPIKE', 'FAULT_SENSOR_DRIFT'))
);

-- Fault injection table
CREATE TABLE fault_injection (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    scenario_run_id UUID NOT NULL REFERENCES scenario_run(id),
    fault_type VARCHAR(100) NOT NULL CHECK (fault_type IN ('OVERHEAT', 'HIGH_VIBRATION', 'CURRENT_SPIKE', 'SENSOR_DRIFT')),
    start_ts TIMESTAMPTZ NOT NULL,
    end_ts TIMESTAMPTZ NOT NULL,
    params JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CHECK (end_ts > start_ts)
);

-- Alarm event table
CREATE TABLE alarm_event (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    first_seen_ts TIMESTAMPTZ NOT NULL,
    last_seen_ts TIMESTAMPTZ NOT NULL,
    robot_id UUID NOT NULL REFERENCES robot(id),
    joint_index INTEGER CHECK (joint_index IS NULL OR joint_index >= 0),
    alarm_type VARCHAR(100) NOT NULL CHECK (alarm_type IN ('TEMP_ANOMALY', 'VIB_ANOMALY', 'CURRENT_ANOMALY', 'POSE_ANOMALY')),
    severity VARCHAR(50) NOT NULL CHECK (severity IN ('INFO', 'WARN', 'CRITICAL')),
    status VARCHAR(50) NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN', 'ACKED', 'CLOSED')),
    dedup_key VARCHAR(500) NOT NULL,
    count INTEGER NOT NULL DEFAULT 1,
    detector VARCHAR(100) NOT NULL,
    score DOUBLE PRECISION NOT NULL,
    evidence JSONB,
    scenario_run_id UUID REFERENCES scenario_run(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(dedup_key)
);

-- Alarm acknowledgment table
CREATE TABLE alarm_ack (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    alarm_id UUID NOT NULL REFERENCES alarm_event(id),
    ack_by UUID NOT NULL, -- Will reference app_user(id)
    ack_ts TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    comment TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Knowledge base case table
CREATE TABLE kb_case (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    title VARCHAR(500) NOT NULL,
    fault_type VARCHAR(100) NOT NULL,
    symptoms JSONB,
    root_cause TEXT NOT NULL,
    actions JSONB NOT NULL,
    tags TEXT[],
    version INTEGER NOT NULL DEFAULT 1,
    created_by UUID NOT NULL, -- Will reference app_user(id)
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Knowledge base rule table
CREATE TABLE kb_rule (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT true,
    priority INTEGER NOT NULL DEFAULT 0,
    when_expr JSONB NOT NULL,
    then_case_id UUID NOT NULL REFERENCES kb_case(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Fault injection template table
CREATE TABLE fault_template (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    fault_type VARCHAR(50) NOT NULL,
    params JSONB NOT NULL,
    duration_seconds INTEGER NOT NULL DEFAULT 30,
    severity VARCHAR(20) NOT NULL DEFAULT 'WARN',
    tags TEXT[],
    enabled BOOLEAN NOT NULL DEFAULT true,
    created_by UUID NOT NULL, -- Will reference app_user(id)
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Application user table
CREATE TABLE app_user (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    username VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    full_name VARCHAR(255),
    enabled BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Application role table
CREATE TABLE app_role (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- User role relationship table
CREATE TABLE user_role (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES app_user(id),
    role_id UUID NOT NULL REFERENCES app_role(id),
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    assigned_by UUID REFERENCES app_user(id),
    UNIQUE(user_id, role_id)
);

-- Audit log table
CREATE TABLE audit_log (
    id BIGSERIAL PRIMARY KEY,
    ts TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actor_user_id UUID REFERENCES app_user(id),
    action VARCHAR(100) NOT NULL,
    resource VARCHAR(100) NOT NULL,
    resource_id VARCHAR(255),
    ip VARCHAR(45),
    user_agent TEXT,
    detail JSONB
);

-- Update foreign key references for alarm_ack and kb_case
ALTER TABLE alarm_ack ADD CONSTRAINT fk_alarm_ack_user FOREIGN KEY (ack_by) REFERENCES app_user(id);
ALTER TABLE kb_case ADD CONSTRAINT fk_kb_case_user FOREIGN KEY (created_by) REFERENCES app_user(id);
ALTER TABLE fault_template ADD CONSTRAINT fk_fault_template_user FOREIGN KEY (created_by) REFERENCES app_user(id);

-- Create indexes for high-frequency queries

-- Joint sample indexes (high-frequency time-series queries)
CREATE INDEX idx_joint_sample_robot_ts ON joint_sample(robot_id, ts DESC);
CREATE INDEX idx_joint_sample_scenario_ts ON joint_sample(scenario_run_id, ts DESC);
CREATE INDEX idx_joint_sample_robot_joint_ts ON joint_sample(robot_id, joint_index, ts DESC);

-- Pose sample indexes
CREATE INDEX idx_pose_sample_robot_ts ON pose_sample(robot_id, ts DESC);
CREATE INDEX idx_pose_sample_scenario_ts ON pose_sample(scenario_run_id, ts DESC);

-- Alarm event indexes
CREATE INDEX idx_alarm_event_status_last_seen ON alarm_event(status, last_seen_ts DESC);
CREATE INDEX idx_alarm_event_robot_last_seen ON alarm_event(robot_id, last_seen_ts DESC);
CREATE INDEX idx_alarm_event_dedup_key ON alarm_event(dedup_key);

-- Audit log indexes
CREATE INDEX idx_audit_log_ts ON audit_log(ts DESC);
CREATE INDEX idx_audit_log_actor ON audit_log(actor_user_id, ts DESC);
CREATE INDEX idx_audit_log_action ON audit_log(action, ts DESC);

-- Insert default roles
INSERT INTO app_role (name, description) VALUES
('ADMIN', 'System administrator with full access'),
('OPERATOR', 'Operations user with monitoring and management access'),
('VIEWER', 'Read-only user with view access');

-- Insert default admin user (password: admin123)
-- Note: In production, use proper password hashing
INSERT INTO app_user (username, password_hash, email, full_name) VALUES
('admin', '{noop}admin123', 'admin@iropsim.com', 'System Administrator');

-- Assign admin role to admin user
INSERT INTO user_role (user_id, role_id) VALUES
((SELECT id FROM app_user WHERE username = 'admin'),
 (SELECT id FROM app_role WHERE name = 'ADMIN'));

-- Insert sample robot
INSERT INTO robot (name, model, joint_count) VALUES
('Robot-001', 'IRB-6700', 6),
('Robot-002', 'IRB-4600', 6);

-- Insert sample scenario
INSERT INTO scenario (name, description, base_params) VALUES
('Normal Operation', 'Normal robot operation scenario', '{"current_nominal": 2.5, "temp_nominal": 40.0, "vibration_nominal": 0.1}'),
('Heavy Load', 'Heavy load operation scenario', '{"current_nominal": 4.0, "temp_nominal": 55.0, "vibration_nominal": 0.3}'),
('Maintenance Mode', 'Maintenance operation scenario', '{"current_nominal": 1.0, "temp_nominal": 35.0, "vibration_nominal": 0.05}');

-- Insert knowledge base cases
INSERT INTO kb_case (title, fault_type, root_cause, symptoms, actions, created_by, created_at) VALUES
('电机过热故障', 'OVERHEAT', '电机轴承润滑不足导致摩擦发热', '["电机温度异常升高", "电流略有增加", "振动幅度增大"]', '["检查电机轴承润滑油位", "清理电机散热片灰尘", "检查风扇工作状态", "必要时更换轴承"]', (SELECT id FROM app_user WHERE username = 'admin'), NOW()),
('减速器振动异常', 'HIGH_VIBRATION', '减速器齿轮磨损严重', '["关节处振动加剧", "运行时发出异常噪音", "位置精度下降"]', '["检查减速器油位和油质", "检测齿轮啮合间隙", "检查紧固螺栓是否松动", "必要时更换齿轮"]', (SELECT id FROM app_user WHERE username = 'admin'), NOW()),
('驱动器电流尖峰', 'CURRENT_SPIKE', '电机卡死或负载突变', '["电流突然大幅上升", "电机无法正常运转", "控制系统报错"]', '["检查电机机械连接", "验证负载是否正常", "检查驱动器参数设置", "重启控制系统"]', (SELECT id FROM app_user WHERE username = 'admin'), NOW()),
('传感器漂移故障', 'SENSOR_DRIFT', '传感器长期使用老化', '["位置反馈逐渐不准", "控制精度下降", "温度/电流数值异常"]', '["校准传感器零点", "检查信号线连接", "更换老化传感器", "更新控制参数"]', (SELECT id FROM app_user WHERE username = 'admin'), NOW()),
('轴承磨损故障', 'HIGH_VIBRATION', '关节轴承长期磨损', '["特定关节振动异常", "运行噪音增大", "定位精度下降"]', '["检查轴承润滑状态", "测量轴承间隙", "更换磨损轴承", "调整关节间隙"]', (SELECT id FROM app_user WHERE username = 'admin'), NOW()),
('控制卡故障', 'CURRENT_SPIKE', '控制卡硬件损坏', '["电流控制异常", "位置控制失准", "系统频繁重启"]', '["检查控制卡供电", "更换控制卡", "更新固件版本", "重新标定系统"]', (SELECT id FROM app_user WHERE username = 'admin'), NOW()),
('编码器故障', 'SENSOR_DRIFT', '编码器信号丢失或异常', '["位置反馈中断", "电机运行不稳", "系统报编码器错误"]', '["检查编码器线缆连接", "测试编码器信号", "更换故障编码器", "重新初始化系统"]', (SELECT id FROM app_user WHERE username = 'admin'), NOW()),
('电源模块故障', 'OVERHEAT', '电源模块散热不良', '["电源温度过高", "输出电压不稳", "系统运行异常"]', '["清理电源模块散热片", "检查风扇工作状态", "更换电源模块", "检查供电线路"]', (SELECT id FROM app_user WHERE username = 'admin'), NOW());

-- Insert knowledge base rules
INSERT INTO kb_rule (name, description, priority, when_expr, then_case_id, enabled, created_by, created_at) VALUES
('温度异常规则', '检测电机温度异常的规则', 1, '{"and": [{"field": "alarmType", "op": "equals", "value": "TEMP_ANOMALY"}, {"field": "severity", "op": "greater_than", "value": "INFO"}]}', (SELECT id FROM kb_case WHERE title = '电机过热故障'), true, (SELECT id FROM app_user WHERE username = 'admin'), NOW()),
('振动异常规则', '检测关节振动异常的规则', 1, '{"and": [{"field": "alarmType", "op": "equals", "value": "VIB_ANOMALY"}, {"field": "severity", "op": "greater_than", "value": "INFO"}]}', (SELECT id FROM kb_case WHERE title = '减速器振动异常'), true, (SELECT id FROM app_user WHERE username = 'admin'), NOW()),
('电流尖峰规则', '检测电流异常尖峰的规则', 2, '{"and": [{"field": "alarmType", "op": "equals", "value": "CURRENT_ANOMALY"}, {"field": "severity", "op": "equals", "value": "CRITICAL"}]}', (SELECT id FROM kb_case WHERE title = '驱动器电流尖峰'), true, (SELECT id FROM app_user WHERE username = 'admin'), NOW()),
('传感器漂移规则', '检测传感器信号漂移的规则', 2, '{"and": [{"field": "alarmType", "op": "equals", "value": "UNKNOWN_ANOMALY"}, {"field": "score", "op": "greater_than", "value": 2.0}]}', (SELECT id FROM kb_case WHERE title = '传感器漂移故障'), true, (SELECT id FROM app_user WHERE username = 'admin'), NOW()),
('轴承磨损规则', '检测轴承磨损导致振动的规则', 3, '{"and": [{"field": "alarmType", "op": "equals", "value": "VIB_ANOMALY"}, {"field": "jointIndex", "op": "greater_than", "value": -1}]}', (SELECT id FROM kb_case WHERE title = '轴承磨损故障'), true, (SELECT id FROM app_user WHERE username = 'admin'), NOW()),
('电源故障规则', '检测电源模块故障的规则', 1, '{"and": [{"field": "alarmType", "op": "equals", "value": "TEMP_ANOMALY"}, {"field": "score", "op": "greater_than", "value": 3.0}]}', (SELECT id FROM kb_case WHERE title = '电源模块故障'), true, (SELECT id FROM app_user WHERE username = 'admin'), NOW());

-- Insert fault injection templates
-- Insert fault templates with realistic industrial robot faults
INSERT INTO fault_template (name, description, fault_type, params, duration_seconds, severity, tags, enabled, created_by, created_at) VALUES
('电机轴承磨损', '电机轴承长期运行导致磨损，表现为电流和振动异常增加', 'OVERHEAT', '{"amplitude": 5.0, "jointIndex": 0}', 180, 'WARN', '{"电机", "轴承", "磨损"}', true, (SELECT id FROM app_user WHERE username = 'admin'), NOW()),
('减速器齿轮故障', '减速器齿轮啮合不良，振动显著增加，伴随电流波动', 'HIGH_VIBRATION', '{"amplitude": 0.15, "jointIndex": 1}', 120, 'CRITICAL', '{"减速器", "齿轮", "振动"}', true, (SELECT id FROM app_user WHERE username = 'admin'), NOW()),
('伺服驱动器过载', '伺服驱动器过载运行，电流急剧上升，可能导致过热', 'CURRENT_SPIKE', '{"amplitude": 8.0, "jointIndex": 2}', 60, 'CRITICAL', '{"驱动器", "过载", "电流"}', true, (SELECT id FROM app_user WHERE username = 'admin'), NOW()),
('编码器信号漂移', '编码器信号逐渐漂移，导致位置控制精度下降', 'SENSOR_DRIFT', '{"amplitude": 0.02, "driftRate": 0.001, "jointIndex": 0}', 300, 'WARN', '{"编码器", "位置", "精度"}', true, (SELECT id FROM app_user WHERE username = 'admin'), NOW()),
('机械臂谐振', '机械臂在特定频率下发生谐振，振动大幅增加', 'HIGH_VIBRATION', '{"amplitude": 0.25, "jointIndex": 3}', 90, 'WARN', '{"谐振", "机械臂", "频率"}', true, (SELECT id FROM app_user WHERE username = 'admin'), NOW()),
('电源模块老化', '电源模块老化导致输出电压不稳，电流波动增加', 'CURRENT_SPIKE', '{"amplitude": 3.5, "jointIndex": 4}', 240, 'WARN', '{"电源", "电压", "老化"}', true, (SELECT id FROM app_user WHERE username = 'admin'), NOW()),
('关节间隙过大', '关节连接间隙增大导致定位精度下降和振动', 'HIGH_VIBRATION', '{"amplitude": 0.12, "jointIndex": 1}', 150, 'INFO', '{"关节", "间隙", "精度"}', true, (SELECT id FROM app_user WHERE username = 'admin'), NOW()),
('控制卡通信故障', '控制卡通信异常导致指令执行不稳定', 'CURRENT_SPIKE', '{"amplitude": 2.0, "jointIndex": 5}', 45, 'CRITICAL', '{"控制卡", "通信", "指令"}', true, (SELECT id FROM app_user WHERE username = 'admin'), NOW());
('轻度电机过热', '电机温度缓慢上升，轻度故障', 'OVERHEAT', '{"amplitude": 10.0, "jointIndex": 0}', 60, 'WARN', ARRAY['电机', '温度', '轻度'], true, (SELECT id FROM app_user WHERE username = 'admin'), NOW()),
('重度电机过热', '电机温度快速上升，重度故障', 'OVERHEAT', '{"amplitude": 25.0, "jointIndex": 0}', 120, 'CRITICAL', ARRAY['电机', '温度', '重度'], true, (SELECT id FROM app_user WHERE username = 'admin'), NOW()),
('关节振动异常', '特定关节出现振动异常', 'HIGH_VIBRATION', '{"amplitude": 2.5, "jointIndex": 2}', 90, 'WARN', ARRAY['振动', '关节', '轴承'], true, (SELECT id FROM app_user WHERE username = 'admin'), NOW()),
('多关节振动', '多个关节同时出现振动异常', 'HIGH_VIBRATION', '{"amplitude": 1.8, "jointIndex": -1}', 180, 'CRITICAL', ARRAY['振动', '多关节', '严重'], true, (SELECT id FROM app_user WHERE username = 'admin'), NOW()),
('电流尖峰故障', '驱动器电流突然出现尖峰', 'CURRENT_SPIKE', '{"amplitude": 3.0, "jointIndex": 1}', 30, 'CRITICAL', ARRAY['电流', '驱动器', '尖峰'], true, (SELECT id FROM app_user WHERE username = 'admin'), NOW()),
('传感器轻度漂移', '位置传感器信号轻度漂移', 'SENSOR_DRIFT', '{"amplitude": 0.02, "driftRate": 0.005, "jointIndex": 3}', 300, 'INFO', ARRAY['传感器', '漂移', '轻度'], true, (SELECT id FROM app_user WHERE username = 'admin'), NOW()),
('传感器重度漂移', '位置传感器信号严重漂移', 'SENSOR_DRIFT', '{"amplitude": 0.1, "driftRate": 0.02, "jointIndex": 3}', 600, 'WARN', ARRAY['传感器', '漂移', '重度'], true, (SELECT id FROM app_user WHERE username = 'admin'), NOW()),
('复合故障-电机+振动', '电机过热同时伴随振动异常', 'OVERHEAT', '{"amplitude": 15.0, "jointIndex": 0, "secondaryFault": "HIGH_VIBRATION", "secondaryAmplitude": 2.0}', 150, 'CRITICAL', ARRAY['复合故障', '电机', '振动'], true, (SELECT id FROM app_user WHERE username = 'admin'), NOW());
