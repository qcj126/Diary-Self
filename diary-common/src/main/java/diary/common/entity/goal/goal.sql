-- auto-generated definition
create table stage_goal
(
    id          bigint unsigned                    not null comment '主键',
    user_id     bigint unsigned                    not null comment '用户ID',
    category    varchar(20)                        not null comment '分类',
    title       varchar(64)                        not null comment '目标标题',
    description varchar(255)                       not null comment '目标描述',
    create_time datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time datetime default CURRENT_TIMESTAMP not null comment '更新时间'
)
    comment '阶段目标表' charset = utf8mb4;


-- auto-generated definition
create table sub_goal
(
    id              bigint                                   not null comment '主键ID'
        primary key,
    user_id         bigint unsigned                          not null comment '用户ID',
    stage_id        bigint unsigned                          not null comment '主目标id',
    title           varchar(100)                             not null comment '小目标标题',
    content         varchar(255)                             null comment '详细内容描述',
    learned_hours   decimal(10, 2) default 0.00              null comment '已学时长(小时)',
    estimated_hours decimal(10, 2) default 0.00              null comment '预计总用时(小时)',
    is_deleted      tinyint(1)     default 0                 null comment '软删除标记',
    create_time     datetime       default CURRENT_TIMESTAMP null comment '创建时间',
    update_time     datetime       default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '修改时间'
)
    comment '小目标表' charset = utf8mb4;
