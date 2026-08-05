create table if not exists image
(
    id            bigint unsigned not null comment 'primary key'
        primary key,
    user_id       bigint unsigned not null comment 'user id',
    file_size     bigint unsigned not null comment 'file size bytes',
    original_name varchar(255) not null comment 'original file name',
    mime_type     varchar(100) not null comment 'mime type',
    type          int not null comment '1000 normal, 1100 thumbnail, 1200 cropped, 1300 cover, 2000 unknown',
    deleted       tinyint(1) default 0 not null comment '0 normal, 1 deleted',
    status        int not null comment '1000 success, 1100 failed, 1200 uploading',
    object_key    varchar(255) not null comment 'oss object key',
    url           varchar(500) null comment 'public or signed url',
    width         int null comment 'image width',
    height        int null comment 'image height',
    create_time   datetime default CURRENT_TIMESTAMP not null comment 'created time',
    update_time   datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment 'updated time',
    constraint uk_image_object_key
        unique (object_key)
) engine = InnoDB default charset = utf8mb4 comment = 'common image table';

create index idx_image_user_id
    on image (user_id, deleted);

create index idx_image_status
    on image (status, create_time);
