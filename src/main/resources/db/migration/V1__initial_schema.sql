CREATE TABLE audience (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(32) NOT NULL,
    refresh_frequency VARCHAR(32) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL
);

CREATE TABLE audience_version (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    audience_id VARCHAR(64) NOT NULL,
    version_number INT NOT NULL,
    match_type VARCHAR(32) NOT NULL,
    published BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT uk_audience_rule_version UNIQUE (audience_id, version_number)
);

CREATE TABLE audience_condition_entity (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    version_id BIGINT NOT NULL,
    field_name VARCHAR(64) NOT NULL,
    operator VARCHAR(32) NOT NULL,
    condition_value VARCHAR(255) NOT NULL,
    INDEX idx_audience_condition_version (version_id)
);

CREATE TABLE audience_calculation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    audience_id VARCHAR(64) NOT NULL,
    rule_version INT NOT NULL,
    batch_number INT NOT NULL,
    member_count INT NOT NULL,
    calculated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT uk_audience_calculation UNIQUE (audience_id, rule_version, batch_number)
);

CREATE TABLE audience_member (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    calculation_id BIGINT NOT NULL,
    user_id VARCHAR(64) NOT NULL,
    CONSTRAINT uk_audience_calculation_member UNIQUE (calculation_id, user_id)
);

CREATE TABLE doris_user_profile (
    user_id VARCHAR(64) PRIMARY KEY,
    city VARCHAR(128),
    member_level VARCHAR(64),
    new_user BOOLEAN NOT NULL,
    risk_level VARCHAR(64),
    marketing_allowed BOOLEAN NOT NULL
);

CREATE TABLE campaign (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    starts_at TIMESTAMP(6),
    ends_at TIMESTAMP(6),
    inventory INT NOT NULL,
    status VARCHAR(32) NOT NULL,
    version BIGINT NOT NULL
);

CREATE TABLE claim (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    campaign_id VARCHAR(64) NOT NULL,
    user_id VARCHAR(64) NOT NULL,
    claimed_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT uk_campaign_claim_user UNIQUE (campaign_id, user_id)
);
