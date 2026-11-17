-- auto-generated definition
create table image
(
    id            bigint unsigned                      not null comment '主键'
        primary key,
    user_id       bigint unsigned                      not null comment '用户ID',
    file_size     bigint unsigned                      not null comment '文件大小（字节）',
    original_name varchar(255)                         not null comment '原始文件名',
    mime_type     varchar(100)                         not null comment 'MIME类型，如 image/jpeg',
    type          int                                  not null comment '图片类型 ',
    deleted       tinyint(1) default 0                 not null comment '是否删除：0-否 1-是',
    status        int                                  not null comment '状态：1200 上传中 1100 失败 1000 成功',
    object_key    varchar(255)                         not null comment '对象存储的key',
    create_time   datetime   default CURRENT_TIMESTAMP not null comment '上传时间',
    update_time   datetime   default CURRENT_TIMESTAMP not null comment '更新时间',
    constraint idx_images_object_key
        unique (object_key)
)
    comment '公共图片表' charset = utf8mb4;

create index idx_images_user_id
    on image (user_id);
alter table image add sha256 CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NULL comment '文件SHA-256(hex)';
alter table image add width INT UNSIGNED NULL comment '图片宽度';
alter table image add height INT UNSIGNED NULL comment '图片高度';
CREATE UNIQUE INDEX uk_image_user_type_sha256 ON image(user_id, type, sha256);


CREATE TABLE `thumbnail` (
                             `id`          BIGINT UNSIGNED NOT NULL ,
                             `image_id`    BIGINT UNSIGNED NOT NULL COMMENT '关联业务图片ID',
                             `user_id`     BIGINT UNSIGNED NOT NULL COMMENT '所属用户',
                             `file_size`   BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '文件大小(字节)',
                             `mime_type`   VARCHAR(32)     NOT NULL DEFAULT '' COMMENT 'MIME类型',
                             `width`       INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '宽度(px)',
                             `height`      INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '高度(px)',
                             `object_key`  VARCHAR(255)    NOT NULL COMMENT '对象存储key',
                             `status`      TINYINT         NOT NULL DEFAULT 0 COMMENT '状态: 0-待处理 1-正常 2-失败',
                             `deleted`     TINYINT         NOT NULL DEFAULT 0 COMMENT '软删除: 0-否 1-是',
                             `create_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                             `update_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                             PRIMARY KEY (`id`),
                             UNIQUE KEY `uk_image_id`   (`image_id`),
                             UNIQUE KEY `uk_object_key` (`object_key`),
                             KEY `idx_user_id`          (`user_id`),
                             KEY `idx_status`           (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='缩略图表';

CREATE TABLE `image_upload_session` (
                                        `upload_id`     VARCHAR(64)     NOT NULL COMMENT '上传会话ID(主键)',
                                        `image_id`      BIGINT UNSIGNED     NOT NULL COMMENT '业务图片ID',
                                        `user_id`       BIGINT UNSIGNED NOT NULL COMMENT '所属用户',
                                        `oss_upload_id` VARCHAR(256)    NOT NULL COMMENT 'OSS分片上传的UploadId',
                                        `object_key`    VARCHAR(255)    NOT NULL DEFAULT '' COMMENT '对象存储key',
                                        `sha256`        CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT '' COMMENT '整文件SHA256(hex)',
                                        `file_size`     BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '文件总大小(字节)',
                                        `chunk_size`    INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '分片大小(字节)',
                                        `total_chunks`  INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '分片总数',
                                        `status`        TINYINT         NOT NULL DEFAULT 0 COMMENT '状态: 0-初始化 1-上传中 2-合并中 3-完成 4-已中止 5-过期',
                                        `version`       INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
                                        `expires_at`    DATETIME        NOT NULL COMMENT '会话过期时间',
                                        `create_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                        `update_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                        PRIMARY KEY (`upload_id`),
                                        UNIQUE KEY `uk_image_id`  (`image_id`),
                                        KEY `idx_user_id`         (`user_id`),
                                        KEY `idx_status`          (`status`),
                                        KEY `idx_expires_at`      (`expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='图片分片上传会话表';

CREATE TABLE `image_upload_chunk` (
                                      `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
                                      `upload_id`    VARCHAR(64)     NOT NULL COMMENT '上传会话ID',
                                      `chunk_index`  INT UNSIGNED    NOT NULL COMMENT '分片序号(从0开始)',
                                      `part_number`  INT UNSIGNED    NOT NULL COMMENT 'OSS分片编号(从1开始)',
                                      `chunk_sha256` CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT '' COMMENT '分片SHA256(hex)',
                                      `etag`         VARCHAR(128)    NOT NULL DEFAULT '' COMMENT 'OSS分片ETag',
                                      `chunk_size`   INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '分片大小(字节)',
                                      `create_time`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                      `update_time`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                      PRIMARY KEY (`id`),
                                      UNIQUE KEY `uk_upload_chunk_index` (`upload_id`, `chunk_index`),
                                      UNIQUE KEY `uk_upload_part_number` (`upload_id`, `part_number`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='图片分片上传分片表';


create table icon
(
    id          bigint auto_increment comment '图标ID'
        primary key,
    icon_name   varchar(64)                        not null comment '图标名称',
    icon_type   tinyint  default 1                 null comment '图标类型：1-PNG，2-SVG，3-字体图标',
    icon_path   varchar(255)                       not null comment '图标文件存储路径',
    icon_size   int                                not null comment '文件大小（字节）',
    icon_pixel  int                                not null comment '图标像素大小',
    user_id     bigint                             not null comment '用户ID',
    create_time datetime default CURRENT_TIMESTAMP null comment '创建时间',
    update_time datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间'
)
    comment '图标库主表' charset = utf8mb4;

create index idx_user_id
    on icon (user_id);
