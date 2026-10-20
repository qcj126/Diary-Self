CREATE TABLE icon (
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
) engine=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='图标库主表';
create index idx_user_id on icon (user_id);