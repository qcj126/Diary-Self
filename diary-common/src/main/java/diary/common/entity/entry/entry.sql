create table if not exists diary_entry
(
    id             bigint unsigned not null comment 'primary key'
        primary key,
    user_id        bigint unsigned not null comment 'author user id',
    title          varchar(128) not null comment 'entry title',
    content        longtext null comment 'markdown content',
    summary        varchar(500) null comment 'entry summary',
    mood           tinyint null comment 'mood code',
    weather        varchar(64) null comment 'weather',
    location       varchar(128) null comment 'location',
    privacy_type   tinyint default 1000 not null comment '1000 private, 1100 public, 1200 friends',
    status         tinyint default 1000 not null comment '1000 published, 1100 draft, 1200 archived',
    view_count     int unsigned default 0 not null comment 'view count',
    like_count     int unsigned default 0 not null comment 'like count',
    comment_count  int unsigned default 0 not null comment 'comment count',
    collect_count  int unsigned default 0 not null comment 'collect count',
    entry_date     date not null comment 'entry calendar date',
    deleted        tinyint(1) default 0 not null comment '0 normal, 1 deleted',
    create_time    datetime default CURRENT_TIMESTAMP not null comment 'created time',
    update_time    datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment 'updated time'
) engine = InnoDB default charset = utf8mb4 comment = 'diary entry table';

create index idx_diary_entry_user_date
    on diary_entry (user_id, entry_date, deleted);

create index idx_diary_entry_privacy_time
    on diary_entry (privacy_type, create_time);

create table if not exists diary_entry_image
(
    id          bigint unsigned not null comment 'primary key'
        primary key,
    entry_id    bigint unsigned not null comment 'entry id',
    user_id     bigint unsigned not null comment 'user id',
    image_id    bigint unsigned not null comment 'image id',
    sort        int default 0 not null comment 'sort value',
    create_time datetime default CURRENT_TIMESTAMP not null comment 'created time'
) engine = InnoDB default charset = utf8mb4 comment = 'diary entry image table';

create index idx_diary_entry_image_entry
    on diary_entry_image (entry_id, sort);

create table if not exists diary_entry_tag
(
    id          bigint unsigned not null comment 'primary key'
        primary key,
    user_id     bigint unsigned not null comment 'user id',
    tag_name    varchar(32) not null comment 'tag name',
    color       varchar(16) null comment 'tag color',
    create_time datetime default CURRENT_TIMESTAMP not null comment 'created time',
    update_time datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment 'updated time',
    constraint uk_diary_entry_tag_user_name
        unique (user_id, tag_name)
) engine = InnoDB default charset = utf8mb4 comment = 'diary entry tag table';

create table if not exists diary_entry_tag_relation
(
    id          bigint unsigned not null comment 'primary key'
        primary key,
    entry_id    bigint unsigned not null comment 'entry id',
    tag_id      bigint unsigned not null comment 'tag id',
    create_time datetime default CURRENT_TIMESTAMP not null comment 'created time',
    constraint uk_diary_entry_tag_relation
        unique (entry_id, tag_id)
) engine = InnoDB default charset = utf8mb4 comment = 'diary entry tag relation table';
