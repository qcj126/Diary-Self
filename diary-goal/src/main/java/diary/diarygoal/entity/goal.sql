-- auto-generated definition
create table stage_goal
(
    id             bigint unsigned                            not null comment '主键ID'
        primary key,
    user_id        bigint unsigned                            not null comment '用户ID',
    category       varchar(20)                                not null comment '分类',
    title          varchar(64)                                not null comment '目标标题',
    description    varchar(255)                               not null comment '目标描述',
    status         tinyint unsigned default '0'               not null comment '状态：0未开始，1进行中，2暂停，3已完成，4已放弃',
    create_time    datetime         default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time    datetime         default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    end_time       datetime         default CURRENT_TIMESTAMP null comment 'DDL',
    completed_time datetime                                   null comment '完成时间',
    constraint ck_stage_goal_status
        check (`status` between 0 and 4)
)
    comment '阶段目标表' charset = utf8mb4;

create index idx_stage_goal_user_status_ddl
    on stage_goal (user_id, status, end_time);



-- auto-generated definition
create table sub_goal
(
    id              bigint unsigned                            not null comment '主键ID'
        primary key,
    user_id         bigint unsigned                            not null comment '用户ID',
    stage_id        bigint unsigned                            not null comment '主目标ID',
    title           varchar(100)                               not null comment '小目标标题',
    content         varchar(255)                               null comment '详细内容描述',
    status          tinyint unsigned default '0'               not null comment '状态：0未开始，1进行中，2暂停，3已完成，4已放弃',
    learned_hours   decimal(10, 2)   default 0.00              null comment '已学时长(小时)',
    estimated_hours decimal(10, 2)   default 0.00              null comment '预计总用时(小时)',
    create_time     datetime         default CURRENT_TIMESTAMP null comment '创建时间',
    update_time     datetime         default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '修改时间',
    end_time        datetime         default CURRENT_TIMESTAMP null comment 'DDL',
    completed_time  datetime                                   null comment '完成时间',
    constraint ck_sub_goal_estimated_hours
        check (`estimated_hours` >= 0),
    constraint ck_sub_goal_learned_hours
        check (`learned_hours` >= 0),
    constraint ck_sub_goal_status
        check (`status` between 0 and 4)
)
    comment '小目标表' charset = utf8mb4;

create index idx_sub_goal_stage_status
    on sub_goal (stage_id, status);

create index idx_sub_goal_user
    on sub_goal (user_id);



-- auto-generated definition
create table goal_checkin
(
    id            bigint unsigned                               not null comment '打卡记录ID，使用雪花算法生成'
        primary key,
    request_id    varchar(64)                                   not null comment '幂等请求ID，防止重复打卡',
    user_id       bigint unsigned                               not null comment '用户ID',
    stage_goal_id bigint unsigned                               not null comment '阶段目标ID',
    sub_goal_id   bigint unsigned                               null comment '子目标ID；为空表示针对阶段目标打卡',
    checkin_type  tinyint unsigned default '1'                  not null comment '打卡类型：1正常打卡，2完成目标，3重新开始，4补打卡',
    spent_hours   decimal(10, 2)   default 0.00                 not null comment '本次投入时长，单位：小时',
    content       varchar(1000)    default ''                   not null comment '本次完成内容或打卡备注',
    evidence_urls json                                          null comment '打卡凭证URL数组',
    checkin_date  date                                          not null comment '打卡业务日期，支持补打卡',
    source        tinyint unsigned default '1'                  not null comment '来源：1 Web，2 App，3 系统任务，4 数据导入',
    is_deleted    tinyint unsigned default '0'                  not null comment '逻辑删除：0正常，1删除',
    create_time   datetime(3)      default CURRENT_TIMESTAMP(3) not null comment '创建时间',
    update_time   datetime(3)      default CURRENT_TIMESTAMP(3) not null on update CURRENT_TIMESTAMP(3) comment '更新时间',
    constraint uk_goal_checkin_user_request
        unique (user_id, request_id),
    constraint ck_goal_checkin_deleted
        check (`is_deleted` in (0, 1)),
    constraint ck_goal_checkin_source
        check (`source` between 1 and 4),
    constraint ck_goal_checkin_spent_hours
        check (`spent_hours` >= 0),
    constraint ck_goal_checkin_type
        check (`checkin_type` between 1 and 4)
)
    comment '目标打卡流水表' charset = utf8mb4;

create index idx_goal_checkin_stage_time
    on goal_checkin (stage_goal_id, create_time);

create index idx_goal_checkin_sub_time
    on goal_checkin (sub_goal_id, create_time);

create index idx_goal_checkin_user_date
    on goal_checkin (user_id, checkin_date);

create index idx_goal_checkin_user_stage_date
    on goal_checkin (user_id, stage_goal_id, checkin_date);


