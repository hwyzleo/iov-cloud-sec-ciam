-- V7__add_ciam_oauth_client_iphone_capp.sql
-- 手机端（iPhone）验证码登录客户端：iphone-capp
--
-- 背景（SEC-CIAM-DSN-CR-003 §5.1，RD-003-4）：
--   手机验证码登录对客户端做“双重校验”：
--     1) 配置允许清单 ciam.authentication.mobile.allowed-client-ids（Nacos / CIAM_MOBILE_CLIENT_IDS）
--     2) 数据库 ciam_oauth_client 中存在 enabled + public 的客户端记录
--   只配允许清单而库中无记录时，登录在第二道校验抛 701306「客户端不存在」。
--   本迁移补齐第 2 项；允许清单仍由配置侧维护，不在迁移中硬编码。
--
-- 约束：
--   1) 不修改已应用的 V1～V6（Flyway 历史不可变）；
--   2) 幂等：重复执行/已存在记录时不报错，并把手机登录依赖的字段修回合法值
--      （row_valid=1、client_status=1、client_type=public、scopes、TTL）；
--      uk_client_id 仅含 client_id，因此逻辑删除（row_valid=0）的旧行会走 UPDATE 修复；
--   3) 不覆盖既有记录的展示类字段（client_name/redirect_uris/description），
--      避免迁移覆盖运营已维护的回调地址等配置；
--   4) 公开客户端（public）无密钥，client_secret_hash 保持 NULL；
--   5) 应用回滚时保留本行（旧版本读取时按 enabled/public 判定，不影响既有客户端），
--      如需清理请手工 DELETE。

INSERT INTO `ciam_oauth_client` (
  `client_id`, `client_name`, `client_secret_hash`, `client_type`,
  `redirect_uris`, `grant_types`, `scopes`,
  `pkce_required`, `access_token_ttl`, `refresh_token_ttl`,
  `client_status`, `description`, `create_by`
) VALUES (
  'iphone-capp', 'iPhone 手机 App', NULL, 'public',
  '["openiov://oauth/callback"]',
  'authorization_code,refresh_token', 'openid profile phone email',
  1, 1800, 2592000,
  1, '手机端验证码登录客户端（公开客户端）', 'system'
) ON DUPLICATE KEY UPDATE
  `row_valid` = 1,
  `client_type` = 'public',
  `client_status` = 1,
  `scopes` = VALUES(`scopes`),
  `access_token_ttl` = VALUES(`access_token_ttl`),
  `refresh_token_ttl` = VALUES(`refresh_token_ttl`),
  `modify_time` = CURRENT_TIMESTAMP;
