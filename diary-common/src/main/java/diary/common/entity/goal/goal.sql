create table stage_goal(
                           id                      BIGINT UNSIGNED         NOT NULL COMMENT '主键',
                           user_id                 BIGINT UNSIGNED         NOT NULL COMMENT '用户ID',
                           category                VARCHAR(20)             NOT NULL COMMENT '分类',
                           title                   VARCHAR(64)             NOT NULL COMMENT '目标标题',
                           description             VARCHAR(255)            NOT NULL COMMENT '目标描述',
                           create_time             DATETIME                NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                           update_time             DATETIME                NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间'
) engine=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='阶段目标表';

create table sub_goal(
                         `id` bigint PRIMARY KEY COMMENT '主键ID',
                         user_id                 BIGINT UNSIGNED         NOT NULL COMMENT '用户ID',
                         `title` VARCHAR(100) NOT NULL COMMENT '小目标标题',
                         `content` VARCHAR(255) COMMENT '详细内容描述',
                         `learned_hours` DECIMAL(10,2) DEFAULT 0.00 COMMENT '已学时长(小时)',
                         `estimated_hours` DECIMAL(10,2) DEFAULT 0.00 COMMENT '预计总用时(小时)',
                         `is_deleted` TINYINT(1) DEFAULT 0 COMMENT '软删除标记',
                         `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                         `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间'
) engine=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='小目标表';