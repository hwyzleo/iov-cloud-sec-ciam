-- V3__alter_ciam_device_column.sql
-- 修改 ciam_device 表：将 device_type 字段改为 client_id

ALTER TABLE `ciam_device` 
CHANGE COLUMN `device_type` `client_id` VARCHAR(64) DEFAULT NULL COMMENT '设备 ID';
