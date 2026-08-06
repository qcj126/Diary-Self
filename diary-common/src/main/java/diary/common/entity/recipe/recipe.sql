create table if not exists recipe
(
    id           bigint unsigned not null comment 'primary key'
        primary key,
    user_id      bigint unsigned not null comment 'creator user id',
    title        varchar(90) not null comment 'recipe title',
    image_id     bigint unsigned null comment 'cover image id',
    description  varchar(300) null comment 'description',
    category     tinyint not null comment 'recipe category',
    meal_type    tinyint not null comment '1 breakfast, 2 lunch, 3 dinner, 4 snack',
    difficulty   tinyint not null comment 'difficulty 1-5',
    cooking_time tinyint not null comment 'cooking minutes',
    story        varchar(300) null comment 'story or note',
    view_count   int unsigned default 0 not null comment 'view count',
    like_count   int unsigned default 0 not null comment 'like count',
    collect_count int unsigned default 0 not null comment 'collect count',
    sort         int unsigned default 0 not null comment 'sort value',
    deleted      tinyint(1) default 0 not null comment '0 normal, 1 deleted',
    create_time  datetime default CURRENT_TIMESTAMP not null comment 'created time',
    update_time  datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment 'updated time',
    constraint uk_recipe_title_user
        unique (user_id, title)
) engine = InnoDB default charset = utf8mb4 comment = 'recipe table';

create index idx_recipe_category
    on recipe (category, meal_type, deleted);

create table if not exists recipe_ingredient
(
    id          bigint unsigned not null comment 'primary key'
        primary key,
    recipe_id   bigint unsigned not null comment 'recipe id',
    user_id     bigint unsigned not null comment 'creator user id',
    name        varchar(255) not null comment 'ingredient name',
    quantity    varchar(255) not null comment 'ingredient quantity',
    is_main     tinyint not null comment '0 no, 1 yes',
    sort        int unsigned default 0 not null comment 'sort value',
    deleted     tinyint(1) default 0 not null comment '0 normal, 1 deleted',
    create_time datetime default CURRENT_TIMESTAMP not null comment 'created time',
    update_time datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment 'updated time'
) engine = InnoDB default charset = utf8mb4 comment = 'recipe ingredient table';

create index idx_recipe_ingredient_recipe_id
    on recipe_ingredient (recipe_id, deleted);

create table if not exists recipe_step
(
    id           bigint unsigned not null comment 'primary key'
        primary key,
    recipe_id    bigint unsigned not null comment 'recipe id',
    user_id      bigint unsigned not null comment 'creator user id',
    step_number  tinyint not null comment 'step number',
    description  varchar(300) not null comment 'step description',
    timer_minute tinyint default 0 not null comment 'timer minutes',
    image_id      bigint unsigned null comment 'step image id',
    sort         int unsigned default 0 not null comment 'sort value',
    deleted      tinyint(1) default 0 not null comment '0 normal, 1 deleted',
    create_time  datetime default CURRENT_TIMESTAMP not null comment 'created time',
    update_time  datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment 'updated time'
) engine = InnoDB default charset = utf8mb4 comment = 'recipe step table';

create index idx_recipe_step_recipe_id
    on recipe_step (recipe_id, deleted);

create table recipe_category(
                                id                        BIGINT UNSIGNED         NOT NULL COMMENT '主键',
                                user_id                   BIGINT UNSIGNED         NOT NULL COMMENT '创建者用户ID',
                                category_name             TINYINT UNIQUE          NOT NULL COMMENT '分类名称',
                                category_num              TINYINT UNIQUE          NOT NULL COMMENT '分类编号',
                                category_icon             varchar(255)            not null comment '分类图标',
                                sort                      INT UNSIGNED            NOT NULL DEFAULT 0 COMMENT '排序',
                                create_time               DATETIME                NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                update_time               DATETIME                NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
                                PRIMARY KEY (id)
) ENGINE=INNODB DEFAULT CHARSET=utf8mb4 COMMENT='食谱步骤表';