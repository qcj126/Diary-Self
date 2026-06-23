-- ============================================
-- 数据库迁移脚本：添加 object_key 字段
-- 说明：将原来存储签名URL的url字段改为存储OSS对象键
-- 执行时间：2026-06-23
-- ============================================

-- 1. 添加 object_key 字段
ALTER TABLE image 
ADD COLUMN object_key VARCHAR(512) NOT NULL DEFAULT '' COMMENT 'OSS对象键（文件路径，用于生成签名URL）' AFTER url;

-- 2. 将现有 url 字段的数据复制到 object_key（如果url存储的是完整URL，需要提取object_key）
-- 如果你的url字段存储的是完整OSS URL（如 https://bucket.oss-cn-chengdu.aliyuncs.com/diary/image/xxx.jpg）
-- 需要提取出 object_key 部分（diary/image/xxx.jpg）
UPDATE image 
SET object_key = SUBSTRING_INDEX(url, '/', -1) 
WHERE url LIKE '%/%/%/%' AND object_key = '';

-- 如果你的url字段本身就存储的是object_key（如 diary/image/xxx.jpg），则直接复制：
-- UPDATE image SET object_key = url WHERE object_key = '';

-- 3. 删除 url 字段的唯一索引
ALTER TABLE image DROP INDEX idx_images_url;

-- 4. 为 object_key 字段添加唯一索引
ALTER TABLE image ADD UNIQUE INDEX idx_images_object_key (object_key);

-- 5. 修改 url 字段注释
ALTER TABLE image 
MODIFY COLUMN url VARCHAR(512) NOT NULL COMMENT '图片访问URL（签名URL，前端展示用）';

-- ============================================
-- 验证脚本
-- ============================================
-- 查看表结构
DESC image;

-- 查看数据
SELECT id, url, object_key, original_name FROM image LIMIT 10;
