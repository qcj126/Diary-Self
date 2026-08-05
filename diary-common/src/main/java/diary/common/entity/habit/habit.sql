create table if not exists habit
(
    id             bigint unsigned not null comment 'primary key'
        primary key,
    user_id        bigint unsigned not null comment 'user id',
    habit_name     varchar(128) not null comment 'habit name',
    description    varchar(500) null comment 'description',
    cycle_type     tinyint default 1000 not null comment '1000 daily, 1100 weekly, 1200 monthly',
    target_count   int default 1 not null comment 'target count per cycle',
    current_streak int default 0 not null comment 'current streak days',
    longest_streak int default 0 not null comment 'longest streak days',
    status         tinyint default 1000 not null comment '1000 active, 1100 paused, 1200 completed',
    deleted        tinyint(1) default 0 not null comment '0 normal, 1 deleted',
    create_time    datetime default CURRENT_TIMESTAMP not null comment 'created time',
    update_time    datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment 'updated time'
) engine = InnoDB default charset = utf8mb4 comment = 'habit table';

create index idx_habit_user_status
    on habit (user_id, status, deleted);

create table if not exists habit_checkin
(
    id            bigint unsigned not null comment 'primary key'
        primary key,
    habit_id      bigint unsigned not null comment 'habit id',
    user_id       bigint unsigned not null comment 'user id',
    checkin_date  date not null comment 'check-in date',
    checkin_count int default 1 not null comment 'check-in count',
    note          varchar(500) null comment 'note',
    makeup_flag   tinyint(1) default 0 not null comment '0 normal, 1 make-up',
    create_time   datetime default CURRENT_TIMESTAMP not null comment 'created time',
    update_time   datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment 'updated time',
    constraint uk_habit_checkin_date
        unique (habit_id, checkin_date)
) engine = InnoDB default charset = utf8mb4 comment = 'habit check-in table';
