create table if not exists chat_session
(
    id             bigint unsigned not null comment 'primary key'
        primary key,
    session_type   tinyint not null comment '1000 single, 1100 group',
    owner_id       bigint unsigned null comment 'owner user id',
    session_name   varchar(128) null comment 'session name',
    avatar_image_id bigint unsigned null comment 'avatar image id',
    status         tinyint default 1000 not null comment '1000 normal, 1100 muted, 1200 closed',
    create_time    datetime default CURRENT_TIMESTAMP not null comment 'created time',
    update_time    datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment 'updated time'
) engine = InnoDB default charset = utf8mb4 comment = 'chat session table';

create table if not exists chat_session_member
(
    id             bigint unsigned not null comment 'primary key'
        primary key,
    session_id     bigint unsigned not null comment 'session id',
    user_id        bigint unsigned not null comment 'member user id',
    member_role    tinyint default 1000 not null comment '1000 member, 1100 admin, 1200 owner',
    mute_end_time  datetime null comment 'mute end time',
    last_read_msg_id bigint unsigned default 0 not null comment 'last read message id',
    status         tinyint default 1000 not null comment '1000 normal, 1100 exited',
    create_time    datetime default CURRENT_TIMESTAMP not null comment 'created time',
    update_time    datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment 'updated time',
    constraint uk_chat_session_member
        unique (session_id, user_id)
) engine = InnoDB default charset = utf8mb4 comment = 'chat session member table';

create table if not exists chat_message
(
    id            bigint unsigned not null comment 'primary key'
        primary key,
    session_id    bigint unsigned not null comment 'session id',
    sender_id     bigint unsigned not null comment 'sender user id',
    client_msg_id varchar(128) null comment 'client message id for idempotency',
    msg_type      tinyint not null comment '1000 text, 1100 image, 1200 file, 1300 system',
    content       varchar(2000) null comment 'message content',
    file_id       bigint unsigned null comment 'file id',
    send_status   tinyint default 1000 not null comment '1000 sent, 1100 recalled, 1200 failed',
    ack_status    tinyint default 0 not null comment '0 not acked, 1 acked',
    create_time   datetime default CURRENT_TIMESTAMP not null comment 'created time',
    update_time   datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment 'updated time',
    constraint uk_chat_client_msg
        unique (sender_id, client_msg_id)
) engine = InnoDB default charset = utf8mb4 comment = 'chat message table';

create index idx_chat_message_session_time
    on chat_message (session_id, create_time);
