create table if not exists user
(
    user_id     bigint unsigned not null comment 'user id'
        primary key,
    username    varchar(64) not null comment 'username',
    nickname    varchar(64) null comment 'nickname',
    avatar_id   bigint unsigned null comment 'avatar image id',
    email       varchar(128) null comment 'email',
    phone       varchar(32) null comment 'phone',
    password    varchar(128) not null comment 'bcrypt password',
    status      int default 1000 not null comment '1000 enabled, 0 disabled',
    create_time datetime default CURRENT_TIMESTAMP not null comment 'created time',
    update_time datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment 'updated time',
    constraint uk_user_email
        unique (email),
    constraint uk_user_phone
        unique (phone),
    constraint uk_user_username
        unique (username)
) engine = InnoDB default charset = utf8mb4 comment = 'user table';

create table if not exists role
(
    role_id     bigint unsigned not null comment 'role id'
        primary key,
    role_code   varchar(32) not null comment 'role code: admin/user',
    role_name   varchar(64) not null comment 'role name',
    description varchar(255) null comment 'description',
    status      int default 1000 not null comment '1000 enabled, 0 disabled',
    create_time datetime default CURRENT_TIMESTAMP not null comment 'created time',
    update_time datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment 'updated time',
    constraint uk_role_code
        unique (role_code)
) engine = InnoDB default charset = utf8mb4 comment = 'role table';

create table if not exists user_role
(
    id          bigint unsigned not null comment 'primary key'
        primary key,
    user_id     bigint unsigned not null comment 'user id',
    role_id     bigint unsigned not null comment 'role id',
    create_time datetime default CURRENT_TIMESTAMP not null comment 'created time',
    constraint uk_user_role
        unique (user_id, role_id)
) engine = InnoDB default charset = utf8mb4 comment = 'user role relation table';

create index idx_user_role_role_id
    on user_role (role_id);

create table if not exists user_token
(
    id             bigint unsigned not null comment 'primary key'
        primary key,
    user_id        bigint unsigned not null comment 'user id',
    access_jti     varchar(128) not null comment 'access token jti',
    refresh_jti    varchar(128) not null comment 'refresh token jti',
    device_id      varchar(128) null comment 'device id',
    client_type    tinyint default 1000 not null comment '1000 web, 1100 app, 1200 mini program',
    expire_time    datetime not null comment 'access token expire time',
    refresh_expire_time datetime not null comment 'refresh token expire time',
    status         tinyint default 1000 not null comment '1000 active, 1100 refreshed, 1200 revoked, 1300 expired',
    create_time    datetime default CURRENT_TIMESTAMP not null comment 'created time',
    update_time    datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment 'updated time',
    constraint uk_user_token_access_jti
        unique (access_jti),
    constraint uk_user_token_refresh_jti
        unique (refresh_jti)
) engine = InnoDB default charset = utf8mb4 comment = 'user token table';

create index idx_user_token_user_status
    on user_token (user_id, status, create_time);

create table if not exists user_login_device
(
    id              bigint unsigned not null comment 'primary key'
        primary key,
    user_id         bigint unsigned not null comment 'user id',
    device_id       varchar(128) not null comment 'device id',
    device_name     varchar(128) null comment 'device name',
    client_type     tinyint default 1000 not null comment '1000 web, 1100 app, 1200 mini program',
    login_ip        varchar(64) null comment 'login ip',
    login_location  varchar(128) null comment 'login location',
    last_login_time datetime not null comment 'last login time',
    online_status   tinyint default 1000 not null comment '1000 online, 1100 offline',
    create_time     datetime default CURRENT_TIMESTAMP not null comment 'created time',
    update_time     datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment 'updated time',
    constraint uk_user_login_device
        unique (user_id, device_id)
) engine = InnoDB default charset = utf8mb4 comment = 'user login device table';

create table if not exists user_login_fail_log
(
    id          bigint unsigned not null comment 'primary key'
        primary key,
    username    varchar(128) null comment 'login username',
    user_id     bigint unsigned null comment 'user id',
    login_ip    varchar(64) null comment 'login ip',
    fail_reason varchar(255) null comment 'fail reason',
    create_time datetime default CURRENT_TIMESTAMP not null comment 'created time'
) engine = InnoDB default charset = utf8mb4 comment = 'user login fail log table';

insert into role (role_id, role_code, role_name, description)
values
    (1, 'admin', 'admin', 'can add, delete and query'),
    (2, 'user', 'user', 'can query')
on duplicate key update
    role_name = values(role_name),
    description = values(description);
