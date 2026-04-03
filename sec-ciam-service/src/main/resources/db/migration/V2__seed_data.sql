-- ============================================================================
-- V2: CIAM 开发/测试环境种子数据
-- 仅用于 dev/test 环境初始化，生产环境通过运营后台或独立脚本管理
-- ============================================================================

-- ----------------------------
-- 1. 测试 OAuth Client — 手机 App（公开客户端，Authorization Code + PKCE）
-- ----------------------------
INSERT INTO `ciam_oauth_client` (
  `client_id`, `client_name`, `client_secret_hash`, `client_type`,
  `redirect_uris`, `grant_types`, `scopes`,
  `pkce_required`, `access_token_ttl`, `refresh_token_ttl`,
  `client_status`, `description`, `create_by`
) VALUES (
  'ciam-mobile-app', 'CIAM 手机 App', NULL, 'public',
  '["https://localhost/callback","openiov://oauth/callback"]',
  'authorization_code,refresh_token', 'openid profile phone email',
  1, 1800, 2592000,
  1, '开发环境测试用手机 App 客户端', 'system'
) ON DUPLICATE KEY UPDATE `modify_time` = CURRENT_TIMESTAMP;

-- ----------------------------
-- 2. 测试 OAuth Client — 官网（公开客户端，Authorization Code + PKCE）
-- ----------------------------
INSERT INTO `ciam_oauth_client` (
  `client_id`, `client_name`, `client_secret_hash`, `client_type`,
  `redirect_uris`, `grant_types`, `scopes`,
  `pkce_required`, `access_token_ttl`, `refresh_token_ttl`,
  `client_status`, `description`, `create_by`
) VALUES (
  'ciam-web-portal', 'CIAM 官网', NULL, 'public',
  '["http://localhost:3000/callback","https://localhost:3000/callback"]',
  'authorization_code,refresh_token', 'openid profile email',
  1, 1800, 2592000,
  1, '开发环境测试用官网客户端', 'system'
) ON DUPLICATE KEY UPDATE `modify_time` = CURRENT_TIMESTAMP;

-- ----------------------------
-- 3. 测试 OAuth Client — 内部服务（机密客户端，Client Credentials）
-- BCrypt hash of 'test-secret-123' with strength 4: $2a$04$YKCmMHzJMCxHvsLSiaVVouDn5XY/JniMKOJl1lfqEOFp5a.BW3Kqm
-- ----------------------------
INSERT INTO `ciam_oauth_client` (
  `client_id`, `client_name`, `client_secret_hash`, `client_type`,
  `redirect_uris`, `grant_types`, `scopes`,
  `pkce_required`, `access_token_ttl`, `refresh_token_ttl`,
  `client_status`, `description`, `create_by`
) VALUES (
  'ciam-internal-service', 'CIAM 内部服务', '$2a$04$YKCmMHzJMCxHvsLSiaVVouDn5XY/JniMKOJl1lfqEOFp5a.BW3Kqm', 'confidential',
  NULL, 'client_credentials', 'internal:read internal:write',
  0, 3600, 0,
  1, '开发环境测试用内部服务客户端（密钥: test-secret-123）', 'system'
) ON DUPLICATE KEY UPDATE `modify_time` = CURRENT_TIMESTAMP;

-- ----------------------------
-- 4. 测试 OAuth Client — 车机（公开客户端，Device Authorization Grant）
-- ----------------------------
INSERT INTO `ciam_oauth_client` (
  `client_id`, `client_name`, `client_secret_hash`, `client_type`,
  `redirect_uris`, `grant_types`, `scopes`,
  `pkce_required`, `access_token_ttl`, `refresh_token_ttl`,
  `client_status`, `description`, `create_by`
) VALUES (
  'ciam-vehicle', 'CIAM 车机', NULL, 'public',
  NULL, 'urn:ietf:params:oauth:grant-type:device_code,refresh_token', 'openid profile',
  0, 1800, 7776000,
  1, '开发环境测试用车机客户端', 'system'
) ON DUPLICATE KEY UPDATE `modify_time` = CURRENT_TIMESTAMP;

-- ----------------------------
-- 5. 测试 OAuth Client — 小程序（公开客户端，Authorization Code + PKCE）
-- ----------------------------
INSERT INTO `ciam_oauth_client` (
  `client_id`, `client_name`, `client_secret_hash`, `client_type`,
  `redirect_uris`, `grant_types`, `scopes`,
  `pkce_required`, `access_token_ttl`, `refresh_token_ttl`,
  `client_status`, `description`, `create_by`
) VALUES (
  'ciam-mini-program', 'CIAM 小程序', NULL, 'public',
  '["https://localhost/mp/callback"]',
  'authorization_code,refresh_token', 'openid profile phone',
  1, 1800, 2592000,
  1, '开发环境测试用小程序客户端', 'system'
) ON DUPLICATE KEY UPDATE `modify_time` = CURRENT_TIMESTAMP;
