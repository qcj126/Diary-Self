CREATE TABLE diet_record
(
    id          BIGINT UNSIGNED                                      NOT NULL COMMENT '雪花主键',
    user_id     BIGINT UNSIGNED                                      NOT NULL COMMENT '用户ID',
    eat_time    DATETIME                                             NOT NULL COMMENT '食用时间',
    meal_type   TINYINT                                              NOT NULL COMMENT '10早餐 15早加餐 20午餐 25午加餐 30晚餐 35夜宵',
    food_name   VARCHAR(200)                                         NOT NULL COMMENT '食物/菜品名称',
    location    VARCHAR(100)            DEFAULT 'home'               NOT NULL COMMENT '用餐地点，前端使用home/outside',
    note        VARCHAR(500)                                         NULL COMMENT '备注',
    image_url   VARCHAR(1000)                                        NULL COMMENT '食物图片地址',
    deleted     TINYINT(1)              DEFAULT 0                    NOT NULL COMMENT '逻辑删除：0正常 1删除',
    create_time DATETIME(3)             DEFAULT CURRENT_TIMESTAMP(3) NOT NULL COMMENT '创建时间',
    update_time DATETIME(3)             DEFAULT CURRENT_TIMESTAMP(3) NOT NULL ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    INDEX idx_diet_user_eat_time (user_id, eat_time, deleted),
    INDEX idx_diet_user_location_time (user_id, location, eat_time),
    INDEX idx_diet_user_meal_time (user_id, meal_type, eat_time)
)
    COMMENT '饮食记录表'
    CHARACTER SET utf8mb4;
