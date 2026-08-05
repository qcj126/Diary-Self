create table if not exists stage_goal
(
    id              bigint unsigned not null comment 'primary key'
        primary key,
    user_id         bigint unsigned not null comment 'user id',
    category        varchar(32) not null comment 'goal category',
    title           varchar(100) not null comment 'goal title',
    description     varchar(500) null comment 'goal description',
    learned_hours   decimal(10, 2) default 0.00 not null comment 'learned hours',
    estimated_hours decimal(10, 2) default 0.00 not null comment 'estimated hours',
    progress        decimal(5, 2) default 0.00 not null comment 'progress percent',
    status          tinyint default 1000 not null comment '1000 active, 1100 completed, 1200 paused, 1300 overdue',
    priority        tinyint default 2 not null comment '1 low, 2 normal, 3 high',
    start_time      datetime null comment 'goal start time',
    end_time        datetime not null comment 'goal end time',
    version         int default 0 not null comment 'optimistic lock version',
    deleted         tinyint(1) default 0 not null comment '0 normal, 1 deleted',
    create_time     datetime default CURRENT_TIMESTAMP not null comment 'created time',
    update_time     datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment 'updated time'
) engine = InnoDB default charset = utf8mb4 comment = 'stage goal table';

create index idx_stage_goal_user_status
    on stage_goal (user_id, status, deleted);

create index idx_stage_goal_end_time
    on stage_goal (end_time);

create table if not exists sub_goal
(
    id              bigint unsigned not null comment 'primary key'
        primary key,
    user_id         bigint unsigned not null comment 'user id',
    stage_id        bigint unsigned not null comment 'stage goal id',
    title           varchar(100) not null comment 'sub goal title',
    content         varchar(500) null comment 'sub goal content',
    learned_hours   decimal(10, 2) default 0.00 not null comment 'learned hours',
    estimated_hours decimal(10, 2) default 0.00 not null comment 'estimated hours',
    progress        decimal(5, 2) default 0.00 not null comment 'progress percent',
    status          tinyint default 1000 not null comment '1000 active, 1100 completed, 1200 paused, 1300 overdue',
    checkin_count   int default 0 not null comment 'check-in count',
    end_time        datetime not null comment 'sub goal end time',
    version         int default 0 not null comment 'optimistic lock version',
    deleted         tinyint(1) default 0 not null comment '0 normal, 1 deleted',
    create_time     datetime default CURRENT_TIMESTAMP not null comment 'created time',
    update_time     datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment 'updated time'
) engine = InnoDB default charset = utf8mb4 comment = 'sub goal table';

create index idx_sub_goal_stage_id
    on sub_goal (stage_id);

create index idx_sub_goal_user_status
    on sub_goal (user_id, status, deleted);

create table if not exists goal_export_task
(
    id            bigint unsigned not null comment 'primary key'
        primary key,
    user_id       bigint unsigned not null comment 'user id',
    task_no       varchar(64) not null comment 'export task number',
    export_type   tinyint not null comment '1 weekly, 2 monthly, 3 custom',
    status        tinyint default 1100 not null comment '1000 success, 1100 processing, 1200 failed',
    file_id       bigint unsigned null comment 'export file id',
    error_message varchar(500) null comment 'error message',
    create_time   datetime default CURRENT_TIMESTAMP not null comment 'created time',
    update_time   datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment 'updated time',
    constraint uk_goal_export_task_no
        unique (task_no)
) engine = InnoDB default charset = utf8mb4 comment = 'goal export task table';
