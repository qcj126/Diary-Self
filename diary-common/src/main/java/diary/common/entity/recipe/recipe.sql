-- auto-generated definition
create table recipe
(
    id           bigint unsigned                        not null comment '主键'
        primary key,
    user_id      bigint unsigned                        not null comment '创建者用户ID',
    title        varchar(90)                            not null comment '标题',
    image_id     bigint unsigned                        not null comment '封面图ID',
    description  varchar(300)                           not null comment '简介',
    category     tinyint                                not null comment '分类：0-家常 1-西餐 2-甜点 3-汤粥 4-其他',
    meal_type    tinyint                                not null comment '餐别：1-早餐 2-午餐 3-晚餐 4-夜宵',
    difficulty   tinyint                                not null comment '难度：1-5',
    cooking_time tinyint                                not null comment '烹饪时长（分钟）',
    story        varchar(300)                           not null comment '情感故事/备注',
    sort         int unsigned default '0'               not null comment '排序',
    deleted      tinyint(1)   default 0                 not null comment '是否删除：0-否 1-是',
    create_time  datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time  datetime     default CURRENT_TIMESTAMP not null comment '更新时间',
    constraint idx_recipe_title
        unique (title) comment '标题唯一索引'
)
    comment '食谱主表' charset = utf8mb4;


-- auto-generated definition
create table recipe_ingredient
(
    id          bigint unsigned                        not null comment '主键'
        primary key,
    recipe_id   bigint unsigned                        not null comment '食谱ID',
    user_id     bigint unsigned                        not null comment '创建者用户ID',
    name        varchar(255)                           not null comment '食材名称',
    quantity    varchar(255)                           not null comment '食材数量',
    is_main     tinyint                                not null comment '是否主料：0-否 1-是',
    sort        int unsigned default '0'               not null comment '排序',
    deleted     tinyint(1)   default 0                 not null comment '是否删除：0-否 1-是',
    create_time datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time datetime     default CURRENT_TIMESTAMP not null comment '更新时间'
)
    comment '食谱食材表' charset = utf8mb4;

create index idx_recipe_ingredient_recipe_id
    on recipe_ingredient (recipe_id)
    comment '食谱ID索引';

-- auto-generated definition
create table recipe_step
(
    id           bigint unsigned                        not null comment '主键'
        primary key,
    recipe_id    bigint unsigned                        not null comment '食谱ID',
    user_id      bigint unsigned                        not null comment '创建者用户ID',
    step_number  tinyint                                not null comment '步骤编号',
    description  varchar(300)                           not null comment '步骤描述',
    timer_minute tinyint                                not null comment '步骤计时（分钟）',
    sort         int unsigned default '0'               not null comment '排序',
    deleted      tinyint(1)   default 0                 not null comment '是否删除：0-否 1-是',
    create_time  datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time  datetime     default CURRENT_TIMESTAMP not null comment '更新时间'
)
    comment '食谱步骤表' charset = utf8mb4;

create index idx_recipe_step_recipe_id
    on recipe_step (recipe_id)
    comment '食谱ID索引';

