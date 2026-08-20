-- Integration Event Log Table
-- Tracks all integration events between Fineract and Core Banking System

CREATE TABLE IF NOT EXISTS `integration_event_log` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `event_id` VARCHAR(50) NOT NULL COMMENT 'Unique event identifier',
    `event_type` VARCHAR(50) NOT NULL COMMENT 'Event type: LOAN_CREATED, LOAN_DISBURSED, REPAYMENT_RECEIVED, etc.',
    `source_system` VARCHAR(30) NOT NULL COMMENT 'Source: FINERACT or CORE_BANKING',
    `target_system` VARCHAR(30) NOT NULL COMMENT 'Target system',
    `correlation_id` VARCHAR(50) NOT NULL COMMENT 'Correlation ID for tracking across systems',
    `payload` TEXT COMMENT 'Event payload JSON',
    `status` VARCHAR(20) NOT NULL COMMENT 'Status: NEW, PUBLISHED, PROCESSED, FAILED',
    `retry_count` INT DEFAULT 0 COMMENT 'Number of retry attempts',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `published_at` DATETIME NULL COMMENT 'When event was successfully published',
    `error_message` TEXT COMMENT 'Error message if failed',
    INDEX `idx_event_type` (`event_type`),
    INDEX `idx_correlation_id` (`correlation_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Integration event log between Fineract and Core Banking';

-- Integration Audit Trail
CREATE TABLE IF NOT EXISTS `integration_audit_trail` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `correlation_id` VARCHAR(50) NOT NULL COMMENT 'Correlation ID',
    `integration_type` VARCHAR(30) NOT NULL COMMENT 'Type: LOAN_APPLICATION, DISBURSEMENT, REPAYMENT, etc.',
    `direction` VARCHAR(10) NOT NULL COMMENT 'Direction: INBOUND or OUTBOUND',
    `request_payload` TEXT COMMENT 'Request payload',
    `response_payload` TEXT COMMENT 'Response payload',
    `status` VARCHAR(20) NOT NULL COMMENT 'Status: SUCCESS, FAILED, TIMEOUT',
    `duration_ms` INT COMMENT 'Processing duration in milliseconds',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `error_message` TEXT COMMENT 'Error message if failed',
    `error_code` VARCHAR(50) COMMENT 'Error code if failed',
    INDEX `idx_correlation_id` (`correlation_id`),
    INDEX `idx_type_status` (`integration_type`, `status`),
    INDEX `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Integration audit trail';

-- External System Mapping Table
CREATE TABLE IF NOT EXISTS `external_system_mapping` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `mapping_type` VARCHAR(30) NOT NULL COMMENT 'Type: CLIENT_ID, ACCOUNT_NO, PRODUCT_CODE, etc.',
    `fineract_id` VARCHAR(100) NOT NULL COMMENT 'Fineract identifier',
    `core_system_id` VARCHAR(100) NOT NULL COMMENT 'Core banking identifier',
    `mapping_status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'Status: ACTIVE, INACTIVE',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_mapping` (`mapping_type`, `fineract_id`, `core_system_id`),
    INDEX `idx_core_id` (`mapping_type`, `core_system_id`),
    INDEX `idx_fineract_id` (`mapping_type`, `fineract_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='External system ID mapping';

-- Insert default payment types for core banking integration
INSERT INTO `payment_type` (`name`, `description`, `is_system_defined`) VALUES
('CORE_BANKING_TRANSFER', 'Core Banking System Transfer', 1),
('CORE_BANKING_DIRECT_DEBIT', 'Core Banking Direct Debit', 1),
('CORE_BANKING_DIRECT_CREDIT', 'Core Banking Direct Credit', 1);
