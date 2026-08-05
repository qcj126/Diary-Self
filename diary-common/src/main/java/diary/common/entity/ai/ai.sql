create table if not exists ai_result
(
    id             bigint unsigned not null comment 'primary key'
        primary key,
    user_id        bigint unsigned not null comment 'user id',
    model          varchar(64) not null comment 'ai model name',
    temperature    decimal(3, 2) default 0.70 not null comment 'model temperature',
    ai_type        tinyint not null comment 'ai provider type',
    ai_application tinyint not null comment 'application type',
    prompt         text null comment 'prompt content',
    request_text   text null comment 'request text',
    response_text  longtext null comment 'response text',
    status         tinyint default 1000 not null comment '1000 success, 1100 processing, 1200 failed',
    error_message  varchar(500) null comment 'error message',
    create_time    datetime default CURRENT_TIMESTAMP not null comment 'created time',
    update_time    datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment 'updated time'
) engine = InnoDB default charset = utf8mb4 comment = 'ai result table';

create index idx_ai_result_user_time
    on ai_result (user_id, create_time);

create table if not exists ai_nutrient
(
    id              bigint unsigned not null comment 'primary key'
        primary key,
    user_id         bigint unsigned not null comment 'user id',
    ai_result_id    bigint unsigned not null comment 'ai result id',
    food_name       varchar(128) null comment 'food name',
    calory          int default 0 not null comment 'calories',
    protein         decimal(10, 2) default 0.00 not null comment 'protein grams',
    fat             decimal(10, 2) default 0.00 not null comment 'fat grams',
    carbohydrate    decimal(10, 2) default 0.00 not null comment 'carbohydrate grams',
    sugar           decimal(10, 2) default 0.00 not null comment 'sugar grams',
    sodium          decimal(10, 2) default 0.00 not null comment 'sodium mg',
    confidence      decimal(5, 2) default 0.00 not null comment 'recognition confidence',
    create_time     datetime default CURRENT_TIMESTAMP not null comment 'created time',
    update_time     datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment 'updated time'
) engine = InnoDB default charset = utf8mb4 comment = 'ai nutrient table';

create index idx_ai_nutrient_result_id
    on ai_nutrient (ai_result_id);

create index idx_ai_nutrient_user_time
    on ai_nutrient (user_id, create_time);
