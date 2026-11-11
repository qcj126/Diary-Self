-- 恋爱纪念册数据表（MySQL 8.0+）
--
-- 设计说明：
-- 1. 所有 BIGINT 主键均由应用侧生成（例如 Snowflake），不使用 AUTO_INCREMENT。
-- 2. 图片复用公共 image 表，通过 love_record_image.image_id 关联 image.id。
-- 3. 为适配微服务架构，本脚本不创建物理外键，数据一致性由业务层维护。
-- 4. 时间线的按周/月统计、年度回顾、地点访问次数均从 love_record 聚合计算。
-- 5. 经度、纬度采用 WGS84 坐标系；MySQL 8 可基于普通索引完成当前数据量查询。

create table love_couple
(
    id              bigint unsigned                            not null comment '主键'
        primary key,
    owner_user_id   bigint unsigned                            not null comment '创建者用户ID',
    partner_user_id bigint unsigned                            null comment '对方用户ID；对方未注册时为空',
    partner_name    varchar(32)                                not null comment '对方昵称',
    start_date      date                                       not null comment '在一起的第一天',
    status          tinyint unsigned default '1'               not null comment '状态：0-停用，1-正常',
    deleted         tinyint(1)       default 0                 not null comment '逻辑删除：0-否，1-是',
    create_time     datetime         default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time     datetime         default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间'
)
    comment '恋爱关系表' charset = utf8mb4;

create index idx_love_couple_owner
    on love_couple (owner_user_id, deleted);

create index idx_love_couple_partner
    on love_couple (partner_user_id, deleted);

create table love_anniversary
(
    id              bigint unsigned                             not null comment '主键'
        primary key,
    couple_id       bigint unsigned                             not null comment '恋爱关系ID',
    creator_user_id bigint unsigned                             not null comment '创建者用户ID',
    name            varchar(64)                                 not null comment '纪念日名称',
    event_date      date                                        not null comment '纪念日原始日期',
    repeat_type     tinyint unsigned  default '1'               not null comment '重复类型：0-不重复，1-每年重复',
    remind_days     smallint unsigned default '7'               not null comment '提前提醒天数',
    pinned          tinyint(1)        default 0                 not null comment '是否置顶：0-否，1-是',
    sort            int unsigned      default '0'               not null comment '排序值，越小越靠前',
    deleted         tinyint(1)        default 0                 not null comment '逻辑删除：0-否，1-是',
    create_time     datetime          default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time     datetime          default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间'
)
    comment '恋爱纪念日表' charset = utf8mb4;

create index idx_love_anniversary_pinned
    on love_anniversary (couple_id, pinned, sort);

create index idx_love_anniversary_upcoming
    on love_anniversary (couple_id, deleted, event_date);

-- auto-generated definition
create table love_record_mood
(
    id          bigint unsigned                        not null comment '主键'
        primary key,
    record_id   bigint unsigned                        not null comment '恋爱记录ID',
    mood_id     bigint unsigned                        not null comment '心情ID',
    sort        int unsigned default '0'               not null comment '展示排序值',
    create_time datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    constraint uk_love_record_mood
        unique (record_id, mood_id)
)
    comment '恋爱记录心情关联表' charset = utf8mb4;

create index idx_love_record_mood_mood
    on love_record_mood (mood_id, record_id);

create table love_location
(
    id          bigint unsigned                      not null comment '主键'
        primary key,
    couple_id   bigint unsigned                      not null comment '恋爱关系ID',
    name        varchar(100)                         not null comment '地点名称',
    address     varchar(255)                         null comment '详细地址',
    longitude   decimal(10, 7)                       null comment '经度，WGS84',
    latitude    decimal(10, 7)                       null comment '纬度，WGS84',
    city_code   varchar(20)                          null comment '城市行政区划编码',
    city_name   varchar(64)                          null comment '城市名称',
    deleted     tinyint(1) default 0                 not null comment '逻辑删除：0-否，1-是',
    create_time datetime   default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time datetime   default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间'
)
    comment '恋爱足迹地点表' charset = utf8mb4;

create index idx_love_location_coordinate
    on love_location (longitude, latitude);

create index idx_love_location_couple
    on love_location (couple_id, deleted);

create index idx_love_location_name
    on love_location (couple_id, name);


create table love_record
(
    id              bigint unsigned                        not null comment '主键'
        primary key,
    couple_id       bigint unsigned                        not null comment '恋爱关系ID',
    creator_user_id bigint unsigned                        not null comment '创建者用户ID',
    location_id     bigint unsigned                        null comment '地点ID，对应 love_location.id',
    title           varchar(100)                           not null comment '记录标题',
    content         text                                   null comment '完整记录内容',
    record_date     date                                   not null comment '记录发生日期',
    category_code   varchar(32)                            not null comment '分类编码：DATE-约会，DAILY-日常，TRAVEL-旅行，ANNIVERSARY-纪念日',
    important       tinyint(1)   default 0                 not null comment '是否重要回忆：0-否，1-是',
    sort            int unsigned default '0'               not null comment '同日期内排序值，越小越靠前',
    deleted         tinyint(1)   default 0                 not null comment '逻辑删除：0-否，1-是',
    create_time     datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time     datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    constraint chk_love_record_category
        check (`category_code` in (_utf8mb4\'DATE\',_utf8mb4\'DAILY\',_utf8mb4\'TRAVEL\',_utf8mb4\'ANNIVERSARY\'))
)
    comment '恋爱记录主表' charset=utf8mb4;

create index idx_love_record_category
    on love_record (couple_id asc, category_code asc, deleted asc, record_date desc);

create index idx_love_record_creator
    on love_record (creator_user_id, deleted);

create index idx_love_record_important
    on love_record (couple_id asc, important asc, deleted asc, record_date desc);

create index idx_love_record_location
    on love_record (location_id asc, deleted asc, record_date desc);

create index idx_love_record_timeline
    on love_record (couple_id asc, deleted asc, record_date desc, id desc);

create table love_record_tag
(
    id          bigint unsigned                        not null comment '主键'
        primary key,
    record_id   bigint unsigned                        not null comment '恋爱记录ID',
    tag_id      bigint unsigned                        not null comment '自定义标签ID',
    sort        int unsigned default '0'               not null comment '展示排序值',
    create_time datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    constraint uk_love_record_tag
        unique (record_id, tag_id)
)
    comment '恋爱记录标签关联表' charset = utf8mb4;

create index idx_love_record_tag_tag
    on love_record_tag (tag_id, record_id);

create table love_record_image
(
    id          bigint unsigned                        not null comment '主键'
        primary key,
    record_id   bigint unsigned                        not null comment '恋爱记录ID',
    image_id    bigint unsigned                        not null comment '公共图片ID，对应 image.id',
    is_cover    tinyint(1)   default 0                 not null comment '是否封面：0-否，1-是',
    sort        int unsigned default '0'               not null comment '图片排序值，越小越靠前',
    create_time datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    constraint uk_love_record_image
        unique (record_id, image_id)
)
    comment '恋爱记录图片关联表' charset = utf8mb4;

create index idx_love_record_image_id
    on love_record_image (image_id);

create index idx_love_record_image_sort
    on love_record_image (record_id, sort);


create table love_mood
(
    id          bigint unsigned                        not null comment '主键'
        primary key,
    mood_code   varchar(32)                            not null comment '心情编码',
    mood_name   varchar(32)                            not null comment '心情名称',
    emoji       varchar(16)                            null comment '展示用 Emoji',
    sort        int unsigned default '0'               not null comment '排序值，越小越靠前',
    enabled     tinyint(1)   default 1                 not null comment '是否启用：0-否，1-是',
    create_time datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    constraint uk_love_mood_code
        unique (mood_code)
)
    comment '恋爱记录心情字典表' charset = utf8mb4;

create table love_mood_record
(
    id          bigint unsigned                    not null comment '主键'
        primary key,
    mood_id     varchar(32)                        not null comment '心情配置id',
    record_id   bigint unsigned                    not null comment '记录id',
    couple_id   bigint unsigned                    not null comment '恋爱关系ID',
    create_time datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间'
)
    comment '恋爱记录心情表' charset = utf8mb4;


create table love_tag
(
    id              bigint unsigned                        not null comment '主键'
        primary key,
    record_id       bigint unsigned                        not null comment '恋爱记录ID',
    couple_id       bigint unsigned                        not null comment '恋爱关系ID',
    creator_user_id bigint unsigned                        not null comment '创建者用户ID',
    tag_name        varchar(32)                            not null comment '标签名称，不包含#',
    color           varchar(16)                            null comment '展示颜色，例如 #FF6B81',
    sort            int unsigned default '0'               not null comment '展示排序值',
    create_time     datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time     datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间'
)
    comment '恋爱记录自定义标签表' charset = utf8mb4;

create index idx_love_tag_couple
    on love_tag (couple_id);

create index idx_love_tag_name
    on love_tag (couple_id, tag_name);

create table love_menstrual_cycle
(
    id                bigint unsigned                             not null comment '主键'
        primary key,
    couple_id         bigint unsigned                             not null comment '恋爱关系ID',
    subject_user_id   bigint unsigned                             null comment '生理期所属用户ID；未注册时为空',
    recorder_user_id  bigint unsigned                             not null comment '记录者用户ID',
    period_start_date date                                        not null comment '本次生理期开始日期',
    period_end_date   date                                        null comment '本次生理期结束日期',
    cycle_length      smallint unsigned default '28'              not null comment '周期长度（天）',
    period_length     smallint unsigned default '5'               not null comment '经期长度（天）',
    symptoms          json                                        null comment '症状 JSON 数组，例如 ["腹痛","疲惫"]',
    note              varchar(500)                                null comment '备注或关怀记录',
    privacy_scope     tinyint unsigned  default '1'               not null comment '可见范围：0-仅本人，1-双方可见',
    deleted           tinyint(1)        default 0                 not null comment '逻辑删除：0-否，1-是',
    create_time       datetime          default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time       datetime          default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    constraint chk_love_cycle_dates
        check ((`period_end_date` is null) or (`period_end_date` >= `period_start_date`)),
    constraint chk_love_cycle_length
        check (`cycle_length` between 15 and 90),
    constraint chk_love_period_length
        check (`period_length` between 1 and 20)
)
    comment '生理期记录表' charset = utf8mb4;

create index idx_love_cycle_subject
    on love_menstrual_cycle (subject_user_id asc, deleted asc, period_start_date desc);

create index idx_love_cycle_timeline
    on love_menstrual_cycle (couple_id asc, deleted asc, period_start_date desc);

-- 初始化前端所需的心情字典。固定 ID 仅用于基础字典数据。
INSERT INTO `love_mood` (`id`, `mood_code`, `mood_name`, `emoji`, `sort`, `enabled`)
VALUES
    (1000, 'HEARTBEAT', '心动', '❤️', 10, 1),
    (1100, 'HAPPY',     '开心', '😊', 20, 1),
    (1200, 'HEALING',   '治愈', '🌿', 30, 1),
    (1300, 'TOUCHED',   '感动', '🥹', 40, 1),
    (1400, 'CALM',      '平静', '☁️', 50, 1),
    (1500, 'LAUGH',     '爆笑', '😆', 60, 1)
ON DUPLICATE KEY UPDATE
    `mood_name` = VALUES(`mood_name`),
    `emoji` = VALUES(`emoji`),
    `sort` = VALUES(`sort`),
    `enabled` = VALUES(`enabled`),
    `update_time` = CURRENT_TIMESTAMP;

