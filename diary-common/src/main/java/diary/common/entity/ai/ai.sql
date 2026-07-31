create table ai_result(
                          id                      BIGINT primary key      NOT NULL COMMENT '主键',
                          user_id                 BIGINT UNSIGNED         NOT NULL COMMENT '用户ID',
                          model                   VARCHAR(20)             NOT NULL COMMENT 'AI模型',
                          temperature             VARCHAR(5)              NOT NULL COMMENT 'AI结果创意度',
                          ai_type                 tinyint(1)              NOT NULL COMMENT 'AI类别，对应着AI模型',
                          ai_application          tinyint(2)              NOT NULL COMMENT 'AI用途：如鉴别营养成分，每日推荐菜品等',
                          create_time             DATETIME                NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                          update_time             DATETIME                NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间'
) engine=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI结果表';

create table ai_nutrient(
                            id                      BIGINT primary key      NOT NULL  COMMENT '主键',
                            user_id                 BIGINT UNSIGNED         NOT NULL  COMMENT '用户ID',
                            ai_result_id            BIGINT UNSIGNED         NOT NULL  COMMENT 'ai结果表主键',
                            calory                  INT(5)                  default 0 COMMENT '热量 卡路里',
                            protein                 INT(3)                  default 0 COMMENT '蛋白质',
                            fat                     INT(3)                  default 0 COMMENT '脂肪',
                            carbohydrate            INT(3)                  default 0 COMMENT '碳水化合物',
                            sugar                   INT(3)                  default 0 COMMENT '糖分',
                            sodium                  INT(3)                  default 0 COMMENT '钠含量',
                            create_time             DATETIME                DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                            update_time             DATETIME                DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间'
) engine=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI饮食营养表';