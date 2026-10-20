CREATE TABLE stage_goal
(
    id             BIGINT UNSIGNED NOT NULL COMMENT '主键ID',
    user_id        BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    category       VARCHAR(20)     NOT NULL COMMENT '分类',
    title          VARCHAR(64)     NOT NULL COMMENT '目标标题',
    description    VARCHAR(255)    NOT NULL COMMENT '目标描述',
    status         TINYINT UNSIGNED NOT NULL DEFAULT 0
        COMMENT '状态：0未开始，1进行中，2暂停，3已完成，4已放弃',
    create_time    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    end_time       DATETIME        NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'DDL',
    completed_time DATETIME        NULL COMMENT '完成时间',

    PRIMARY KEY (id),
    KEY idx_stage_goal_user_status_ddl (user_id, status, end_time),

    CONSTRAINT ck_stage_goal_status CHECK (status BETWEEN 0 AND 4)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = '阶段目标表';

CREATE TABLE sub_goal
(
    id              BIGINT UNSIGNED NOT NULL COMMENT '主键ID',
    user_id         BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    stage_id        BIGINT UNSIGNED NOT NULL COMMENT '主目标ID',
    title           VARCHAR(100)    NOT NULL COMMENT '小目标标题',
    content         VARCHAR(255)    NULL COMMENT '详细内容描述',
    status          TINYINT UNSIGNED NOT NULL DEFAULT 0
        COMMENT '状态：0未开始，1进行中，2暂停，3已完成，4已放弃',
    learned_hours   DECIMAL(10, 2)  NULL DEFAULT 0.00 COMMENT '已学时长(小时)',
    estimated_hours DECIMAL(10, 2)  NULL DEFAULT 0.00 COMMENT '预计总用时(小时)',
    create_time     DATETIME        NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    end_time        DATETIME        NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'DDL',
    completed_time  DATETIME        NULL COMMENT '完成时间',

    PRIMARY KEY (id),
    KEY idx_sub_goal_stage_status (stage_id, status),
    KEY idx_sub_goal_user (user_id),

    CONSTRAINT ck_sub_goal_status CHECK (status BETWEEN 0 AND 4),
    CONSTRAINT ck_sub_goal_learned_hours CHECK (learned_hours >= 0),
    CONSTRAINT ck_sub_goal_estimated_hours CHECK (estimated_hours >= 0)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = '小目标表';

CREATE TABLE goal_checkin
(
    id             BIGINT UNSIGNED NOT NULL COMMENT '打卡记录ID，使用雪花算法生成',
    request_id     VARCHAR(64)     NOT NULL COMMENT '幂等请求ID，防止重复打卡',
    user_id        BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    stage_goal_id  BIGINT UNSIGNED NOT NULL COMMENT '阶段目标ID',
    sub_goal_id    BIGINT UNSIGNED NULL COMMENT '子目标ID；为空表示针对阶段目标打卡',
    checkin_type   TINYINT UNSIGNED NOT NULL DEFAULT 1
        COMMENT '打卡类型：1正常打卡，2完成目标，3重新开始，4补打卡',
    spent_hours    DECIMAL(10, 2)  NOT NULL DEFAULT 0.00 COMMENT '本次投入时长，单位：小时',
    content        VARCHAR(1000)   NOT NULL DEFAULT '' COMMENT '本次完成内容或打卡备注',
    evidence_urls  JSON            NULL COMMENT '打卡凭证URL数组',
    checkin_date   DATE            NOT NULL COMMENT '打卡业务日期，支持补打卡',
    source         TINYINT UNSIGNED NOT NULL DEFAULT 1
        COMMENT '来源：1 Web，2 App，3 系统任务，4 数据导入',
    is_deleted     TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常，1删除',
    create_time    DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    update_time    DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',

    PRIMARY KEY (id),
    UNIQUE KEY uk_goal_checkin_user_request (user_id, request_id),
    KEY idx_goal_checkin_stage_time (stage_goal_id, create_time),
    KEY idx_goal_checkin_sub_time (sub_goal_id, create_time),
    KEY idx_goal_checkin_user_date (user_id, checkin_date),
    KEY idx_goal_checkin_user_stage_date (user_id, stage_goal_id, checkin_date),

    CONSTRAINT ck_goal_checkin_spent_hours CHECK (spent_hours >= 0),
    CONSTRAINT ck_goal_checkin_type CHECK (checkin_type BETWEEN 1 AND 4),
    CONSTRAINT ck_goal_checkin_source CHECK (source BETWEEN 1 AND 4),
    CONSTRAINT ck_goal_checkin_deleted CHECK (is_deleted IN (0, 1))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = '目标打卡流水表';
