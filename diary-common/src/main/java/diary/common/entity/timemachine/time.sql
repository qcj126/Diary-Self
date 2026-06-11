create table time_category(
    `id`             BIGINT          UNSIGNED NOT NULL COMMENT '主键',
    `user_id`        BIGINT          UNSIGNED NOT NULL COMMENT '用户ID',
    `category_name`  VARCHAR(255)    NOT NULL COMMENT '分类名称',
    `category_num`   INT             UNSIGNED    NOT NULL COMMENT '分类编号 1100, 1200, 1300...',
    `deleted`        TINYINT(1)      DEFAULT 0 COMMENT '是否删除  0：在用 1：删除 默认：0',
    `category_sort`           INT             UNSIGNED    NOT NULL default 0 COMMENT '分类排序',
    `created_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
    `updated_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    primary key (`id`)
) engine=innodb default charset=utf8mb4 comment='时光机分类表';

create table time_card(
    `id`             BIGINT          UNSIGNED NOT NULL COMMENT '主键',
    `user_id`        BIGINT          UNSIGNED NOT NULL COMMENT '用户ID',
    `image_id`       BIGINT          UNSIGNED NOT NULL COMMENT '图片ID',
    `category_id`    BIGINT          UNSIGNED NOT NULL COMMENT '分类ID',
    `deleted`        TINYINT(1)      DEFAULT 0 COMMENT '是否删除  0：在用 1：删除 默认：0',
    `card_sort`           INT             UNSIGNED    NOT NULL default 0 COMMENT '分类排序',
    `card_title`     VARCHAR(50)    NOT NULL COMMENT '卡片标题',
    `card_content`   varchar(300)    NOT NULL COMMENT '卡片内容',
    `record_time`    DATETIME        NOT NULL COMMENT '记录此事的时间',
    `created_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
    `updated_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    primary key (`id`)
) engine=innodb default charset=utf8mb4 comment='时间机分类卡片表';