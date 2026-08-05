create table if not exists diet_record
(
    id             bigint unsigned not null comment 'primary key'
        primary key,
    user_id        bigint unsigned not null comment 'user id',
    eat_time       datetime not null comment 'eat time',
    meal_type      tinyint not null comment '10 breakfast, 15 morning snack, 20 lunch, 25 afternoon snack, 30 dinner, 35 night snack',
    food_name      varchar(128) not null comment 'food name',
    calories       int default 0 not null comment 'calories',
    protein        decimal(10, 2) default 0.00 not null comment 'protein grams',
    fat            decimal(10, 2) default 0.00 not null comment 'fat grams',
    carbohydrate   decimal(10, 2) default 0.00 not null comment 'carbohydrate grams',
    fullness_score tinyint null comment 'fullness score 1-10',
    location       varchar(128) null comment 'meal location',
    note           varchar(500) null comment 'note',
    source_type    tinyint default 1000 not null comment '1000 manual, 1100 ai image, 1200 recipe, 1300 plan',
    image_id       bigint unsigned null comment 'food image id',
    ai_result_id   bigint unsigned null comment 'ai result id',
    deleted        tinyint(1) default 0 not null comment '0 normal, 1 deleted',
    create_time    datetime default CURRENT_TIMESTAMP not null comment 'created time',
    update_time    datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment 'updated time'
) engine = InnoDB default charset = utf8mb4 comment = 'diet record table';

create index idx_diet_record_user_eat_time
    on diet_record (user_id, eat_time, deleted);

create table if not exists diet_daily_target
(
    id                  bigint unsigned not null comment 'primary key'
        primary key,
    user_id             bigint unsigned not null comment 'user id',
    target_date         date not null comment 'target date',
    calorie_target      int default 0 not null comment 'calorie target',
    protein_target      decimal(10, 2) default 0.00 not null comment 'protein target grams',
    fat_target          decimal(10, 2) default 0.00 not null comment 'fat target grams',
    carbohydrate_target decimal(10, 2) default 0.00 not null comment 'carbohydrate target grams',
    create_time         datetime default CURRENT_TIMESTAMP not null comment 'created time',
    update_time         datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment 'updated time',
    constraint uk_diet_daily_target_user_date
        unique (user_id, target_date)
) engine = InnoDB default charset = utf8mb4 comment = 'diet daily target table';

create table if not exists diet_analysis_report
(
    id            bigint unsigned not null comment 'primary key'
        primary key,
    user_id       bigint unsigned not null comment 'user id',
    report_type   tinyint not null comment '1 daily, 2 weekly, 3 monthly',
    period_start  date not null comment 'period start',
    period_end    date not null comment 'period end',
    summary       varchar(1000) null comment 'summary',
    ai_result_id  bigint unsigned null comment 'ai result id',
    status        tinyint default 1000 not null comment '1000 generated, 1100 processing, 1200 failed',
    create_time   datetime default CURRENT_TIMESTAMP not null comment 'created time',
    update_time   datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment 'updated time'
) engine = InnoDB default charset = utf8mb4 comment = 'diet analysis report table';
