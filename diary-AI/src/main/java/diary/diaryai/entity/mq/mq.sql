-- auto-generated definition
create table mq_outbox
(
    id                bigint auto_increment comment '主键 ID'
        primary key,
    event_id          varchar(64)                        not null comment '事件 ID（全局唯一）',
    source_event_id   varchar(64)                        null comment '触发当前 Retry 的来源消息 ID',
    aggregate_type    varchar(64)                        not null comment '聚合根类型',
    aggregate_id      bigint                             not null comment '聚合根 ID  当前为taskId',
    event_type        varchar(128)                       not null comment '事件类型',
    topic             varchar(128)                       not null comment 'MQ Topic 名称',
    tag               varchar(64)                        null comment 'MQ Tag（RocketMQ 特有）',
    message_key       varchar(128)                       null comment '消息 Key',
    payload           json                               not null comment '消息体（JSON 格式）',
    schema_version    int      default 1                 null comment '消息体 Schema 版本',
    status            varchar(32)                        not null comment '消息状态：NEW/SENDING/RETRY_WAIT/SENT/DEAD',
    retry_count       int      default 0                 null comment '当前重试次数',
    max_retries       int      default 3                 null comment '最大重试次数',
    next_retry_time   datetime                           null comment '下次重试时间',
    broker_message_id varchar(128)                       null comment 'MQ 返回的消息 ID',
    last_error        text                               null comment '最后一次错误信息',
    sent_time         datetime                           null comment '消息发送时间',
    create_time       datetime                           not null comment '创建时间',
    update_time       datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    version_id        int      default 0                 not null comment '乐观锁版本号',
    constraint uk_mq_outbox_event_id
        unique (event_id),
    constraint uk_retry_source_event
        unique (event_type, source_event_id)
)
    comment 'MQ 发件箱表' collate = utf8mb4_unicode_ci;

create index idx_mq_outbox_aggregate
    on mq_outbox (aggregate_type, aggregate_id);

create index idx_mq_outbox_publish
    on mq_outbox (status, next_retry_time, update_time);

create index idx_status_update
    on mq_outbox (status, update_time, id);

