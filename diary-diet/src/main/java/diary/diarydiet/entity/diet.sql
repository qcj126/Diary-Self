create table diet_record
(
    id          bigint unsigned                           not null comment '雪花主键'
        primary key,
    user_id     bigint unsigned                           not null comment '用户ID',
    eat_time    datetime                                  not null comment '食用时间',
    meal_type   tinyint                                   not null comment '10早餐 15早加餐 20午餐 25午加餐 30晚餐 35夜宵',
    food_name   varchar(200)                              not null comment '食物/菜品名称',
    location    varchar(100) default 'home'               not null comment '用餐地点，前端使用home/outside',
    note        varchar(500)                              null comment '备注',
    image_url   varchar(1000)                             null comment '食物图片地址',
    deleted     tinyint(1)   default 0                    not null comment '逻辑删除：0正常 1删除',
    create_time datetime(3)  default CURRENT_TIMESTAMP(3) not null comment '创建时间',
    update_time datetime(3)  default CURRENT_TIMESTAMP(3) not null on update CURRENT_TIMESTAMP(3) comment '更新时间'
)
    comment '饮食记录表' charset = utf8mb4;

create index idx_diet_user_eat_time
    on diet_record (user_id, eat_time, deleted);

create index idx_diet_user_location_time
    on diet_record (user_id, location, eat_time);

create index idx_diet_user_meal_time
    on diet_record (user_id, meal_type, eat_time);