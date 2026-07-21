create table time_category
(
    id            bigint unsigned                        not null comment '主键'
        primary key,
    user_id       bigint unsigned                        not null comment '用户ID',
    category_name varchar(20)                            not null comment '分类名称',
    category_num  int unsigned                           not null comment '分类编号 1100, 1200, 1300...',
    sort          int unsigned default '0'               not null comment '排序',
    deleted       tinyint(1)   default 0                 not null comment '是否删除：0-否 1-是',
    create_time   datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time   datetime     default CURRENT_TIMESTAMP not null comment '更新时间'
)
    comment '时光机分类表' charset = utf8mb4;

create table time_card
(
    id           bigint unsigned                      not null comment '主键'
        primary key,
    user_id      bigint unsigned                      not null comment '用户ID',
    image_id     bigint unsigned                      not null comment '图片ID',
    category_id  bigint unsigned                      not null comment '分类ID',
    card_title   varchar(50)                          not null comment '卡片标题',
    card_content varchar(300)                         not null comment '卡片内容',
    record_time  datetime                             not null comment '记录此事的时间',
    deleted      tinyint(1) default 0                 not null comment '是否删除：0-否 1-是',
    create_time  datetime   default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time  datetime   default CURRENT_TIMESTAMP not null comment '更新时间'
)
    comment '时间机分类卡片表' charset = utf8mb4;