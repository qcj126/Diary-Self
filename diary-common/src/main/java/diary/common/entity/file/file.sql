create table if not exists file_resource
(
    id            bigint unsigned not null comment 'primary key'
        primary key,
    user_id       bigint unsigned not null comment 'user id',
    bucket_name   varchar(128) null comment 'oss bucket name',
    object_key    varchar(255) not null comment 'oss object key',
    original_name varchar(255) not null comment 'original file name',
    file_hash     varchar(128) null comment 'file hash',
    file_size     bigint unsigned not null comment 'file size bytes',
    mime_type     varchar(100) null comment 'mime type',
    file_type     tinyint default 1000 not null comment '1000 image, 1100 video, 1200 document, 1300 audio, 1900 other',
    status        tinyint default 1100 not null comment '1000 success, 1100 uploading, 1200 failed, 1300 processing',
    deleted       tinyint(1) default 0 not null comment '0 normal, 1 deleted',
    create_time   datetime default CURRENT_TIMESTAMP not null comment 'created time',
    update_time   datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment 'updated time',
    constraint uk_file_resource_object_key
        unique (object_key)
) engine = InnoDB default charset = utf8mb4 comment = 'file resource table';

create index idx_file_resource_hash
    on file_resource (file_hash);

create index idx_file_resource_user
    on file_resource (user_id, file_type, deleted);

create table if not exists file_upload_task
(
    id              bigint unsigned not null comment 'primary key'
        primary key,
    user_id         bigint unsigned not null comment 'user id',
    file_id         bigint unsigned null comment 'file resource id',
    upload_id       varchar(128) null comment 'multipart upload id',
    file_hash       varchar(128) null comment 'file hash',
    total_part      int default 1 not null comment 'total part count',
    uploaded_part   int default 0 not null comment 'uploaded part count',
    status          tinyint default 1100 not null comment '1000 success, 1100 uploading, 1200 failed, 1300 cancelled',
    error_message   varchar(500) null comment 'error message',
    create_time     datetime default CURRENT_TIMESTAMP not null comment 'created time',
    update_time     datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment 'updated time'
) engine = InnoDB default charset = utf8mb4 comment = 'file upload task table';

create index idx_file_upload_task_user_status
    on file_upload_task (user_id, status, create_time);

create table if not exists file_process_task
(
    id             bigint unsigned not null comment 'primary key'
        primary key,
    file_id        bigint unsigned not null comment 'file resource id',
    process_type   tinyint not null comment '1000 thumbnail, 1100 compress, 1200 watermark, 1300 cover',
    status         tinyint default 1100 not null comment '1000 success, 1100 processing, 1200 failed',
    result_file_id bigint unsigned null comment 'result file id',
    retry_count    int default 0 not null comment 'retry count',
    error_message  varchar(500) null comment 'error message',
    create_time    datetime default CURRENT_TIMESTAMP not null comment 'created time',
    update_time    datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment 'updated time'
) engine = InnoDB default charset = utf8mb4 comment = 'file process task table';
