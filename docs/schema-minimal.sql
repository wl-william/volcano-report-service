-- Minimal database schema for testing Volcano Report Service
-- This creates the minimum tables needed for the application to start

-- Task Progress table (required for checkpoint tracking)
CREATE TABLE IF NOT EXISTS `task_progress` (
  `task_id` varchar(100) NOT NULL,
  `table_name` varchar(50) NOT NULL,
  `task_type` varchar(20) NOT NULL,
  `status` int NOT NULL DEFAULT 0 COMMENT '0:pending, 1:running, 2:completed, 3:failed',
  `last_processed_id` bigint NOT NULL DEFAULT 0,
  `processed_count` bigint NOT NULL DEFAULT 0,
  `success_count` bigint NOT NULL DEFAULT 0,
  `fail_count` bigint NOT NULL DEFAULT 0,
  `start_time` datetime DEFAULT NULL,
  `end_time` datetime DEFAULT NULL,
  `error_msg` text,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`task_id`),
  KEY `idx_table_type_status` (`table_name`, `task_type`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Task progress tracking';

-- Example event table: page_vidw
CREATE TABLE IF NOT EXISTS `page_vidw` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_unique_id` varchar(100) NOT NULL COMMENT 'User unique ID',
  `event_time` bigint NOT NULL COMMENT 'Event timestamp (milliseconds)',
  `report_status` int NOT NULL DEFAULT 0 COMMENT '0:pending, 1:processing, 2:success, 3:failed',
  `retry_count` int NOT NULL DEFAULT 0 COMMENT 'Retry count',
  `error_msg` varchar(500) DEFAULT NULL COMMENT 'Error message if failed',
  `refer_page_id` varchar(100) DEFAULT NULL COMMENT 'Referrer page ID',
  `page_id` varchar(100) DEFAULT NULL COMMENT 'Current page ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_report_status` (`report_status`, `id`),
  KEY `idx_user_time` (`user_unique_id`, `event_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Page view events';

-- Insert test data
INSERT INTO `page_vidw` (`user_unique_id`, `event_time`, `report_status`, `refer_page_id`, `page_id`) VALUES
('user_001', UNIX_TIMESTAMP(NOW()) * 1000, 0, 'home', 'product_list'),
('user_002', UNIX_TIMESTAMP(NOW()) * 1000, 0, 'search', 'product_detail'),
('user_003', UNIX_TIMESTAMP(NOW()) * 1000, 0, 'product_list', 'checkout');

-- Other event tables (optional, can be created as needed)

-- Element click events
CREATE TABLE IF NOT EXISTS `element_click` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_unique_id` varchar(100) NOT NULL,
  `event_time` bigint NOT NULL,
  `report_status` int NOT NULL DEFAULT 0,
  `retry_count` int NOT NULL DEFAULT 0,
  `error_msg` varchar(500) DEFAULT NULL,
  `click_type` varchar(50) DEFAULT NULL,
  `click_position` varchar(100) DEFAULT NULL,
  `click_name` varchar(100) DEFAULT NULL,
  `click_area` varchar(100) DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_report_status` (`report_status`, `id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Payment events
CREATE TABLE IF NOT EXISTS `pay` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_unique_id` varchar(100) NOT NULL,
  `event_time` bigint NOT NULL,
  `report_status` int NOT NULL DEFAULT 0,
  `retry_count` int NOT NULL DEFAULT 0,
  `error_msg` varchar(500) DEFAULT NULL,
  `pay_type` varchar(50) DEFAULT NULL,
  `pay_amount` decimal(10,2) DEFAULT NULL,
  `package_type` varchar(50) DEFAULT NULL,
  `package_id` varchar(100) DEFAULT NULL,
  `package_name` varchar(200) DEFAULT NULL,
  `is_ai` tinyint DEFAULT 0,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_report_status` (`report_status`, `id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Payment result events
CREATE TABLE IF NOT EXISTS `pay_result` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_unique_id` varchar(100) NOT NULL,
  `event_time` bigint NOT NULL,
  `report_status` int NOT NULL DEFAULT 0,
  `retry_count` int NOT NULL DEFAULT 0,
  `error_msg` varchar(500) DEFAULT NULL,
  `pay_result` varchar(50) DEFAULT NULL,
  `pay_type` varchar(50) DEFAULT NULL,
  `pay_amount` decimal(10,2) DEFAULT NULL,
  `package_type` varchar(50) DEFAULT NULL,
  `package_id` varchar(100) DEFAULT NULL,
  `package_name` varchar(200) DEFAULT NULL,
  `is_ai` tinyint DEFAULT 0,
  `source` varchar(100) DEFAULT NULL,
  `device` varchar(100) DEFAULT NULL,
  `device_type` varchar(50) DEFAULT NULL,
  `sale_channel` varchar(100) DEFAULT NULL,
  `sd_card` varchar(50) DEFAULT NULL,
  `device_first_time` bigint DEFAULT NULL,
  `cloud_expire_time` bigint DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_report_status` (`report_status`, `id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- User info events
CREATE TABLE IF NOT EXISTS `user_info` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_unique_id` varchar(100) NOT NULL,
  `event_time` bigint NOT NULL,
  `report_status` int NOT NULL DEFAULT 0,
  `retry_count` int NOT NULL DEFAULT 0,
  `error_msg` varchar(500) DEFAULT NULL,
  `reg_time` bigint DEFAULT NULL,
  `ys_dev_cnt` int DEFAULT NULL,
  `user_add_day` int DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_report_status` (`report_status`, `id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
