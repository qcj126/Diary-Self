create table if not exists social_feed
(
    id            bigint unsigned not null comment 'primary key'
        primary key,
    user_id       bigint unsigned not null comment 'publisher user id',
    source_type   tinyint not null comment '1000 entry, 1100 recipe, 1200 goal, 1300 time card',
    source_id     bigint unsigned not null comment 'source id',
    content       varchar(1000) null comment 'feed content',
    visibility    tinyint default 1100 not null comment '1000 private, 1100 public, 1200 friends',
    like_count    int unsigned default 0 not null comment 'like count',
    comment_count int unsigned default 0 not null comment 'comment count',
    collect_count int unsigned default 0 not null comment 'collect count',
    status        tinyint default 1000 not null comment '1000 published, 1100 hidden',
    deleted       tinyint(1) default 0 not null comment '0 normal, 1 deleted',
    create_time   datetime default CURRENT_TIMESTAMP not null comment 'created time',
    update_time   datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment 'updated time'
) engine = InnoDB default charset = utf8mb4 comment = 'social feed table';

create index idx_social_feed_time
    on social_feed (visibility, create_time, deleted);

create table if not exists social_like
(
    id          bigint unsigned not null comment 'primary key'
        primary key,
    user_id     bigint unsigned not null comment 'user id',
    target_type tinyint not null comment '1000 feed, 1100 comment, 1200 entry, 1300 recipe',
    target_id   bigint unsigned not null comment 'target id',
    status      tinyint default 1 not null comment '0 cancelled, 1 active',
    create_time datetime default CURRENT_TIMESTAMP not null comment 'created time',
    update_time datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment 'updated time',
    constraint uk_social_like_user_target
        unique (user_id, target_type, target_id)
) engine = InnoDB default charset = utf8mb4 comment = 'social like table';

create table if not exists social_comment
(
    id          bigint unsigned not null comment 'primary key'
        primary key,
    user_id     bigint unsigned not null comment 'comment user id',
    target_type tinyint not null comment '1000 feed, 1100 entry, 1200 recipe, 1300 goal',
    target_id   bigint unsigned not null comment 'target id',
    parent_id   bigint unsigned default 0 not null comment 'parent comment id',
    root_id     bigint unsigned default 0 not null comment 'root comment id',
    content     varchar(1000) not null comment 'comment content',
    like_count  int unsigned default 0 not null comment 'like count',
    status      tinyint default 1000 not null comment '1000 normal, 1100 hidden',
    deleted     tinyint(1) default 0 not null comment '0 normal, 1 deleted',
    create_time datetime default CURRENT_TIMESTAMP not null comment 'created time',
    update_time datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment 'updated time'
) engine = InnoDB default charset = utf8mb4 comment = 'social comment table';

create index idx_social_comment_target
    on social_comment (target_type, target_id, create_time);

create table if not exists social_collect
(
    id          bigint unsigned not null comment 'primary key'
        primary key,
    user_id     bigint unsigned not null comment 'user id',
    folder_id   bigint unsigned default 0 not null comment 'collect folder id',
    target_type tinyint not null comment '1000 feed, 1100 entry, 1200 recipe',
    target_id   bigint unsigned not null comment 'target id',
    status      tinyint default 1 not null comment '0 cancelled, 1 active',
    create_time datetime default CURRENT_TIMESTAMP not null comment 'created time',
    update_time datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment 'updated time',
    constraint uk_social_collect_user_target
        unique (user_id, target_type, target_id)
) engine = InnoDB default charset = utf8mb4 comment = 'social collect table';

create table if not exists social_follow
(
    id           bigint unsigned not null comment 'primary key'
        primary key,
    user_id      bigint unsigned not null comment 'follower user id',
    follow_id    bigint unsigned not null comment 'followed user id',
    status       tinyint default 1 not null comment '0 cancelled, 1 active',
    create_time  datetime default CURRENT_TIMESTAMP not null comment 'created time',
    update_time  datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment 'updated time',
    constraint uk_social_follow_user
        unique (user_id, follow_id)
) engine = InnoDB default charset = utf8mb4 comment = 'social follow table';
