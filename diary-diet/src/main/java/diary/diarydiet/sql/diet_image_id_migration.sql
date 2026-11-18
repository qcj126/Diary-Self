-- 人工选择并执行：为饮食卡片关联缩略图查询所需的图片ID。
-- DEFAULT 0 使现有记录兼容；旧数据不回填，前端在 image_id=0 时回退原图。
ALTER TABLE diet_record
    ADD COLUMN image_id BIGINT UNSIGNED NOT NULL DEFAULT 0
        COMMENT '关联image.id，0表示未关联'
        AFTER image_url;
