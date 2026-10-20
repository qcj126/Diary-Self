create table if not exists notify_message
(
    id            bigint unsigned not null comment 'primary key'
        primary key,
    user_id       bigint unsigned not null comment 'receiver user id',
    sender_id     bigint unsigned null comment 'sender user id',
    notify_type   tinyint not null comment '1000 system, 1100 like, 1200 comment, 1300 follow, 1400 goal, 1500 ai, 1600 file',
    title         varchar(128) not null comment 'message title',
    content       varchar(1000) null comment 'message content',
    biz_type      varchar(64) null comment 'business type',
    biz_id        bigint unsigned null comment 'business id',
    read_status   tinyint default 0 not null comment '0 unread, 1 read',
    ack_status    tinyint default 0 not null comment '0 not acked, 1 acked',
    push_status   tinyint default 0 not null comment '0 pending, 1 pushed, 2 failed',
    create_time   datetime default CURRENT_TIMESTAMP not null comment 'created time',
    update_time   datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment 'updated time'
) engine = InnoDB default charset = utf8mb4 comment = 'notify message table';

create index idx_notify_message_user_read
    on notify_message (user_id, read_status, create_time);

create table if not exists notify_connection
(
    id            bigint unsigned not null comment 'primary key'
        primary key,
    user_id       bigint unsigned not null comment 'user id',
    device_id     varchar(128) not null comment 'device id',
    channel_id    varchar(128) not null comment 'netty channel id',
    client_type   tinyint default 1000 not null comment '1000 web, 1100 app, 1200 mini program',
    status        tinyint default 1000 not null comment '1000 online, 1100 offline',
    last_heartbeat datetime null comment 'last heartbeat time',
    create_time   datetime default CURRENT_TIMESTAMP not null comment 'created time',
    update_time   datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment 'updated time',
    constraint uk_notify_connection_channel
        unique (channel_id)
) engine = InnoDB default charset = utf8mb4 comment = 'notify websocket connection table';

create index idx_notify_connection_user
    on notify_connection (user_id, status);
