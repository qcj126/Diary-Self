CREATE TABLE time_category(
    id                      BIGINT UNSIGNED         NOT NULL COMMENT '主键',
    user_id                 BIGINT UNSIGNED         NOT NULL COMMENT '用户ID',
    category_name           VARCHAR(20)             NOT NULL COMMENT '分类名称',
    category_num            INT UNSIGNED            NOT NULL COMMENT '分类编号 1100, 1200, 1300...',
    sort                    INT UNSIGNED            NOT NULL DEFAULT 0 COMMENT '排序',
    deleted                 TINYINT(1)              NOT NULL DEFAULT 0 COMMENT '是否删除：0-否 1-是',
    create_time             DATETIME                NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time             DATETIME                NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id)
) ENGINE=INNODB DEFAULT CHARSET=utf8mb4 COMMENT='时光机分类表';

CREATE TABLE time_card(
    id                      BIGINT UNSIGNED         NOT NULL COMMENT '主键',
    user_id                 BIGINT UNSIGNED         NOT NULL COMMENT '用户ID',
    image_id                BIGINT UNSIGNED         NOT NULL COMMENT '图片ID',
    category_id             BIGINT UNSIGNED         NOT NULL COMMENT '分类ID',
    card_title              VARCHAR(50)             NOT NULL COMMENT '卡片标题',
    card_content            VARCHAR(300)            NOT NULL COMMENT '卡片内容',
    record_time             DATETIME                NOT NULL COMMENT '记录此事的时间',
    sort                    INT UNSIGNED            NOT NULL DEFAULT 0 COMMENT '排序',
    deleted                 TINYINT(1)              NOT NULL DEFAULT 0 COMMENT '是否删除：0-否 1-是',
    create_time             DATETIME                NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time             DATETIME                NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id)
) ENGINE=INNODB DEFAULT CHARSET=utf8mb4 COMMENT='时间机分类卡片表';