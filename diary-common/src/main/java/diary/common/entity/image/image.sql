CREATE TABLE `images` (
                          `id`             BIGINT          UNSIGNED NOT NULL COMMENT '主键',
                          `user_id`        BIGINT          UNSIGNED NOT NULL COMMENT '用户ID',
                          `file_size`      INT             UNSIGNED    NOT NULL COMMENT '文件大小（字节）',
                          `deleted`        TINYINT(1)      DEFAULT 0 COMMENT '是否删除',
                          `url`            VARCHAR(512)    NOT NULL COMMENT '图片访问URL',
                          `thumbnail_url`  VARCHAR(512)    NULL COMMENT '缩略图URL',
                          `original_name`  VARCHAR(255)    NOT NULL COMMENT '原始文件名',
                          `mime_type`      VARCHAR(100)    NOT NULL COMMENT 'MIME类型，如 image/jpeg',
                          `type`           TINYINT(4)      NOT NULL COMMENT '图片类型  1000 正常图，1100 缩略图，1200 裁剪图，2000 未知类型',
                          `created_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
                          `updated_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
                          PRIMARY KEY (`id`),
                          UNIQUE KEY `idx_images_url` (`url`),
                          INDEX `idx_images_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公共图片表';

