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

-- Insert minimal seed data for basic functionality
INSERT INTO app_role (name, description) VALUES
('ADMIN', 'System administrator with full access'),
('OPERATOR', 'Operations user with monitoring and management access'),
('VIEWER', 'Read-only user with view access');

INSERT INTO app_user (username, password_hash, email, full_name) VALUES
('admin', '{noop}admin123', 'admin@iropsim.com', 'System Administrator');

INSERT INTO user_role (user_id, role_id) VALUES
((SELECT id FROM app_user WHERE username = 'admin'), (SELECT id FROM app_role WHERE name = 'ADMIN'));

INSERT INTO robot (name, model, joint_count) VALUES
('Robot-001', 'IRB-6700', 6);

INSERT INTO scenario (name, description, base_params) VALUES
('Normal Operation', 'Normal robot operation scenario', '{"current_nominal": 2.5, "temp_nominal": 40.0, "vibration_nominal": 0.1}');

INSERT INTO fault_template (name, description, fault_type, params, duration_seconds, severity, tags, enabled, created_by) VALUES
('电机过热', '电机温度异常升高', 'OVERHEAT', '{"amplitude": 10.0, "jointIndex": 0}', 60, 'WARN', ARRAY['电机', '温度'], true, (SELECT id FROM app_user WHERE username = 'admin')),
('振动异常', '关节振动异常', 'HIGH_VIBRATION', '{"amplitude": 2.5, "jointIndex": 2}', 90, 'WARN', ARRAY['振动', '关节'], true, (SELECT id FROM app_user WHERE username = 'admin')),
('电流尖峰', '电流突然上升', 'CURRENT_SPIKE', '{"amplitude": 3.0, "jointIndex": 1}', 30, 'CRITICAL', ARRAY['电流', '驱动器'], true, (SELECT id FROM app_user WHERE username = 'admin')),
('传感器漂移', '位置传感器信号漂移', 'SENSOR_DRIFT', '{"amplitude": 0.02, "driftRate": 0.005, "jointIndex": 3}', 300, 'INFO', ARRAY['传感器', '位置'], true, (SELECT id FROM app_user WHERE username = 'admin'));
