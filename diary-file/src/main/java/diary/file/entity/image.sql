-- auto-generated definition
create table image
(
    id            bigint unsigned                      not null comment '主键'
        primary key,
    user_id       bigint unsigned                      not null comment '用户ID',
    file_size     bigint unsigned                      not null comment '文件大小（字节）',
    original_name varchar(255)                         not null comment '原始文件名',
    mime_type     varchar(100)                         not null comment 'MIME类型，如 image/jpeg',
    type          int                                  not null comment '图片类型 ',
    deleted       tinyint(1) default 0                 not null comment '是否删除：0-否 1-是',
    status        int                                  not null comment '状态：1200 上传中 1100 失败 1000 成功',
    object_key    varchar(255)                         not null comment '对象存储的key',
    create_time   datetime   default CURRENT_TIMESTAMP not null comment '上传时间',
    update_time   datetime   default CURRENT_TIMESTAMP not null comment '更新时间',
    constraint idx_images_object_key
        unique (object_key)
)
    comment '公共图片表' charset = utf8mb4;

create index idx_images_user_id
    on image (user_id);


create table icon
(
    id          bigint auto_increment comment '图标ID'
        primary key,
    icon_name   varchar(64)                        not null comment '图标名称',
    icon_type   tinyint  default 1                 null comment '图标类型：1-PNG，2-SVG，3-字体图标',
    icon_path   varchar(255)                       not null comment '图标文件存储路径',
    icon_size   int                                not null comment '文件大小（字节）',
    icon_pixel  int                                not null comment '图标像素大小',
    user_id     bigint                             not null comment '用户ID',
    create_time datetime default CURRENT_TIMESTAMP null comment '创建时间',
    update_time datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间'
)
    comment '图标库主表' charset = utf8mb4;

create index idx_user_id
    on icon (user_id);