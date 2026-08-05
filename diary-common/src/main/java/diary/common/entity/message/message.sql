create table if not exists message_center
(
    id            bigint unsigned not null comment 'primary key'
        primary key,
    user_id       bigint unsigned not null comment 'receiver user id',
    sender_id     bigint unsigned null comment 'sender user id',
    message_type  tinyint not null comment '1000 system, 1100 social, 1200 goal, 1300 diet, 1400 ai',
    title         varchar(128) not null comment 'message title',
    content       varchar(1000) null comment 'message content',
    biz_type      varchar(64) null comment 'business type',
    biz_id        bigint unsigned null comment 'business id',
    read_status   tinyint default 0 not null comment '0 unread, 1 read',
    deleted       tinyint(1) default 0 not null comment '0 normal, 1 deleted',
    create_time   datetime default CURRENT_TIMESTAMP not null comment 'created time',
    update_time   datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment 'updated time'
) engine = InnoDB default charset = utf8mb4 comment = 'message center table';

create index idx_message_center_user_read
    on message_center (user_id, read_status, create_time);

create table if not exists message_read_log
(
    id          bigint unsigned not null comment 'primary key'
        primary key,
    user_id     bigint unsigned not null comment 'user id',
    message_id  bigint unsigned not null comment 'message id',
    read_time   datetime default CURRENT_TIMESTAMP not null comment 'read time',
    constraint uk_message_read_log
        unique (user_id, message_id)
) engine = InnoDB default charset = utf8mb4 comment = 'message read log table';
