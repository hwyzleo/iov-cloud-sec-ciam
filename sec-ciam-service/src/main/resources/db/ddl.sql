-- ============================================================================
-- CIAM 用户身份及访问管理系统 — 核心业务表 DDL
-- 数据库引擎: InnoDB | 字符集: utf8mb4 | 排序规则: utf8mb4_general_ci
-- 生成依据: database-design.md 字段级详细设计
-- ============================================================================

-- ============================================================================
-- 一、用户核心表组
-- 包含: ciam_user, ciam_user_identity, ciam_user_credential,
--       ciam_user_profile, ciam_user_tag
-- ============================================================================

-- ----------------------------
-- 1. ciam_user — 用户主表
-- ----------------------------
DROP TABLE IF EXISTS `ciam_user`;
CREATE TABLE `ciam_user` (
  `id`                    BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`               VARCHAR(64)   NOT NULL                COMMENT '用户业务唯一标识',
  `user_status`           TINYINT       NOT NULL DEFAULT 0      COMMENT '用户状态：0-待验证，1-正常，2-已锁定，3-已禁用，4-注销处理中，5-已注销',
  `brand_code`            VARCHAR(32)   NOT NULL DEFAULT 'OPENIOV' COMMENT '品牌编码',
  `register_source`       VARCHAR(32)   DEFAULT NULL            COMMENT '注册来源：mobile,email,wechat,apple,google,local_mobile',
  `register_channel`      VARCHAR(64)   DEFAULT NULL            COMMENT '注册渠道',
  `primary_identity_type` VARCHAR(32)   DEFAULT NULL            COMMENT '主登录标识类型',
  `last_login_time`       TIMESTAMP     NULL DEFAULT NULL       COMMENT '最后登录时间',
  `deactivated_time`      TIMESTAMP     NULL DEFAULT NULL       COMMENT '注销完成时间',
  `description`           VARCHAR(255)  DEFAULT NULL            COMMENT '备注',
  `create_time`           TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_by`             VARCHAR(64)   DEFAULT NULL            COMMENT '创建者',
  `modify_time`           TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  `modify_by`             VARCHAR(64)   DEFAULT NULL            COMMENT '修改者',
  `row_version`           INT           DEFAULT 1               COMMENT '记录版本',
  `row_valid`             TINYINT       DEFAULT 1               COMMENT '记录是否有效：1-有效，0-无效',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`),
  KEY `idx_user_status` (`user_status`),
  KEY `idx_last_login_time` (`last_login_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户主表';

-- ----------------------------
-- 2. ciam_user_identity — 登录标识表
-- ----------------------------
DROP TABLE IF EXISTS `ciam_user_identity`;
CREATE TABLE `ciam_user_identity` (
  `id`               BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `identity_id`      VARCHAR(64)   NOT NULL                COMMENT '登录标识业务唯一标识',
  `user_id`          VARCHAR(64)   NOT NULL                COMMENT '用户业务唯一标识',
  `identity_type`    VARCHAR(32)   NOT NULL                COMMENT '标识类型：mobile,email,wechat,apple,google,local_mobile',
  `identity_value`   VARCHAR(255)  NOT NULL                COMMENT '登录标识原值（AES-256-GCM 加密存储，FieldEncryptor.encrypt）',
  `identity_hash`    CHAR(64)      NOT NULL                COMMENT '登录标识 SHA-256 哈希值（FieldEncryptor.hash，用于唯一查重）',
  `country_code`     VARCHAR(8)    DEFAULT NULL            COMMENT '国家区号（手机号场景使用）',
  `verified_flag`    TINYINT       NOT NULL DEFAULT 0      COMMENT '是否已验证：0-否，1-是',
  `primary_flag`     TINYINT       NOT NULL DEFAULT 0      COMMENT '是否主标识：0-否，1-是',
  `bind_source`      VARCHAR(32)   DEFAULT NULL            COMMENT '绑定来源',
  `bind_time`        TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '绑定时间',
  `unbind_time`      TIMESTAMP     NULL DEFAULT NULL       COMMENT '解绑时间',
  `identity_status`  TINYINT       NOT NULL DEFAULT 1      COMMENT '标识状态：1-已绑定，0-已解绑',
  `description`      VARCHAR(255)  DEFAULT NULL            COMMENT '备注',
  `create_time`      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_by`        VARCHAR(64)   DEFAULT NULL            COMMENT '创建者',
  `modify_time`      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  `modify_by`        VARCHAR(64)   DEFAULT NULL            COMMENT '修改者',
  `row_version`      INT           DEFAULT 1               COMMENT '记录版本',
  `row_valid`        TINYINT       DEFAULT 1               COMMENT '记录是否有效：1-有效，0-无效',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_identity_id` (`identity_id`),
  UNIQUE KEY `uk_identity_type_hash_valid` (`identity_type`, `identity_hash`, `row_valid`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_identity_type_status` (`identity_type`, `identity_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='登录标识表';

-- ----------------------------
-- 3. ciam_user_credential — 凭据表
-- ----------------------------
DROP TABLE IF EXISTS `ciam_user_credential`;
CREATE TABLE `ciam_user_credential` (
  `id`                BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `credential_id`     VARCHAR(64)   NOT NULL                COMMENT '凭据业务唯一标识',
  `user_id`           VARCHAR(64)   NOT NULL                COMMENT '用户业务唯一标识',
  `credential_type`   VARCHAR(32)   NOT NULL                COMMENT '凭据类型：email_password',
  `credential_hash`   VARCHAR(255)  NOT NULL                COMMENT '凭据 BCrypt 哈希值（PasswordEncoder.encode，不可逆）',
  `salt`              VARCHAR(128)  DEFAULT NULL            COMMENT '盐值（BCrypt 内含盐，此列保留备用）',
  `hash_algorithm`    VARCHAR(32)   NOT NULL                COMMENT '哈希算法（当前固定 BCRYPT）',
  `password_set_time` TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '密码设置时间',
  `last_verify_time`  TIMESTAMP     NULL DEFAULT NULL       COMMENT '最后校验成功时间',
  `fail_count`        INT           NOT NULL DEFAULT 0      COMMENT '连续失败次数',
  `locked_until`      TIMESTAMP     NULL DEFAULT NULL       COMMENT '锁定截止时间',
  `credential_status` TINYINT       NOT NULL DEFAULT 1      COMMENT '凭据状态：1-有效，0-失效',
  `description`       VARCHAR(255)  DEFAULT NULL            COMMENT '备注',
  `create_time`       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_by`         VARCHAR(64)   DEFAULT NULL            COMMENT '创建者',
  `modify_time`       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  `modify_by`         VARCHAR(64)   DEFAULT NULL            COMMENT '修改者',
  `row_version`       INT           DEFAULT 1               COMMENT '记录版本',
  `row_valid`         TINYINT       DEFAULT 1               COMMENT '记录是否有效：1-有效，0-无效',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_credential_id` (`credential_id`),
  UNIQUE KEY `uk_user_credential_type_valid` (`user_id`, `credential_type`, `row_valid`),
  KEY `idx_locked_until` (`locked_until`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='凭据表';

-- ----------------------------
-- 4. ciam_user_profile — 用户资料扩展表
-- ----------------------------
DROP TABLE IF EXISTS `ciam_user_profile`;
CREATE TABLE `ciam_user_profile` (
  `id`            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `profile_id`    VARCHAR(64)   NOT NULL                COMMENT '资料业务唯一标识',
  `user_id`       VARCHAR(64)   NOT NULL                COMMENT '用户业务唯一标识',
  `nickname`      VARCHAR(64)   DEFAULT NULL            COMMENT '昵称',
  `avatar_url`    VARCHAR(255)  DEFAULT NULL            COMMENT '头像地址',
  `real_name`     VARCHAR(64)   DEFAULT NULL            COMMENT '姓名',
  `gender`        TINYINT       NOT NULL DEFAULT 0      COMMENT '性别：0-未知，1-男，2-女',
  `birthday`      DATE          DEFAULT NULL            COMMENT '生日',
  `region_code`   VARCHAR(32)   DEFAULT NULL            COMMENT '地区编码',
  `region_name`   VARCHAR(128)  DEFAULT NULL            COMMENT '地区名称',
  `description`   VARCHAR(255)  DEFAULT NULL            COMMENT '备注',
  `create_time`   TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_by`     VARCHAR(64)   DEFAULT NULL            COMMENT '创建者',
  `modify_time`   TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  `modify_by`     VARCHAR(64)   DEFAULT NULL            COMMENT '修改者',
  `row_version`   INT           DEFAULT 1               COMMENT '记录版本',
  `row_valid`     TINYINT       DEFAULT 1               COMMENT '记录是否有效：1-有效，0-无效',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_profile_id` (`profile_id`),
  UNIQUE KEY `uk_user_profile_valid` (`user_id`, `row_valid`),
  KEY `idx_region_code` (`region_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户资料扩展表';

-- ----------------------------
-- 5. ciam_user_tag — 认证标签表
-- ----------------------------
DROP TABLE IF EXISTS `ciam_user_tag`;
CREATE TABLE `ciam_user_tag` (
  `id`              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tag_id`          VARCHAR(64)   NOT NULL                COMMENT '标签业务唯一标识',
  `user_id`         VARCHAR(64)   NOT NULL                COMMENT '用户业务唯一标识',
  `tag_code`        VARCHAR(32)   NOT NULL                COMMENT '标签编码：real_name_verified,owner_verified',
  `tag_name`        VARCHAR(64)   NOT NULL                COMMENT '标签名称',
  `tag_status`      TINYINT       NOT NULL DEFAULT 1      COMMENT '标签状态：1-生效，0-失效',
  `tag_source`      VARCHAR(32)   DEFAULT NULL            COMMENT '标签来源',
  `effective_time`  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '生效时间',
  `expire_time`     TIMESTAMP     NULL DEFAULT NULL       COMMENT '失效时间',
  `description`     VARCHAR(255)  DEFAULT NULL            COMMENT '备注',
  `create_time`     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_by`       VARCHAR(64)   DEFAULT NULL            COMMENT '创建者',
  `modify_time`     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  `modify_by`       VARCHAR(64)   DEFAULT NULL            COMMENT '修改者',
  `row_version`     INT           DEFAULT 1               COMMENT '记录版本',
  `row_valid`       TINYINT       DEFAULT 1               COMMENT '记录是否有效：1-有效，0-无效',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tag_id` (`tag_id`),
  UNIQUE KEY `uk_user_tag_code_valid` (`user_id`, `tag_code`, `row_valid`),
  KEY `idx_tag_code_status` (`tag_code`, `tag_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='认证标签表';

-- ============================================================================
-- 二、会话与令牌表组
-- 包含: ciam_session, ciam_device, ciam_refresh_token
-- ============================================================================

-- ----------------------------
-- 6. ciam_session — 会话表
-- ----------------------------
DROP TABLE IF EXISTS `ciam_session`;
CREATE TABLE `ciam_session` (
  `id`               BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `session_id`       VARCHAR(64)   NOT NULL                COMMENT '会话业务唯一标识',
  `user_id`          VARCHAR(64)   NOT NULL                COMMENT '用户业务唯一标识',
  `device_id`        VARCHAR(64)   DEFAULT NULL            COMMENT '设备业务唯一标识',
  `client_id`        VARCHAR(64)   DEFAULT NULL            COMMENT 'OAuth客户端标识',
  `client_type`      VARCHAR(32)   NOT NULL                COMMENT '客户端类型：app,mini_program,web,vehicle,admin',
  `login_ip`         VARCHAR(64)   DEFAULT NULL            COMMENT '登录IP',
  `login_region`     VARCHAR(128)  DEFAULT NULL            COMMENT '登录地区',
  `risk_level`       TINYINT       NOT NULL DEFAULT 0      COMMENT '风险等级：0-低，1-中，2-高',
  `session_status`   TINYINT       NOT NULL DEFAULT 1      COMMENT '会话状态：1-有效，0-失效，2-下线，3-过期',
  `login_time`       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
  `last_active_time` TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最后活跃时间',
  `logout_time`      TIMESTAMP     NULL DEFAULT NULL       COMMENT '退出时间',
  `expire_time`      TIMESTAMP     NOT NULL                COMMENT '过期时间',
  `description`      VARCHAR(255)  DEFAULT NULL            COMMENT '备注',
  `create_time`      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_by`        VARCHAR(64)   DEFAULT NULL            COMMENT '创建者',
  `modify_time`      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  `modify_by`        VARCHAR(64)   DEFAULT NULL            COMMENT '修改者',
  `row_version`      INT           DEFAULT 1               COMMENT '记录版本',
  `row_valid`        TINYINT       DEFAULT 1               COMMENT '记录是否有效：1-有效，0-无效',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_session_id` (`session_id`),
  KEY `idx_user_session_status` (`user_id`, `session_status`),
  KEY `idx_device_id` (`device_id`),
  KEY `idx_expire_time` (`expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='会话表';

-- ----------------------------
-- 7. ciam_device — 设备表
-- ----------------------------
DROP TABLE IF EXISTS `ciam_device`;
CREATE TABLE `ciam_device` (
  `id`                 BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `device_id`          VARCHAR(64)   NOT NULL                COMMENT '设备业务唯一标识',
  `user_id`            VARCHAR(64)   NOT NULL                COMMENT '用户业务唯一标识',
  `client_type`        VARCHAR(32)   NOT NULL                COMMENT '客户端类型',
  `device_type`        VARCHAR(32)   DEFAULT NULL            COMMENT '设备类型',
  `device_name`        VARCHAR(64)   DEFAULT NULL            COMMENT '设备名称',
  `device_os`          VARCHAR(64)   DEFAULT NULL            COMMENT '设备操作系统',
  `app_version`        VARCHAR(32)   DEFAULT NULL            COMMENT '应用版本',
  `device_fingerprint` VARCHAR(128)  DEFAULT NULL            COMMENT '设备指纹',
  `trusted_flag`       TINYINT       NOT NULL DEFAULT 0      COMMENT '是否受信任设备：0-否，1-是',
  `first_login_time`   TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '首次登录时间',
  `last_login_time`    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最后登录时间',
  `device_status`      TINYINT       NOT NULL DEFAULT 1      COMMENT '设备状态：1-正常，0-失效',
  `description`        VARCHAR(255)  DEFAULT NULL            COMMENT '备注',
  `create_time`        TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_by`          VARCHAR(64)   DEFAULT NULL            COMMENT '创建者',
  `modify_time`        TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  `modify_by`          VARCHAR(64)   DEFAULT NULL            COMMENT '修改者',
  `row_version`        INT           DEFAULT 1               COMMENT '记录版本',
  `row_valid`          TINYINT       DEFAULT 1               COMMENT '记录是否有效：1-有效，0-无效',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_device_id` (`device_id`),
  KEY `idx_user_device_status` (`user_id`, `device_status`),
  KEY `idx_device_fingerprint` (`device_fingerprint`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='设备表';

-- ----------------------------
-- 8. ciam_refresh_token — 刷新令牌表
-- ----------------------------
DROP TABLE IF EXISTS `ciam_refresh_token`;
CREATE TABLE `ciam_refresh_token` (
  `id`                BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `refresh_token_id`  VARCHAR(64)   NOT NULL                COMMENT '刷新令牌业务唯一标识',
  `user_id`           VARCHAR(64)   NOT NULL                COMMENT '用户业务唯一标识',
  `session_id`        VARCHAR(64)   NOT NULL                COMMENT '会话业务唯一标识',
  `client_id`         VARCHAR(64)   DEFAULT NULL            COMMENT '客户端标识',
  `token_fingerprint` CHAR(64)      NOT NULL                COMMENT '刷新令牌 SHA-256 指纹（TokenDigest.fingerprint，原值不落库）',
  `parent_token_id`   VARCHAR(64)   DEFAULT NULL            COMMENT '上一个刷新令牌业务标识',
  `token_status`      TINYINT       NOT NULL DEFAULT 1      COMMENT '令牌状态：1-有效，2-已轮换，3-已撤销，4-已过期',
  `issue_time`        TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '签发时间',
  `used_time`         TIMESTAMP     NULL DEFAULT NULL       COMMENT '使用时间',
  `revoke_time`       TIMESTAMP     NULL DEFAULT NULL       COMMENT '撤销时间',
  `expire_time`       TIMESTAMP     NOT NULL                COMMENT '过期时间',
  `description`       VARCHAR(255)  DEFAULT NULL            COMMENT '备注',
  `create_time`       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_by`         VARCHAR(64)   DEFAULT NULL            COMMENT '创建者',
  `modify_time`       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  `modify_by`         VARCHAR(64)   DEFAULT NULL            COMMENT '修改者',
  `row_version`       INT           DEFAULT 1               COMMENT '记录版本',
  `row_valid`         TINYINT       DEFAULT 1               COMMENT '记录是否有效：1-有效，0-无效',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_refresh_token_id` (`refresh_token_id`),
  UNIQUE KEY `uk_token_fingerprint` (`token_fingerprint`),
  KEY `idx_user_token_status` (`user_id`, `token_status`),
  KEY `idx_session_status` (`session_id`, `token_status`),
  KEY `idx_expire_time` (`expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='刷新令牌表';

-- ============================================================================
-- 三、OAuth 协议表组
-- 包含: ciam_oauth_client, ciam_auth_code
-- ============================================================================

-- ----------------------------
-- 9. ciam_oauth_client — 接入应用表
-- ----------------------------
DROP TABLE IF EXISTS `ciam_oauth_client`;
CREATE TABLE `ciam_oauth_client` (
  `id`                 BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `client_id`          VARCHAR(64)   NOT NULL                COMMENT '客户端标识',
  `client_name`        VARCHAR(128)  NOT NULL                COMMENT '客户端名称',
  `client_secret_hash` VARCHAR(255)  DEFAULT NULL            COMMENT '客户端密钥 BCrypt 哈希（PasswordEncoder.encode，公开客户端可为空）',
  `client_type`        VARCHAR(32)   NOT NULL                COMMENT '客户端类型：public,confidential,internal',
  `redirect_uris`      TEXT          DEFAULT NULL            COMMENT '回调地址列表（JSON 数组序列化，LongTextSerializer.toJsonArray）',
  `grant_types`        VARCHAR(255)  NOT NULL                COMMENT '授权类型列表（逗号分隔，LongTextSerializer.toCommaSeparated）',
  `scopes`             VARCHAR(255)  DEFAULT NULL            COMMENT '授权范围列表（空格分隔，LongTextSerializer.toSpaceSeparated）',
  `pkce_required`      TINYINT       NOT NULL DEFAULT 1      COMMENT '是否强制PKCE：0-否，1-是',
  `access_token_ttl`   INT           NOT NULL DEFAULT 1800   COMMENT '访问令牌有效期（秒）',
  `refresh_token_ttl`  INT           NOT NULL DEFAULT 2592000 COMMENT '刷新令牌有效期（秒）',
  `client_status`      TINYINT       NOT NULL DEFAULT 1      COMMENT '客户端状态：1-启用，0-停用',
  `description`        VARCHAR(255)  DEFAULT NULL            COMMENT '备注',
  `create_time`        TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_by`          VARCHAR(64)   DEFAULT NULL            COMMENT '创建者',
  `modify_time`        TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  `modify_by`          VARCHAR(64)   DEFAULT NULL            COMMENT '修改者',
  `row_version`        INT           DEFAULT 1               COMMENT '记录版本',
  `row_valid`          TINYINT       DEFAULT 1               COMMENT '记录是否有效：1-有效，0-无效',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_client_id` (`client_id`),
  KEY `idx_client_status` (`client_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='接入应用表';

-- ----------------------------
-- 10. ciam_auth_code — 授权码记录表
-- ----------------------------
DROP TABLE IF EXISTS `ciam_auth_code`;
CREATE TABLE `ciam_auth_code` (
  `id`               BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `auth_code_id`     VARCHAR(64)   NOT NULL                COMMENT '授权码业务唯一标识',
  `client_id`        VARCHAR(64)   NOT NULL                COMMENT '客户端标识',
  `user_id`          VARCHAR(64)   NOT NULL                COMMENT '用户业务唯一标识',
  `session_id`       VARCHAR(64)   DEFAULT NULL            COMMENT '会话业务唯一标识',
  `code_hash`        CHAR(64)      NOT NULL                COMMENT '授权码 SHA-256 哈希值（TokenDigest.fingerprint，原值不落库）',
  `redirect_uri`     VARCHAR(255)  NOT NULL                COMMENT '回调地址',
  `scope`            VARCHAR(255)  DEFAULT NULL            COMMENT '授权范围',
  `code_challenge`   VARCHAR(128)  DEFAULT NULL            COMMENT 'PKCE challenge',
  `challenge_method` VARCHAR(16)   DEFAULT NULL            COMMENT 'PKCE challenge method',
  `expire_time`      TIMESTAMP     NOT NULL                COMMENT '过期时间',
  `used_flag`        TINYINT       NOT NULL DEFAULT 0      COMMENT '是否已使用：0-否，1-是',
  `used_time`        TIMESTAMP     NULL DEFAULT NULL       COMMENT '使用时间',
  `description`      VARCHAR(255)  DEFAULT NULL            COMMENT '备注',
  `create_time`      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_by`        VARCHAR(64)   DEFAULT NULL            COMMENT '创建者',
  `modify_time`      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  `modify_by`        VARCHAR(64)   DEFAULT NULL            COMMENT '修改者',
  `row_version`      INT           DEFAULT 1               COMMENT '记录版本',
  `row_valid`        TINYINT       DEFAULT 1               COMMENT '记录是否有效：1-有效，0-无效',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_auth_code_id` (`auth_code_id`),
  UNIQUE KEY `uk_code_hash` (`code_hash`),
  KEY `idx_client_user` (`client_id`, `user_id`),
  KEY `idx_expire_time` (`expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='授权码记录表';

-- ============================================================================
-- 四、MFA 与风控 / 审计表组
-- 包含: ciam_mfa_challenge, ciam_risk_event, ciam_audit_log
-- ============================================================================

-- ----------------------------
-- 11. ciam_mfa_challenge — MFA 挑战表
-- ----------------------------
DROP TABLE IF EXISTS `ciam_mfa_challenge`;
CREATE TABLE `ciam_mfa_challenge` (
  `id`               BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `challenge_id`     VARCHAR(64)   NOT NULL                COMMENT '挑战业务唯一标识',
  `user_id`          VARCHAR(64)   NOT NULL                COMMENT '用户业务唯一标识',
  `session_id`       VARCHAR(64)   DEFAULT NULL            COMMENT '会话业务唯一标识',
  `challenge_type`   VARCHAR(32)   NOT NULL                COMMENT '挑战类型：sms,email',
  `challenge_scene`  VARCHAR(32)   NOT NULL                COMMENT '挑战场景：new_device,geo_change,high_risk',
  `receiver_mask`    VARCHAR(128)  DEFAULT NULL            COMMENT '脱敏接收目标',
  `verify_code_hash` CHAR(64)      DEFAULT NULL            COMMENT '验证码 SHA-256 哈希值（TokenDigest.fingerprint，原值不落库）',
  `send_time`        TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
  `expire_time`      TIMESTAMP     NOT NULL                COMMENT '过期时间',
  `verify_time`      TIMESTAMP     NULL DEFAULT NULL       COMMENT '验证通过时间',
  `challenge_status` TINYINT       NOT NULL DEFAULT 0      COMMENT '挑战状态：0-待验证，1-通过，2-失败，3-过期，4-取消',
  `risk_event_id`    VARCHAR(64)   DEFAULT NULL            COMMENT '关联风险事件业务标识',
  `description`      VARCHAR(255)  DEFAULT NULL            COMMENT '备注',
  `create_time`      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_by`        VARCHAR(64)   DEFAULT NULL            COMMENT '创建者',
  `modify_time`      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  `modify_by`        VARCHAR(64)   DEFAULT NULL            COMMENT '修改者',
  `row_version`      INT           DEFAULT 1               COMMENT '记录版本',
  `row_valid`        TINYINT       DEFAULT 1               COMMENT '记录是否有效：1-有效，0-无效',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_challenge_id` (`challenge_id`),
  KEY `idx_user_status` (`user_id`, `challenge_status`),
  KEY `idx_expire_time` (`expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='MFA挑战表';

-- ----------------------------
-- 12. ciam_risk_event — 风险事件表
-- ----------------------------
DROP TABLE IF EXISTS `ciam_risk_event`;
CREATE TABLE `ciam_risk_event` (
  `id`               BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `risk_event_id`    VARCHAR(64)   NOT NULL                COMMENT '风险事件业务唯一标识',
  `user_id`          VARCHAR(64)   DEFAULT NULL            COMMENT '用户业务唯一标识',
  `session_id`       VARCHAR(64)   DEFAULT NULL            COMMENT '会话业务唯一标识',
  `device_id`        VARCHAR(64)   DEFAULT NULL            COMMENT '设备业务唯一标识',
  `risk_scene`       VARCHAR(32)   NOT NULL                COMMENT '风险场景',
  `risk_type`        VARCHAR(32)   NOT NULL                COMMENT '风险类型',
  `risk_level`       TINYINT       NOT NULL DEFAULT 0      COMMENT '风险等级：0-低，1-中，2-高',
  `client_type`      VARCHAR(32)   DEFAULT NULL            COMMENT '客户端类型',
  `ip_address`       VARCHAR(64)   DEFAULT NULL            COMMENT 'IP地址',
  `region_code`      VARCHAR(32)   DEFAULT NULL            COMMENT '地区编码',
  `decision_result`  VARCHAR(32)   NOT NULL                COMMENT '处置结果：allow,challenge,block,kickout',
  `hit_rules`        VARCHAR(1024) DEFAULT NULL            COMMENT '命中规则列表（逗号分隔，LongTextSerializer.toCommaSeparated）',
  `event_time`       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '事件时间',
  `handled_flag`     TINYINT       NOT NULL DEFAULT 0      COMMENT '是否已处理：0-否，1-是',
  `description`      VARCHAR(255)  DEFAULT NULL            COMMENT '备注',
  `create_time`      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_by`        VARCHAR(64)   DEFAULT NULL            COMMENT '创建者',
  `modify_time`      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  `modify_by`        VARCHAR(64)   DEFAULT NULL            COMMENT '修改者',
  `row_version`      INT           DEFAULT 1               COMMENT '记录版本',
  `row_valid`        TINYINT       DEFAULT 1               COMMENT '记录是否有效：1-有效，0-无效',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_risk_event_id` (`risk_event_id`),
  KEY `idx_user_event_time` (`user_id`, `event_time`),
  KEY `idx_risk_level` (`risk_level`),
  KEY `idx_decision_result` (`decision_result`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='风险事件表';

-- ----------------------------
-- 13. ciam_audit_log — 审计日志表
-- ----------------------------
DROP TABLE IF EXISTS `ciam_audit_log`;
CREATE TABLE `ciam_audit_log` (
  `id`                BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `audit_id`          VARCHAR(64)   NOT NULL                COMMENT '审计日志业务唯一标识',
  `user_id`           VARCHAR(64)   DEFAULT NULL            COMMENT '用户业务唯一标识',
  `session_id`        VARCHAR(64)   DEFAULT NULL            COMMENT '会话业务唯一标识',
  `client_id`         VARCHAR(64)   DEFAULT NULL            COMMENT '客户端标识',
  `client_type`       VARCHAR(32)   DEFAULT NULL            COMMENT '客户端类型',
  `event_type`        VARCHAR(32)   NOT NULL                COMMENT '事件类型',
  `event_name`        VARCHAR(64)   NOT NULL                COMMENT '事件名称',
  `operation_result`  TINYINT       NOT NULL                COMMENT '操作结果：1-成功，0-失败',
  `request_uri`       VARCHAR(255)  DEFAULT NULL            COMMENT '请求URI',
  `request_method`    VARCHAR(16)   DEFAULT NULL            COMMENT '请求方法',
  `response_code`     VARCHAR(32)   DEFAULT NULL            COMMENT '响应码',
  `ip_address`        VARCHAR(64)   DEFAULT NULL            COMMENT '请求IP',
  `trace_id`          VARCHAR(64)   DEFAULT NULL            COMMENT '追踪标识',
  `request_snapshot`  TEXT          DEFAULT NULL            COMMENT '请求快照（JSON 格式脱敏存储，LongTextSerializer.toJson）',
  `event_time`        TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '事件时间',
  `description`       VARCHAR(255)  DEFAULT NULL            COMMENT '备注',
  `create_time`       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_by`         VARCHAR(64)   DEFAULT NULL            COMMENT '创建者',
  `modify_time`       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  `modify_by`         VARCHAR(64)   DEFAULT NULL            COMMENT '修改者',
  `row_version`       INT           DEFAULT 1               COMMENT '记录版本',
  `row_valid`         TINYINT       DEFAULT 1               COMMENT '记录是否有效：1-有效，0-无效',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_audit_id` (`audit_id`),
  KEY `idx_user_event_time` (`user_id`, `event_time`),
  KEY `idx_event_type_time` (`event_type`, `event_time`),
  KEY `idx_trace_id` (`trace_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='审计日志表';

-- ============================================================================
-- 五、账号生命周期表组
-- 包含: ciam_merge_request, ciam_deactivation_request
-- ============================================================================

-- ----------------------------
-- 14. ciam_merge_request — 账号合并申请表
-- ----------------------------
DROP TABLE IF EXISTS `ciam_merge_request`;
CREATE TABLE `ciam_merge_request` (
  `id`                     BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `merge_request_id`       VARCHAR(64)   NOT NULL                COMMENT '合并申请业务唯一标识',
  `source_user_id`         VARCHAR(64)   NOT NULL                COMMENT '源账号用户业务标识',
  `target_user_id`         VARCHAR(64)   NOT NULL                COMMENT '目标账号用户业务标识',
  `conflict_identity_type` VARCHAR(32)   NOT NULL                COMMENT '冲突标识类型',
  `conflict_identity_hash` CHAR(64)      NOT NULL                COMMENT '冲突标识 SHA-256 哈希值（FieldEncryptor.hash）',
  `apply_source`           VARCHAR(32)   DEFAULT NULL            COMMENT '申请来源',
  `review_status`          TINYINT       NOT NULL DEFAULT 0      COMMENT '审核状态：0-待审，1-通过，2-驳回，3-取消',
  `reviewer`               VARCHAR(64)   DEFAULT NULL            COMMENT '审核人',
  `review_time`            TIMESTAMP     NULL DEFAULT NULL       COMMENT '审核时间',
  `final_user_id`          VARCHAR(64)   DEFAULT NULL            COMMENT '最终保留用户业务标识',
  `finish_time`            TIMESTAMP     NULL DEFAULT NULL       COMMENT '完成时间',
  `description`            VARCHAR(255)  DEFAULT NULL            COMMENT '备注',
  `create_time`            TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_by`              VARCHAR(64)   DEFAULT NULL            COMMENT '创建者',
  `modify_time`            TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  `modify_by`              VARCHAR(64)   DEFAULT NULL            COMMENT '修改者',
  `row_version`            INT           DEFAULT 1               COMMENT '记录版本',
  `row_valid`              TINYINT       DEFAULT 1               COMMENT '记录是否有效：1-有效，0-无效',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_merge_request_id` (`merge_request_id`),
  KEY `idx_source_user` (`source_user_id`),
  KEY `idx_target_user` (`target_user_id`),
  KEY `idx_review_status` (`review_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='账号合并申请表';

-- ----------------------------
-- 15. ciam_deactivation_request — 注销申请表
-- ----------------------------
DROP TABLE IF EXISTS `ciam_deactivation_request`;
CREATE TABLE `ciam_deactivation_request` (
  `id`                       BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `deactivation_request_id`  VARCHAR(64)   NOT NULL                COMMENT '注销申请业务唯一标识',
  `user_id`                  VARCHAR(64)   NOT NULL                COMMENT '用户业务唯一标识',
  `request_source`           VARCHAR(32)   DEFAULT NULL            COMMENT '申请来源',
  `request_reason`           VARCHAR(255)  DEFAULT NULL            COMMENT '申请原因',
  `check_status`             TINYINT       NOT NULL DEFAULT 0      COMMENT '校验状态：0-待校验，1-通过，2-不通过',
  `review_status`            TINYINT       NOT NULL DEFAULT 0      COMMENT '审核状态：0-待审，1-通过，2-驳回',
  `execute_status`           TINYINT       NOT NULL DEFAULT 0      COMMENT '执行状态：0-待执行，1-已执行，2-失败',
  `requested_time`           TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
  `reviewer`                 VARCHAR(64)   DEFAULT NULL            COMMENT '审核人',
  `review_time`              TIMESTAMP     NULL DEFAULT NULL       COMMENT '审核时间',
  `execute_time`             TIMESTAMP     NULL DEFAULT NULL       COMMENT '执行时间',
  `retain_audit_only`        TINYINT       NOT NULL DEFAULT 1      COMMENT '是否仅保留脱敏审计凭证：0-否，1-是',
  `description`              VARCHAR(255)  DEFAULT NULL            COMMENT '备注',
  `create_time`              TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_by`                VARCHAR(64)   DEFAULT NULL            COMMENT '创建者',
  `modify_time`              TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  `modify_by`                VARCHAR(64)   DEFAULT NULL            COMMENT '修改者',
  `row_version`              INT           DEFAULT 1               COMMENT '记录版本',
  `row_valid`                TINYINT       DEFAULT 1               COMMENT '记录是否有效：1-有效，0-无效',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_deactivation_request_id` (`deactivation_request_id`),
  KEY `idx_user_review_status` (`user_id`, `review_status`),
  KEY `idx_execute_status` (`execute_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='注销申请表';

-- ============================================================================
-- 六、扩展业务表组
-- 包含: ciam_invitation_relation, ciam_owner_cert_state, ciam_user_consent
-- ============================================================================

-- ----------------------------
-- 16. ciam_invitation_relation — 邀请关系表
-- ----------------------------
DROP TABLE IF EXISTS `ciam_invitation_relation`;
CREATE TABLE `ciam_invitation_relation` (
  `id`                    BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `relation_id`           VARCHAR(64)   NOT NULL                COMMENT '邀请关系业务唯一标识',
  `inviter_user_id`       VARCHAR(64)   DEFAULT NULL            COMMENT '邀请人用户业务标识',
  `invitee_user_id`       VARCHAR(64)   NOT NULL                COMMENT '被邀请人用户业务标识',
  `invite_code`           VARCHAR(64)   DEFAULT NULL            COMMENT '邀请码',
  `invite_channel_code`   VARCHAR(64)   DEFAULT NULL            COMMENT '渠道码',
  `invite_activity_code`  VARCHAR(64)   DEFAULT NULL            COMMENT '活动码',
  `relation_lock_flag`    TINYINT       NOT NULL DEFAULT 1      COMMENT '关系是否固化：0-否，1-是',
  `register_time`         TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册成功时间',
  `description`           VARCHAR(255)  DEFAULT NULL            COMMENT '备注',
  `create_time`           TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_by`             VARCHAR(64)   DEFAULT NULL            COMMENT '创建者',
  `modify_time`           TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  `modify_by`             VARCHAR(64)   DEFAULT NULL            COMMENT '修改者',
  `row_version`           INT           DEFAULT 1               COMMENT '记录版本',
  `row_valid`             TINYINT       DEFAULT 1               COMMENT '记录是否有效：1-有效，0-无效',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_relation_id` (`relation_id`),
  KEY `idx_inviter_user_id` (`inviter_user_id`),
  KEY `idx_invitee_user_id` (`invitee_user_id`),
  KEY `idx_invite_channel_code` (`invite_channel_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='邀请关系表';

-- ----------------------------
-- 17. ciam_owner_cert_state — 车主认证状态表
-- ----------------------------
DROP TABLE IF EXISTS `ciam_owner_cert_state`;
CREATE TABLE `ciam_owner_cert_state` (
  `id`               BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `owner_cert_id`    VARCHAR(64)   NOT NULL                COMMENT '车主认证记录业务唯一标识',
  `user_id`          VARCHAR(64)   NOT NULL                COMMENT '用户业务唯一标识',
  `vehicle_id`       VARCHAR(64)   DEFAULT NULL            COMMENT '车辆业务唯一标识',
  `vin`              VARCHAR(32)   DEFAULT NULL            COMMENT '车架号',
  `cert_status`      TINYINT       NOT NULL DEFAULT 0      COMMENT '认证状态：0-未认证，1-认证中，2-已认证，3-认证失败，4-已失效',
  `cert_source`      VARCHAR(32)   DEFAULT NULL            COMMENT '认证来源系统',
  `callback_time`    TIMESTAMP     NULL DEFAULT NULL       COMMENT '回调时间',
  `last_query_time`  TIMESTAMP     NULL DEFAULT NULL       COMMENT '补偿查询时间',
  `effective_time`   TIMESTAMP     NULL DEFAULT NULL       COMMENT '生效时间',
  `expire_time`      TIMESTAMP     NULL DEFAULT NULL       COMMENT '失效时间',
  `result_message`   VARCHAR(255)  DEFAULT NULL            COMMENT '认证结果说明',
  `description`      VARCHAR(255)  DEFAULT NULL            COMMENT '备注',
  `create_time`      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_by`        VARCHAR(64)   DEFAULT NULL            COMMENT '创建者',
  `modify_time`      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  `modify_by`        VARCHAR(64)   DEFAULT NULL            COMMENT '修改者',
  `row_version`      INT           DEFAULT 1               COMMENT '记录版本',
  `row_valid`        TINYINT       DEFAULT 1               COMMENT '记录是否有效：1-有效，0-无效',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_owner_cert_id` (`owner_cert_id`),
  KEY `idx_user_cert_status` (`user_id`, `cert_status`),
  KEY `idx_vehicle_id` (`vehicle_id`),
  KEY `idx_vin` (`vin`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='车主认证状态表';

-- ----------------------------
-- 18. ciam_user_consent — 协议与营销同意表
-- ----------------------------
DROP TABLE IF EXISTS `ciam_user_consent`;
CREATE TABLE `ciam_user_consent` (
  `id`              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `consent_id`      VARCHAR(64)   NOT NULL                COMMENT '同意记录业务唯一标识',
  `user_id`         VARCHAR(64)   NOT NULL                COMMENT '用户业务唯一标识',
  `consent_type`    VARCHAR(32)   NOT NULL                COMMENT '同意类型：user_agreement,privacy_policy,marketing',
  `consent_status`  TINYINT       NOT NULL                COMMENT '同意状态：1-同意，0-撤回',
  `policy_version`  VARCHAR(32)   DEFAULT NULL            COMMENT '协议版本',
  `source_channel`  VARCHAR(64)   DEFAULT NULL            COMMENT '来源渠道',
  `client_type`     VARCHAR(32)   DEFAULT NULL            COMMENT '客户端类型',
  `operate_ip`      VARCHAR(64)   DEFAULT NULL            COMMENT '操作IP',
  `operate_time`    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  `description`     VARCHAR(255)  DEFAULT NULL            COMMENT '备注',
  `create_time`     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_by`       VARCHAR(64)   DEFAULT NULL            COMMENT '创建者',
  `modify_time`     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  `modify_by`       VARCHAR(64)   DEFAULT NULL            COMMENT '修改者',
  `row_version`     INT           DEFAULT 1               COMMENT '记录版本',
  `row_valid`       TINYINT       DEFAULT 1               COMMENT '记录是否有效：1-有效，0-无效',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_consent_id` (`consent_id`),
  KEY `idx_user_consent_type` (`user_id`, `consent_type`),
  KEY `idx_operate_time` (`operate_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='协议与营销同意表';
