create table if not exists statistics_daily
(
    id             bigint unsigned not null comment 'primary key'
        primary key,
    user_id        bigint unsigned not null comment 'user id',
    stat_date      date not null comment 'statistics date',
    entry_count    int default 0 not null comment 'entry count',
    diet_count     int default 0 not null comment 'diet record count',
    goal_done_count int default 0 not null comment 'completed goal count',
    habit_done_count int default 0 not null comment 'habit check-in count',
    total_calories int default 0 not null comment 'total calories',
    study_hours    decimal(10, 2) default 0.00 not null comment 'study hours',
    mood_avg       decimal(5, 2) null comment 'average mood',
    create_time    datetime default CURRENT_TIMESTAMP not null comment 'created time',
    update_time    datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment 'updated time',
    constraint uk_statistics_daily_user_date
        unique (user_id, stat_date)
) engine = InnoDB default charset = utf8mb4 comment = 'daily statistics table';

create table if not exists statistics_report
(
    id           bigint unsigned not null comment 'primary key'
        primary key,
    user_id      bigint unsigned not null comment 'user id',
    report_type  tinyint not null comment '1 weekly, 2 monthly, 3 yearly',
    period_start date not null comment 'period start',
    period_end   date not null comment 'period end',
    report_data  json null comment 'report data json',
    summary      varchar(1000) null comment 'report summary',
    file_id      bigint unsigned null comment 'export file id',
    status       tinyint default 1000 not null comment '1000 generated, 1100 processing, 1200 failed',
    create_time  datetime default CURRENT_TIMESTAMP not null comment 'created time',
    update_time  datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment 'updated time'
) engine = InnoDB default charset = utf8mb4 comment = 'statistics report table';
