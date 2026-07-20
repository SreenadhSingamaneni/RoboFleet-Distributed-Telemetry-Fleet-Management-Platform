CREATE TABLE robots (
    id VARCHAR(32) PRIMARY KEY,
    display_name VARCHAR(100) NOT NULL,
    model VARCHAR(80) NOT NULL,
    facility VARCHAR(120) NOT NULL,
    ward VARCHAR(80) NOT NULL,
    floor INTEGER NOT NULL,
    operational_status VARCHAR(32) NOT NULL,
    last_seen_at TIMESTAMPTZ,
    battery_percent DOUBLE PRECISION,
    current_x DOUBLE PRECISION,
    current_y DOUBLE PRECISION,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_robot_battery CHECK (battery_percent IS NULL OR battery_percent BETWEEN 0 AND 100)
);

CREATE INDEX idx_robots_status ON robots (operational_status);
CREATE INDEX idx_robots_last_seen ON robots (last_seen_at);

CREATE TABLE telemetry (
    event_id UUID NOT NULL,
    robot_id VARCHAR(32) NOT NULL REFERENCES robots(id),
    sequence_number BIGINT NOT NULL,
    recorded_at TIMESTAMPTZ NOT NULL,
    received_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    x DOUBLE PRECISION NOT NULL,
    y DOUBLE PRECISION NOT NULL,
    floor INTEGER NOT NULL,
    battery_percent DOUBLE PRECISION NOT NULL,
    speed_mps DOUBLE PRECISION NOT NULL,
    heading_degrees DOUBLE PRECISION NOT NULL,
    temperature_celsius DOUBLE PRECISION NOT NULL,
    mission_state VARCHAR(32) NOT NULL,
    connectivity VARCHAR(32) NOT NULL,
    error_codes JSONB NOT NULL DEFAULT '[]'::jsonb,
    PRIMARY KEY (event_id, recorded_at),
    CONSTRAINT ck_telemetry_battery CHECK (battery_percent BETWEEN 0 AND 100),
    CONSTRAINT ck_telemetry_heading CHECK (heading_degrees >= 0 AND heading_degrees < 360)
) PARTITION BY RANGE (recorded_at);

CREATE TABLE telemetry_default PARTITION OF telemetry DEFAULT;
CREATE INDEX idx_telemetry_robot_time ON telemetry (robot_id, recorded_at DESC);
CREATE INDEX idx_telemetry_recorded_at ON telemetry (recorded_at DESC);

CREATE TABLE alerts (
    id UUID PRIMARY KEY,
    robot_id VARCHAR(32) NOT NULL REFERENCES robots(id),
    type VARCHAR(48) NOT NULL,
    severity VARCHAR(16) NOT NULL,
    status VARCHAR(16) NOT NULL,
    message VARCHAR(500) NOT NULL,
    first_triggered_at TIMESTAMPTZ NOT NULL,
    last_triggered_at TIMESTAMPTZ NOT NULL,
    acknowledged_at TIMESTAMPTZ,
    acknowledged_by VARCHAR(120),
    resolved_at TIMESTAMPTZ,
    occurrences INTEGER NOT NULL DEFAULT 1,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX uq_alert_open_robot_type ON alerts (robot_id, type) WHERE status IN ('OPEN', 'ACKNOWLEDGED');
CREATE INDEX idx_alert_status_severity_time ON alerts (status, severity, last_triggered_at DESC);
CREATE INDEX idx_alert_robot_time ON alerts (robot_id, last_triggered_at DESC);

INSERT INTO robots (id, display_name, model, facility, ward, floor, operational_status)
SELECT
    'RBT-' || lpad(number::text, 4, '0'),
    'Hospital Robot ' || number,
    CASE WHEN number % 3 = 0 THEN 'RX-Courier' WHEN number % 3 = 1 THEN 'RX-Supply' ELSE 'RX-Lab' END,
    'Rovex Medical Center',
    'Ward ' || chr(65 + (number % 8)),
    1 + (number % 5),
    'OFFLINE'
FROM generate_series(1, 1000) AS number;
