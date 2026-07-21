-- auto-generated definition
create table user
(
    user_id     bigint                             not null comment 'user id'
        primary key,
    username    varchar(64)                        not null comment 'username',
    email       varchar(128)                       null comment 'email',
    phone       varchar(32)                        null comment 'phone',
    password    varchar(128)                       not null comment 'BCrypt password',
    status      int      default 1000              not null comment '1000 enabled, 0 disabled',
    create_time datetime default CURRENT_TIMESTAMP not null comment 'created time',
    update_time datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment 'updated time',
    constraint uk_user_email
        unique (email),
    constraint uk_user_phone
        unique (phone),
    constraint uk_user_username
        unique (username)
)
    comment 'user table' charset = utf8mb4;

-- auto-generated definition
create table role
(
    role_id     bigint                             not null comment 'role id'
        primary key,
    role_code   varchar(32)                        not null comment 'role code: admin/user',
    role_name   varchar(64)                        not null comment 'role name',
    description varchar(255)                       null comment 'description',
    status      int      default 1000              not null comment '1000 enabled, 0 disabled',
    create_time datetime default CURRENT_TIMESTAMP not null comment 'created time',
    update_time datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment 'updated time',
    constraint uk_role_code
        unique (role_code)
)
    comment 'role table' charset = utf8mb4;



-- auto-generated definition
create table user_role
(
    id          bigint                             not null comment 'id'
        primary key,
    user_id     bigint                             not null comment 'user id',
    role_id     bigint                             not null comment 'role id',
    create_time datetime default CURRENT_TIMESTAMP not null comment 'created time',
    constraint uk_user_role
        unique (user_id, role_id)
)
    comment 'user role relation table' charset = utf8mb4;

create index idx_user_role_role_id
    on user_role (role_id);



INSERT INTO `role` (`role_id`, `role_code`, `role_name`, `description`)
VALUES
  (1, 'admin', 'admin', 'can add, delete and query'),
  (2, 'user', 'user', 'can query')
ON DUPLICATE KEY UPDATE
  `role_name` = VALUES(`role_name`),
  `description` = VALUES(`description`);
