CREATE TABLE image(
    id                      BIGINT UNSIGNED         NOT NULL COMMENT '主键',
    user_id                 BIGINT UNSIGNED         NOT NULL COMMENT '用户ID',
    file_size               BIGINT UNSIGNED         NOT NULL COMMENT '文件大小（字节）',
    url                     VARCHAR(512)            NOT NULL COMMENT '图片访问URL（签名URL，前端展示用）',
    object_key              VARCHAR(512)            NOT NULL COMMENT 'OSS对象键（文件路径，用于生成签名URL）',
    original_name           VARCHAR(255)            NOT NULL COMMENT '原始文件名',
    mime_type               VARCHAR(100)            NOT NULL COMMENT 'MIME类型，如 image/jpeg',
    type                    TINYINT                 NOT NULL COMMENT '图片类型 ',
    deleted                 TINYINT(1)              NOT NULL DEFAULT 0 COMMENT '是否删除：0-否 1-是',
    status                  TINYINT                 NOT NULL COMMENT '状态：1200 上传中 1100 失败 1000 成功',
    create_time             DATETIME                NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
    update_time             DATETIME                NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY idx_images_object_key (object_key),
    INDEX idx_images_user_id (user_id)
) ENGINE=INNODB DEFAULT CHARSET=utf8mb4 COMMENT='公共图片表';