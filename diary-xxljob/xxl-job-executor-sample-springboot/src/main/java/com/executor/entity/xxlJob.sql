create table if not exists job_execution_log
(
    id             bigint unsigned not null comment 'primary key'
        primary key,
    job_name       varchar(128) not null comment 'job name',
    biz_id         varchar(128) null comment 'business id for idempotency',
    status         tinyint default 1100 not null comment '1000 success, 1100 running, 1200 failed',
    start_time     datetime not null comment 'job start time',
    end_time       datetime null comment 'job end time',
    total_count    int default 0 not null comment 'total count',
    success_count  int default 0 not null comment 'success count',
    fail_count     int default 0 not null comment 'fail count',
    error_message  varchar(1000) null comment 'error message',
    create_time    datetime default CURRENT_TIMESTAMP not null comment 'created time',
    update_time    datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment 'updated time',
    constraint uk_job_execution_biz
        unique (job_name, biz_id)
) engine = InnoDB default charset = utf8mb4 comment = 'job execution log table';

create table if not exists image_cleanup_record
(
    id          bigint unsigned not null comment 'primary key'
        primary key,
    object_key  varchar(255) not null comment 'oss object key',
    image_id    bigint unsigned null comment 'image id',
    status      tinyint default 1100 not null comment '1000 success, 1100 pending, 1200 failed',
    retry_count int default 0 not null comment 'retry count',
    create_time datetime default CURRENT_TIMESTAMP not null comment 'created time',
    update_time datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment 'updated time',
    constraint uk_image_cleanup_object_key
        unique (object_key)
) engine = InnoDB default charset = utf8mb4 comment = 'image cleanup record table';

create table if not exists user_push_config
(
    id          bigint unsigned not null comment 'primary key'
        primary key,
    user_id     bigint unsigned not null comment 'user id',
    nickname    varchar(64) null comment 'nickname',
    city        varchar(64) null comment 'city',
    push_type   varchar(32) not null comment 'wechat, email, sms, websocket',
    target_id   varchar(128) not null comment 'push target id',
    enabled     tinyint(1) default 1 not null comment '0 disabled, 1 enabled',
    create_time datetime default CURRENT_TIMESTAMP not null comment 'created time',
    update_time datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment 'updated time'
) engine = InnoDB default charset = utf8mb4 comment = 'user push config table';
