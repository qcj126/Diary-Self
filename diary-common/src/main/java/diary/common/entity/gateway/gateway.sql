create table if not exists gateway_route_config
(
    id           bigint unsigned not null comment 'primary key'
        primary key,
    route_id     varchar(128) not null comment 'route id',
    uri          varchar(255) not null comment 'route uri',
    predicates   varchar(1000) not null comment 'route predicates json',
    filters      varchar(1000) null comment 'route filters json',
    order_num    int default 0 not null comment 'route order',
    enabled      tinyint(1) default 1 not null comment '0 disabled, 1 enabled',
    create_time  datetime default CURRENT_TIMESTAMP not null comment 'created time',
    update_time  datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment 'updated time',
    constraint uk_gateway_route_id
        unique (route_id)
) engine = InnoDB default charset = utf8mb4 comment = 'gateway route config table';

create table if not exists gateway_rate_limit_rule
(
    id           bigint unsigned not null comment 'primary key'
        primary key,
    rule_key     varchar(128) not null comment 'rule key',
    limit_type   tinyint not null comment '1000 ip, 1100 user, 1200 uri',
    uri_pattern  varchar(255) null comment 'uri pattern',
    limit_count  int not null comment 'limit count',
    window_sec   int not null comment 'window seconds',
    enabled      tinyint(1) default 1 not null comment '0 disabled, 1 enabled',
    create_time  datetime default CURRENT_TIMESTAMP not null comment 'created time',
    update_time  datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment 'updated time',
    constraint uk_gateway_rate_limit_rule_key
        unique (rule_key)
) engine = InnoDB default charset = utf8mb4 comment = 'gateway rate limit rule table';
