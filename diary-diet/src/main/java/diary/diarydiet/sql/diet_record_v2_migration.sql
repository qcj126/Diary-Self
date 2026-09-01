-- 仅已有旧版饮食表时执行；全新环境直接执行 diet.sql。
DROP PROCEDURE IF EXISTS `migrate_diet_record_v2`;
DELIMITER //
CREATE PROCEDURE `migrate_diet_record_v2`()
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = DATABASE() AND table_name = 'diet_records'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = DATABASE() AND table_name = 'diet_record'
    ) THEN
        RENAME TABLE `diet_records` TO `diet_record`;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'diet_record' AND column_name = 'create_time'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'diet_record' AND column_name = 'created_at'
    ) THEN
        ALTER TABLE `diet_record` CHANGE COLUMN `create_time` `created_at`
            DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3);
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'diet_record' AND column_name = 'update_time'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'diet_record' AND column_name = 'updated_at'
    ) THEN
        ALTER TABLE `diet_record` CHANGE COLUMN `update_time` `updated_at`
            DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'diet_record' AND column_name = 'sugar'
    ) THEN
        ALTER TABLE `diet_record` ADD COLUMN `sugar`
            DECIMAL(10,2) UNSIGNED NOT NULL DEFAULT 0.00 COMMENT '糖(g)' AFTER `carbohydrate`;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'diet_record' AND column_name = 'sodium'
    ) THEN
        ALTER TABLE `diet_record` ADD COLUMN `sodium`
            DECIMAL(10,2) UNSIGNED NOT NULL DEFAULT 0.00 COMMENT '钠(mg)' AFTER `sugar`;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'diet_record' AND column_name = 'image_url'
    ) THEN
        ALTER TABLE `diet_record` ADD COLUMN `image_url`
            VARCHAR(1000) NULL COMMENT '食物图片地址' AFTER `note`;
    END IF;
END//
DELIMITER ;

CALL `migrate_diet_record_v2`();
DROP PROCEDURE `migrate_diet_record_v2`;
