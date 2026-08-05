create table if not exists search_index
(
    id          bigint unsigned not null comment 'primary key'
        primary key,
    user_id     bigint unsigned null comment 'owner user id',
    target_type tinyint not null comment '1000 entry, 1100 recipe, 1200 feed, 1300 goal, 1400 diet',
    target_id   bigint unsigned not null comment 'target id',
    title       varchar(255) null comment 'search title',
    content     longtext null comment 'search content',
    tags        varchar(500) null comment 'search tags',
    visibility  tinyint default 1000 not null comment '1000 private, 1100 public, 1200 friends',
    status      tinyint default 1000 not null comment '1000 active, 1100 deleted',
    create_time datetime default CURRENT_TIMESTAMP not null comment 'created time',
    update_time datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment 'updated time',
    constraint uk_search_index_target
        unique (target_type, target_id)
) engine = InnoDB default charset = utf8mb4 comment = 'search index table';

create index idx_search_index_user_type
    on search_index (user_id, target_type, status);

create table if not exists search_log
(
    id          bigint unsigned not null comment 'primary key'
        primary key,
    user_id     bigint unsigned null comment 'user id',
    keyword     varchar(128) not null comment 'search keyword',
    target_type tinyint null comment 'target type',
    result_count int default 0 not null comment 'result count',
    create_time datetime default CURRENT_TIMESTAMP not null comment 'created time'
) engine = InnoDB default charset = utf8mb4 comment = 'search log table';

create index idx_search_log_keyword_time
    on search_log (keyword, create_time);
