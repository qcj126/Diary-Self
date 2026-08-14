create table ai_info(
                        id             bigint                             not null comment '主键'
                            primary key,
                        user_id        bigint unsigned                    not null comment '用户ID',
                        model          varchar(20)                        not null comment 'AI模型',
                        temperature    varchar(5)                         not null comment 'AI结果创意度',
                        ai_type        tinyint(1)                         not null comment 'AI类别，对应着AI模型',
                        ai_application tinyint                            not null comment 'AI用途：如鉴别营养成分，每日推荐菜品等',
                        create_time    datetime default CURRENT_TIMESTAMP not null comment '创建时间',
                        update_time    datetime default CURRENT_TIMESTAMP not null comment '更新时间'
) engine=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI信息表';

create table ai_nutrient(
                            id           bigint                                not null comment '主键'
                                primary key,
                            user_id      bigint unsigned                       not null comment '用户ID',
                            universal_id bigint unsigned                       not null comment '图片ID',
                            ai_info_id   bigint unsigned                       not null comment 'ai结果表主键',
                            calory       varchar(10) default '0'               null comment '热量 卡路里',
                            protein      varchar(8)  default '0'               null comment '蛋白质',
                            fat          varchar(8)  default '0'               null comment '脂肪',
                            carbohydrate varchar(8)  default '0'               null comment '碳水化合物',
                            sugar        varchar(8)  default '0'               null comment '糖分',
                            sodium       varchar(8)  default '0'               null comment '钠含量',
                            create_time  datetime    default CURRENT_TIMESTAMP null comment '创建时间',
                            update_time  datetime    default CURRENT_TIMESTAMP null comment '更新时间',
                            ai_task_id   bigint                                not null comment 'ai任务表主键',
                            flag         varchar(16)                           not null comment 'ai通用id类型标志'
) engine=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI饮食营养表';
create index idx_ai_nutrient_info_id on ai_nutrient (ai_info_id);
create index idx_ai_nutrient_task_id on ai_nutrient (ai_task_id);
create index idx_ai_nutrient_user_time on ai_nutrient (user_id, create_time);

create table ai_task(
    id                bigint unsigned                    not null comment 'primary key'
        primary key,
    user_id           bigint unsigned                    not null comment 'user id',
    client_request_id varchar(128)                       not null comment '幂等id',
    task_type         varchar(64)                        not null comment '任务类型',
    status            varchar(64)                        not null comment '任务状态',
    input_snapshot    longtext                           not null comment '实体类的json数据',
    attempt_count     int                                not null comment '消息尝试消费次数',
    max_attempts      int                                not null comment '消息最大消费次数',
    worker_id         varchar(128)                       null comment '实例工作id',
    lease_until       datetime                           null comment '占有此消息的持续时间',
    ai_info_id        bigint unsigned                    null comment '与aiInFo主键关联',
    error_code        varchar(64)                        null comment 'error code',
    error_message     longtext                           null comment 'error message',
    create_time       datetime default CURRENT_TIMESTAMP not null comment '创建任务的时间',
    queue_time        datetime                           null comment '进入队列的时间',
    start_time        datetime                           null comment 'start time',
    finish_time       datetime                           null comment 'finish time',
    version_id        int unsigned                       not null comment 'version id',
    constraint uk_ai_task_user_client_request unique (user_id, client_request_id)
) engine=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI任务表';