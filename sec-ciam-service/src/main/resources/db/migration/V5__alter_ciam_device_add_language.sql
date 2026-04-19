-- V5__alter_ciam_device_add_language.sql
-- 在 ciam_device 表添加 language 字段，用于记录用户当前使用的语言

ALTER TABLE `ciam_device` 
ADD COLUMN `language` VARCHAR(16) DEFAULT NULL COMMENT '当前语言：如 zh-CN, en-US';
