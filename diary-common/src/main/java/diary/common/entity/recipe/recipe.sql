CREATE TABLE recipe(
    id                      BIGINT UNSIGNED         NOT NULL COMMENT '主键',
    user_id                 BIGINT UNSIGNED         NOT NULL COMMENT '创建者用户ID',
    title                   VARCHAR(90)             NOT NULL COMMENT '标题',
    cover_img               VARCHAR(255)            NOT NULL COMMENT '封面图URL',
    description             VARCHAR(300)            NOT NULL COMMENT '简介',
    category                TINYINT                 NOT NULL COMMENT '分类：0-家常 1-西餐 2-甜点 3-汤粥 4-其他',
    meal_type               TINYINT                 NOT NULL COMMENT '餐别：1-早餐 2-午餐 3-晚餐 4-夜宵',
    difficulty              TINYINT                 NOT NULL COMMENT '难度：1-5',
    cooking_time            TINYINT                 NOT NULL COMMENT '烹饪时长（分钟）',
    story                   VARCHAR(300)            NOT NULL COMMENT '情感故事/备注',
    sort                    INT UNSIGNED            NOT NULL DEFAULT 0 COMMENT '排序',
    deleted                 TINYINT(1)              NOT NULL DEFAULT 0 COMMENT '是否删除：0-否 1-是',
    create_time             DATETIME                NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time             DATETIME                NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY idx_recipe_title (title) COMMENT '标题唯一索引'
) ENGINE=INNODB DEFAULT CHARSET=utf8mb4 COMMENT='食谱主表';

CREATE TABLE recipe_ingredient(
    id                      BIGINT UNSIGNED         NOT NULL COMMENT '主键',
    recipe_id               BIGINT UNSIGNED         NOT NULL COMMENT '食谱ID',
    user_id                 BIGINT UNSIGNED         NOT NULL COMMENT '创建者用户ID',
    name                    VARCHAR(255)            NOT NULL COMMENT '食材名称',
    quantity                VARCHAR(255)            NOT NULL COMMENT '食材数量',
    is_main                 TINYINT                 NOT NULL COMMENT '是否主料：0-否 1-是',
    sort                    INT UNSIGNED            NOT NULL DEFAULT 0 COMMENT '排序',
    deleted                 TINYINT(1)              NOT NULL DEFAULT 0 COMMENT '是否删除：0-否 1-是',
    create_time             DATETIME                NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time             DATETIME                NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    INDEX idx_recipe_ingredient_recipe_id (recipe_id) COMMENT '食谱ID索引'
) ENGINE=INNODB DEFAULT CHARSET=utf8mb4 COMMENT='食谱食材表';

CREATE TABLE recipe_step(
    id                      BIGINT UNSIGNED         NOT NULL COMMENT '主键',
    recipe_id               BIGINT UNSIGNED         NOT NULL COMMENT '食谱ID',
    user_id                 BIGINT UNSIGNED         NOT NULL COMMENT '创建者用户ID',
    step_number             TINYINT                 NOT NULL COMMENT '步骤编号',
    description             VARCHAR(300)            NOT NULL COMMENT '步骤描述',
    image_url               VARCHAR(255)            NOT NULL COMMENT '步骤图片URL',
    timer_minute            TINYINT                 NOT NULL COMMENT '步骤计时（分钟）',
    sort                    INT UNSIGNED            NOT NULL DEFAULT 0 COMMENT '排序',
    deleted                 TINYINT(1)              NOT NULL DEFAULT 0 COMMENT '是否删除：0-否 1-是',
    create_time             DATETIME                NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time             DATETIME                NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    INDEX idx_recipe_step_recipe_id (recipe_id) COMMENT '食谱ID索引'
) ENGINE=INNODB DEFAULT CHARSET=utf8mb4 COMMENT='食谱步骤表';