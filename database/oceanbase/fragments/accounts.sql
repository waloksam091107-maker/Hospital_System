-- Account credentials remain in doctors/patients/admins; old accounts default enabled.
CREATE TABLE IF NOT EXISTS account_security (
    account_role VARCHAR(20) NOT NULL,
    account_id INT NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    session_version BIGINT NOT NULL DEFAULT 0,
    must_change_password BOOLEAN NOT NULL DEFAULT FALSE,
    failed_attempts INT NOT NULL DEFAULT 0,
    failure_window_start TIMESTAMP NULL,
    locked_until TIMESTAMP NULL,
    PRIMARY KEY (account_role,account_id)
) DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS login_throttle (
    source_ip VARCHAR(64) NOT NULL,
    attempts INT NOT NULL DEFAULT 0,
    window_start TIMESTAMP NOT NULL,
    PRIMARY KEY (source_ip)
) DEFAULT CHARSET=utf8mb4;
