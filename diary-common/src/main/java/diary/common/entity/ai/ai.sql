create table if not exists ai_info
(
    id             bigint                             not null comment '主键'
    primary key,
    user_id        bigint unsigned                    not null comment '用户ID',
    model          varchar(20)                        not null comment 'AI模型',
    temperature    varchar(5)                         not null comment 'AI结果创意度',
    ai_type        tinyint(1)                         not null comment 'AI类别，对应着AI模型',
    ai_application tinyint                            not null comment 'AI用途：如鉴别营养成分，每日推荐菜品等',
    create_time    datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time    datetime default CURRENT_TIMESTAMP not null comment '更新时间'
) engine = InnoDB default charset = utf8mb4 comment = 'ai 信息表';

create index idx_ai_result_user_time
    on ai_info (user_id, create_time);

create table if not exists ai_nutrient
(
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
) engine = InnoDB default charset = utf8mb4 comment = 'ai 营养信息表';

create index idx_ai_nutrient_info_id
    on ai_nutrient (ai_info_id);

-- 以前这里只建普通索引，重复消息仍可为同一 task 写入多条结果。
-- 现在使用唯一索引，让结果幂等最终由数据库兜底。
create unique index uk_ai_nutrient_task_id
    on ai_nutrient (ai_task_id);

create index idx_ai_nutrient_user_time
    on ai_nutrient (user_id, create_time);

create table ai_task(
    id             bigint unsigned not null comment 'primary key'
        primary key,
    user_id         bigint unsigned not null comment 'user id',
    client_request_id varchar(128) not null comment 'client request id',
    task_type       varchar(64) not null comment 'task type',
    status          varchar(64) not null comment 'status',
    input_snapshot  longtext not null comment 'input snapshot',
    attempt_count   int not null comment 'attempt count',
    max_attempts    int not null comment 'max attempts',
    worker_id       varchar(128) null comment 'worker id',
    lease_until     datetime null comment 'lease until',
    ai_info_id      bigint unsigned null comment 'ai info id',
    error_code      varchar(64) null comment 'error code',
    error_message   longtext null comment 'error message',
    create_time     datetime default CURRENT_TIMESTAMP not null comment 'create time',
    queue_time      datetime null comment 'queue time',
    start_time      datetime null comment 'start time',
    finish_time     datetime null comment 'finish time',
    version_id      int unsigned not null comment 'version id'
) engine = InnoDB default charset = utf8mb4 comment = 'ai 任务表';

-- 实际数据库已经存在该唯一索引；以前 SQL 文档没有记录，按文档重建环境时会丢失提交幂等保障。
create unique index uk_ai_task_user_client_request
    on ai_task (user_id, client_request_id);
