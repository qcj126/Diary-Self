create table stage_goal(
                           id                      BIGINT UNSIGNED         NOT NULL COMMENT 'primary key',
                           user_id                 BIGINT UNSIGNED         NOT NULL COMMENT 'user id',
                           creator                 VARCHAR(32)             COMMENT 'creator display name',
                           category                VARCHAR(20)             NOT NULL COMMENT 'category',
                           title                   VARCHAR(64)             NOT NULL COMMENT 'goal title',
                           description             VARCHAR(255)            NOT NULL COMMENT 'goal description',
                           is_deleted              TINYINT(1)              NOT NULL DEFAULT 0 COMMENT 'logical delete flag',
                           create_time             DATETIME                NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'create time',
                           update_time             DATETIME                NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'update time',
                           PRIMARY KEY (id),
                           INDEX idx_user_id (user_id),
                           INDEX idx_creator (creator)
) engine=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='stage goal table';

create table sub_goal(
                         id                      BIGINT UNSIGNED         NOT NULL COMMENT 'primary key',
                         stage_goal_id           BIGINT UNSIGNED         NOT NULL COMMENT 'stage goal id',
                         user_id                 BIGINT UNSIGNED         NOT NULL COMMENT 'user id',
                         title                   VARCHAR(100)            NOT NULL COMMENT 'sub goal title',
                         content                 VARCHAR(255)            COMMENT 'detail content',
                         learned_hours           DECIMAL(10,2)           DEFAULT 0.00 COMMENT 'learned hours',
                         estimated_hours         DECIMAL(10,2)           DEFAULT 0.00 COMMENT 'estimated hours',
                         is_deleted              TINYINT(1)              DEFAULT 0 COMMENT 'logical delete flag',
                         create_time             DATETIME                DEFAULT CURRENT_TIMESTAMP COMMENT 'create time',
                         update_time             DATETIME                DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'update time',
                         PRIMARY KEY (id),
                         INDEX idx_stage_goal_id (stage_goal_id),
                         INDEX idx_user_id (user_id)
) engine=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='sub goal table';
