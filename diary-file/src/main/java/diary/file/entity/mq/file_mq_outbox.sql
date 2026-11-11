create table file_mq_outbox
(
    id                bigint auto_increment comment '发件箱记录主键'
        primary key,
    event_id          varchar(64)                        not null comment '业务事件唯一 ID；重发时保持不变',
    image_id          bigint                             not null comment '关联的 image.id',
    event_type        varchar(64)                        not null comment '事件类型，当前为 IMAGE_UPLOADED',
    topic             varchar(128)                       not null comment '事件投递的 RocketMQ Topic',
    tag               varchar(64)                        not null comment '事件投递的 RocketMQ Tag',
    message_key       varchar(128)                       not null comment 'RocketMQ 消息检索 Key，当前使用 event_id',
    payload           json                               not null comment '固定的事件 JSON；重发时不重新构造',
    schema_version    int                                not null comment '消息协议版本，当前 IMAGE_UPLOADED 为 1',
    status            varchar(32)                        not null comment '发布状态：NEW、SENDING、RETRY_WAIT、SENT、DEAD',
    retry_count       int      default 0                 not null comment '额外发送重试次数，不含首次发送',
    max_retries       int                                not null comment '允许的最大额外发送重试次数',
    next_retry_time   datetime                           not null comment '下一次可领取发送的时间',
    broker_message_id varchar(128)                       null comment 'RocketMQ 确认发送后返回的消息 ID',
    last_error        varchar(500)                       null comment '最近一次发送失败或超时的原因',
    sent_time         datetime                           null comment '确认消息已发送到 Broker 的时间',
    create_time       datetime default CURRENT_TIMESTAMP not null comment '发件箱记录创建时间',
    update_time       datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '记录最近更新时间；用于判断 SENDING 超时',
    version_id        int      default 0                 not null comment '领取与确认发送时使用的乐观锁版本',
    constraint uk_file_outbox_event_id
        unique (event_id),
    constraint uk_file_outbox_image_event
        unique (image_id, event_type)
)
    comment 'diary-file 图片事件发件箱' charset = utf8mb4;

create index idx_file_outbox_ready
    on file_mq_outbox (status, next_retry_time, id);

create index idx_file_outbox_sending
    on file_mq_outbox (status, update_time, id);