create table if not exists diary_plan
(
    id          bigint unsigned not null comment 'primary key'
        primary key,
    user_id     bigint unsigned not null comment 'user id',
    plan_type   tinyint not null comment '1000 diet, 1100 goal, 1200 habit, 1300 mixed',
    title       varchar(128) not null comment 'plan title',
    description varchar(500) null comment 'plan description',
    start_date  date not null comment 'start date',
    end_date    date not null comment 'end date',
    status      tinyint default 1000 not null comment '1000 active, 1100 completed, 1200 paused',
    deleted     tinyint(1) default 0 not null comment '0 normal, 1 deleted',
    create_time datetime default CURRENT_TIMESTAMP not null comment 'created time',
    update_time datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment 'updated time'
) engine = InnoDB default charset = utf8mb4 comment = 'plan table';

create index idx_diary_plan_user_type
    on diary_plan (user_id, plan_type, status);

create table if not exists diary_plan_item
(
    id          bigint unsigned not null comment 'primary key'
        primary key,
    plan_id     bigint unsigned not null comment 'plan id',
    user_id     bigint unsigned not null comment 'user id',
    item_type   tinyint not null comment '1000 diet, 1100 goal, 1200 habit, 1300 custom',
    target_id   bigint unsigned null comment 'linked target id',
    title       varchar(128) not null comment 'item title',
    plan_date   date not null comment 'plan date',
    sort        int default 0 not null comment 'sort value',
    status      tinyint default 1000 not null comment '1000 todo, 1100 done, 1200 skipped',
    create_time datetime default CURRENT_TIMESTAMP not null comment 'created time',
    update_time datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment 'updated time'
) engine = InnoDB default charset = utf8mb4 comment = 'plan item table';

create index idx_diary_plan_item_plan_date
    on diary_plan_item (plan_id, plan_date, status);
