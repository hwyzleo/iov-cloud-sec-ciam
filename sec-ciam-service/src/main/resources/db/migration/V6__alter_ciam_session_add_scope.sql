-- V6__alter_ciam_session_add_scope.sql
-- SEC-CIAM-DSN-CR-003 T-003-1（RD-003-2，§4.2）
-- 会话冻结授权范围：登录时按客户端配置规范化后写入，刷新链路只从会话恢复，不再读取当前客户端配置。
-- 约束：
--   1) 不修改已应用的 V1～V5（Flyway 历史不可变）；
--   2) 历史记录保持 NULL，不按当前客户端配置回填；
--   3) 新手机会话 scope 必须非空（由应用层保证）；
--   4) 不新增索引，不改变既有主键、会话状态和时间字段；
--   5) 应用回滚时保留本列（旧版本模型忽略未知列），不在常规回滚中 DROP COLUMN。

ALTER TABLE `ciam_session`
    ADD COLUMN `scope` VARCHAR(1024) DEFAULT NULL
    COMMENT '登录时冻结的授权范围，空格分隔'
    AFTER `client_type`;
