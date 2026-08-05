create table if not exists time_category
(
    id            bigint unsigned not null comment 'primary key'
        primary key,
    user_id       bigint unsigned not null comment 'user id',
    category_name varchar(32) not null comment 'category name',
    category_num  int unsigned not null comment 'category number',
    sort          int unsigned default 0 not null comment 'sort value',
    deleted       tinyint(1) default 0 not null comment '0 normal, 1 deleted',
    create_time   datetime default CURRENT_TIMESTAMP not null comment 'created time',
    update_time   datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment 'updated time',
    constraint uk_time_category_user_num
        unique (user_id, category_num)
) engine = InnoDB default charset = utf8mb4 comment = 'time machine category table';

create table if not exists time_card
(
    id           bigint unsigned not null comment 'primary key'
        primary key,
    user_id      bigint unsigned not null comment 'user id',
    image_id     bigint unsigned null comment 'image id',
    category_id  bigint unsigned not null comment 'category id',
    card_title   varchar(80) not null comment 'card title',
    card_content varchar(1000) not null comment 'card content',
    record_time  datetime not null comment 'record time',
    open_time    datetime null comment 'time capsule open time',
    status       tinyint default 1000 not null comment '1000 normal, 1100 sealed, 1200 opened',
    deleted      tinyint(1) default 0 not null comment '0 normal, 1 deleted',
    create_time  datetime default CURRENT_TIMESTAMP not null comment 'created time',
    update_time  datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment 'updated time'
) engine = InnoDB default charset = utf8mb4 comment = 'time machine card table';

create index idx_time_card_user_record
    on time_card (user_id, record_time, deleted);

create index idx_time_card_category
    on time_card (category_id, deleted);
