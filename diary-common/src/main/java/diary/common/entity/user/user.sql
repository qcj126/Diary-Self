CREATE TABLE IF NOT EXISTS `user` (
  `user_id` BIGINT NOT NULL COMMENT 'user id',
  `username` VARCHAR(64) NOT NULL COMMENT 'username',
  `email` VARCHAR(128) DEFAULT NULL COMMENT 'email',
  `phone` VARCHAR(32) DEFAULT NULL COMMENT 'phone',
  `password` VARCHAR(128) NOT NULL COMMENT 'BCrypt password',
  `status` INT NOT NULL DEFAULT 1000 COMMENT '1000 enabled, 0 disabled',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'created time',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'updated time',
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `uk_user_username` (`username`),
  UNIQUE KEY `uk_user_email` (`email`),
  UNIQUE KEY `uk_user_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='user table';

CREATE TABLE IF NOT EXISTS `role` (
  `role_id` BIGINT NOT NULL COMMENT 'role id',
  `role_code` VARCHAR(32) NOT NULL COMMENT 'role code: admin/user',
  `role_name` VARCHAR(64) NOT NULL COMMENT 'role name',
  `description` VARCHAR(255) DEFAULT NULL COMMENT 'description',
  `status` INT NOT NULL DEFAULT 1000 COMMENT '1000 enabled, 0 disabled',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'created time',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'updated time',
  PRIMARY KEY (`role_id`),
  UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='role table';

CREATE TABLE IF NOT EXISTS `user_role` (
  `id` BIGINT NOT NULL COMMENT 'id',
  `user_id` BIGINT NOT NULL COMMENT 'user id',
  `role_id` BIGINT NOT NULL COMMENT 'role id',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'created time',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_role` (`user_id`, `role_id`),
  KEY `idx_user_role_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='user role relation table';

INSERT INTO `role` (`role_id`, `role_code`, `role_name`, `description`)
VALUES
  (1, 'admin', 'admin', 'can add, delete and query'),
  (2, 'user', 'user', 'can query')
ON DUPLICATE KEY UPDATE
  `role_name` = VALUES(`role_name`),
  `description` = VALUES(`description`);
