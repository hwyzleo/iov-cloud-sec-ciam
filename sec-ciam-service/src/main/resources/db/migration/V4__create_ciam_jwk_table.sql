-- 9. ciam_jwk — JWK 密钥表
-- 用于持久化存储 JWT 签名密钥对，支持密钥轮换和多密钥管理

CREATE TABLE IF NOT EXISTS `ciam_jwk` (
  `id`              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `key_id`          VARCHAR(64)   NOT NULL                COMMENT '密钥 ID(kid)',
  `private_key_pem` TEXT          NOT NULL                COMMENT '私钥 PEM 格式',
  `public_key_pem`  TEXT          NOT NULL                COMMENT '公钥 PEM 格式',
  `algorithm`       VARCHAR(32)   NOT NULL DEFAULT 'RSA'  COMMENT '签名算法',
  `key_size`        INT           NOT NULL DEFAULT 2048   COMMENT '密钥长度',
  `status`          TINYINT       NOT NULL DEFAULT 1      COMMENT '状态：1-激活，2-禁用，3-过期',
  `issue_time`      DATETIME(3)   NOT NULL                COMMENT '签发时间',
  `expire_time`     DATETIME(3)                            COMMENT '过期时间 (NULL=永不过期)',
  `is_primary`      TINYINT       NOT NULL DEFAULT 0      COMMENT '是否主密钥 (1-是，0-否，用于签名新 token)',
  `create_time`     DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `modify_time`     DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `row_version`     INT           NOT NULL DEFAULT 1,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_key_id` (`key_id`),
  KEY `idx_status_primary` (`status`, `is_primary`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='JWK 密钥表';

-- 插入初始主密钥（由应用启动时生成，此处预留）
