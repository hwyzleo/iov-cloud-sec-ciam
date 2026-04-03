# CIAM 数据库字段级详细设计

## 概述

本文档基于 `design.md` 中的字段级表结构建议，对 CIAM 系统全部 18 张核心业务表进行逐表确认，明确每个字段的名称、类型、是否可空、默认值、注释及状态枚举值。

## 设计约定

### 双主键策略

所有核心业务表统一采用 **物理主键 `id` + 业务主键 `xxx_id`** 的双主键策略：

| 主键类型 | 字段名 | 类型 | 说明 |
|---------|--------|------|------|
| 物理主键 | `id` | `BIGINT NOT NULL AUTO_INCREMENT` | 数据库内部自增主键，仅用于存储引擎与内部关联 |
| 业务主键 | `xxx_id` | `VARCHAR(64) NOT NULL` | 全局唯一业务标识（雪花算法 ID 或 UUID），对外暴露，跨表关联优先使用 |

- 跨表关联优先使用业务 ID 字段，不直接依赖自增主键进行业务耦合
- 业务主键对业务侧稳定且不可复用
- `user_id` 可作为 OIDC `sub` 声明的来源字段

### 通用字段规范

所有核心业务表统一继承以下 7 个通用字段：

| 字段名 | 类型 | 可空 | 默认值 | 注释 |
|--------|------|------|--------|------|
| `description` | `VARCHAR(255)` | YES | `NULL` | 备注 |
| `create_time` | `TIMESTAMP` | NO | `CURRENT_TIMESTAMP` | 创建时间 |
| `create_by` | `VARCHAR(64)` | YES | `NULL` | 创建者 |
| `modify_time` | `TIMESTAMP` | NO | `CURRENT_TIMESTAMP` | 修改时间 |
| `modify_by` | `VARCHAR(64)` | YES | `NULL` | 修改者 |
| `row_version` | `INT` | YES | `1` | 记录版本（乐观锁） |
| `row_valid` | `TINYINT` | YES | `1` | 记录是否有效：1-有效，0-无效 |

补充约定：
- `modify_time` 是否使用 `ON UPDATE CURRENT_TIMESTAMP` 由统一建表规范决定，若不启用则由应用层显式维护
- `row_version` 用于乐观锁或更新防并发覆盖控制

### 全局技术约定

- 存储引擎：InnoDB
- 字符集：`utf8mb4`
- 排序规则：`utf8mb4_general_ci`
- 状态字段统一采用 `TINYINT`，配合注释枚举
- 时间字段优先命名为 `xxx_time`
- 哈希字段优先使用 `CHAR(64)` 存储 SHA-256 结果
- 多值集合（回调地址、授权范围、命中规则等）采用 `TEXT` 或 `VARCHAR(1024)` 存储序列化结果

### 索引设计策略

#### 索引分类规则

| 索引类型 | 命名规范 | 适用场景 |
|---------|---------|---------|
| 主键 | `PRIMARY KEY (id)` | 每张表物理主键 |
| 业务唯一索引 | `uk_<业务主键>` | 业务主键全局唯一，如 `uk_user_id`、`uk_session_id` |
| 业务复合唯一索引 | `uk_<业务含义>_valid` | 需要 `row_valid` 参与的逻辑唯一约束 |
| 普通索引 | `idx_<查询含义>` | 支撑核心查询场景 |

#### `row_valid` 参与唯一性约束的规则

系统采用 `row_valid` 字段实现逻辑删除。当 `row_valid` 参与唯一索引时，遵循以下规则：

1. **有效记录**：`row_valid = 1`，唯一索引正常生效，保证同一业务维度下仅存在一条有效记录
2. **软删除处理**：当需要逻辑删除一条记录时，应将 `row_valid` 设置为 `NULL`（而非 `0`），因为 MySQL 的 `UNIQUE KEY` 对 `NULL` 值不做唯一性校验，允许多条 `NULL` 记录共存
3. **适用表清单**：以下表的唯一索引包含 `row_valid`：
   - `ciam_user_identity`：`uk_identity_type_hash_valid (identity_type, identity_hash, row_valid)` — 保证同类型同哈希值仅一条有效标识
   - `ciam_user_credential`：`uk_user_credential_type_valid (user_id, credential_type, row_valid)` — 保证同用户同凭据类型仅一条有效凭据
   - `ciam_user_profile`：`uk_user_profile_valid (user_id, row_valid)` — 保证同用户仅一条有效资料
   - `ciam_user_tag`：`uk_user_tag_code_valid (user_id, tag_code, row_valid)` — 保证同用户同标签编码仅一条有效标签
4. **不参与唯一索引的表**：业务主键本身全局唯一的表（如 `ciam_user`、`ciam_session`、`ciam_audit_log` 等），其唯一索引仅约束业务 ID 字段，`row_valid` 不参与

#### 核心查询场景与索引支撑

| 查询场景 | 涉及表 | 支撑索引 |
|---------|--------|---------|
| 登录标识查重（注册/绑定） | `ciam_user_identity` | `uk_identity_type_hash_valid (identity_type, identity_hash, row_valid)` |
| 用户登录标识列表 | `ciam_user_identity` | `idx_user_id (user_id)` |
| 用户凭据校验 | `ciam_user_credential` | `uk_user_credential_type_valid (user_id, credential_type, row_valid)` |
| 用户会话列表 | `ciam_session` | `idx_user_session_status (user_id, session_status)` |
| 设备关联会话查询 | `ciam_session` | `idx_device_id (device_id)` |
| 过期会话清理 | `ciam_session` | `idx_expire_time (expire_time)` |
| 用户设备列表 | `ciam_device` | `idx_user_device_status (user_id, device_status)` |
| 设备指纹识别 | `ciam_device` | `idx_device_fingerprint (device_fingerprint)` |
| 密码修改后批量吊销令牌 | `ciam_refresh_token` | `idx_user_token_status (user_id, token_status)` |
| 会话关联令牌查询 | `ciam_refresh_token` | `idx_session_status (session_id, token_status)` |
| 令牌指纹校验 | `ciam_refresh_token` | `uk_token_fingerprint (token_fingerprint)` |
| 授权码兑换 | `ciam_auth_code` | `uk_code_hash (code_hash)` |
| 用户审计日志检索 | `ciam_audit_log` | `idx_user_event_time (user_id, event_time)` |
| 事件类型审计检索 | `ciam_audit_log` | `idx_event_type_time (event_type, event_time)` |
| 链路追踪审计检索 | `ciam_audit_log` | `idx_trace_id (trace_id)` |
| 用户风险事件查询 | `ciam_risk_event` | `idx_user_event_time (user_id, event_time)` |
| 风险等级筛选 | `ciam_risk_event` | `idx_risk_level (risk_level)` |
| 合并申请审核列表 | `ciam_merge_request` | `idx_review_status (review_status)` |
| 注销申请审核列表 | `ciam_deactivation_request` | `idx_user_review_status (user_id, review_status)` |
| 车主认证状态查询 | `ciam_owner_cert_state` | `idx_user_cert_status (user_id, cert_status)` |
| 渠道归因统计 | `ciam_invitation_relation` | `idx_invite_channel_code (invite_channel_code)` |

#### MySQL 与 Elasticsearch 分工

- **MySQL 索引**：承载事务性查询、唯一性校验、关联查询等 OLTP 场景
- **Elasticsearch 索引**：承载用户检索、审计日志全文检索、安全事件聚合分析、运营统计等 OLAP 场景
- MySQL 中的普通索引主要服务于精确匹配和范围查询；模糊搜索、全文检索、多维聚合分析由 ES 承担
- ES 索引数据通过 MySQL binlog 或 Kafka 事件异步构建，不作为主数据来源

---

## 表结构详细设计

### 1. ciam_user — 用户主表

用途：统一用户主档，承载稳定业务身份和主状态。

| # | 字段名 | 类型 | 可空 | 默认值 | 注释 |
|---|--------|------|------|--------|------|
| 1 | `id` | `BIGINT` | NO | AUTO_INCREMENT | 主键 |
| 2 | `user_id` | `VARCHAR(64)` | NO | — | 用户业务唯一标识 |
| 3 | `user_status` | `TINYINT` | NO | `0` | 用户状态 |
| 4 | `brand_code` | `VARCHAR(32)` | NO | `'OPENIOV'` | 品牌编码 |
| 5 | `register_source` | `VARCHAR(32)` | YES | `NULL` | 注册来源 |
| 6 | `register_channel` | `VARCHAR(64)` | YES | `NULL` | 注册渠道 |
| 7 | `primary_identity_type` | `VARCHAR(32)` | YES | `NULL` | 主登录标识类型 |
| 8 | `last_login_time` | `TIMESTAMP` | YES | `NULL` | 最后登录时间 |
| 9 | `deactivated_time` | `TIMESTAMP` | YES | `NULL` | 注销完成时间 |
| 10 | `description` | `VARCHAR(255)` | YES | `NULL` | 备注 |
| 11 | `create_time` | `TIMESTAMP` | NO | `CURRENT_TIMESTAMP` | 创建时间 |
| 12 | `create_by` | `VARCHAR(64)` | YES | `NULL` | 创建者 |
| 13 | `modify_time` | `TIMESTAMP` | NO | `CURRENT_TIMESTAMP` | 修改时间 |
| 14 | `modify_by` | `VARCHAR(64)` | YES | `NULL` | 修改者 |
| 15 | `row_version` | `INT` | YES | `1` | 记录版本 |
| 16 | `row_valid` | `TINYINT` | YES | `1` | 记录是否有效 |

状态枚举 `user_status`：

| 值 | 含义 |
|----|------|
| 0 | 待验证 |
| 1 | 正常 |
| 2 | 已锁定 |
| 3 | 已禁用 |
| 4 | 注销处理中 |
| 5 | 已注销 |

枚举 `register_source`：`mobile`, `email`, `wechat`, `apple`, `google`, `local_mobile`

索引策略：

| 索引名 | 类型 | 字段 |
|--------|------|------|
| `PRIMARY` | 主键 | `(id)` |
| `uk_user_id` | 唯一索引 | `(user_id)` |
| `idx_user_status` | 普通索引 | `(user_status)` |
| `idx_last_login_time` | 普通索引 | `(last_login_time)` |

---

### 2. ciam_user_identity — 登录标识表

用途：用户与登录标识映射，一名用户可绑定多个标识。

| # | 字段名 | 类型 | 可空 | 默认值 | 注释 |
|---|--------|------|------|--------|------|
| 1 | `id` | `BIGINT` | NO | AUTO_INCREMENT | 主键 |
| 2 | `identity_id` | `VARCHAR(64)` | NO | — | 登录标识业务唯一标识 |
| 3 | `user_id` | `VARCHAR(64)` | NO | — | 用户业务唯一标识 |
| 4 | `identity_type` | `VARCHAR(32)` | NO | — | 标识类型 |
| 5 | `identity_value` | `VARCHAR(255)` | NO | — | 登录标识原值（手机号邮箱建议加密存储） |
| 6 | `identity_hash` | `CHAR(64)` | NO | — | 登录标识哈希值（用于唯一查重） |
| 7 | `country_code` | `VARCHAR(8)` | YES | `NULL` | 国家区号（手机号场景使用） |
| 8 | `verified_flag` | `TINYINT` | NO | `0` | 是否已验证 |
| 9 | `primary_flag` | `TINYINT` | NO | `0` | 是否主标识 |
| 10 | `bind_source` | `VARCHAR(32)` | YES | `NULL` | 绑定来源 |
| 11 | `bind_time` | `TIMESTAMP` | NO | `CURRENT_TIMESTAMP` | 绑定时间 |
| 12 | `unbind_time` | `TIMESTAMP` | YES | `NULL` | 解绑时间 |
| 13 | `identity_status` | `TINYINT` | NO | `1` | 标识状态 |
| 14 | `description` | `VARCHAR(255)` | YES | `NULL` | 备注 |
| 15 | `create_time` | `TIMESTAMP` | NO | `CURRENT_TIMESTAMP` | 创建时间 |
| 16 | `create_by` | `VARCHAR(64)` | YES | `NULL` | 创建者 |
| 17 | `modify_time` | `TIMESTAMP` | NO | `CURRENT_TIMESTAMP` | 修改时间 |
| 18 | `modify_by` | `VARCHAR(64)` | YES | `NULL` | 修改者 |
| 19 | `row_version` | `INT` | YES | `1` | 记录版本 |
| 20 | `row_valid` | `TINYINT` | YES | `1` | 记录是否有效 |

枚举 `identity_type`：`mobile`, `email`, `wechat`, `apple`, `google`, `local_mobile`

状态枚举 `identity_status`：

| 值 | 含义 |
|----|------|
| 1 | 已绑定 |
| 0 | 已解绑 |

标志位 `verified_flag`：0-否，1-是

标志位 `primary_flag`：0-否，1-是

索引策略：

| 索引名 | 类型 | 字段 |
|--------|------|------|
| `PRIMARY` | 主键 | `(id)` |
| `uk_identity_id` | 唯一索引 | `(identity_id)` |
| `uk_identity_type_hash_valid` | 唯一索引 | `(identity_type, identity_hash, row_valid)` |
| `idx_user_id` | 普通索引 | `(user_id)` |
| `idx_identity_type_status` | 普通索引 | `(identity_type, identity_status)` |

---

### 3. ciam_user_credential — 凭据表

用途：密码类凭据存储，仅邮箱密码登录场景首发启用。

| # | 字段名 | 类型 | 可空 | 默认值 | 注释 |
|---|--------|------|------|--------|------|
| 1 | `id` | `BIGINT` | NO | AUTO_INCREMENT | 主键 |
| 2 | `credential_id` | `VARCHAR(64)` | NO | — | 凭据业务唯一标识 |
| 3 | `user_id` | `VARCHAR(64)` | NO | — | 用户业务唯一标识 |
| 4 | `credential_type` | `VARCHAR(32)` | NO | — | 凭据类型 |
| 5 | `credential_hash` | `VARCHAR(255)` | NO | — | 凭据哈希值 |
| 6 | `salt` | `VARCHAR(128)` | YES | `NULL` | 盐值（按算法需要决定是否保留） |
| 7 | `hash_algorithm` | `VARCHAR(32)` | NO | — | 哈希算法 |
| 8 | `password_set_time` | `TIMESTAMP` | NO | `CURRENT_TIMESTAMP` | 密码设置时间 |
| 9 | `last_verify_time` | `TIMESTAMP` | YES | `NULL` | 最后校验成功时间 |
| 10 | `fail_count` | `INT` | NO | `0` | 连续失败次数 |
| 11 | `locked_until` | `TIMESTAMP` | YES | `NULL` | 锁定截止时间 |
| 12 | `credential_status` | `TINYINT` | NO | `1` | 凭据状态 |
| 13 | `description` | `VARCHAR(255)` | YES | `NULL` | 备注 |
| 14 | `create_time` | `TIMESTAMP` | NO | `CURRENT_TIMESTAMP` | 创建时间 |
| 15 | `create_by` | `VARCHAR(64)` | YES | `NULL` | 创建者 |
| 16 | `modify_time` | `TIMESTAMP` | NO | `CURRENT_TIMESTAMP` | 修改时间 |
| 17 | `modify_by` | `VARCHAR(64)` | YES | `NULL` | 修改者 |
| 18 | `row_version` | `INT` | YES | `1` | 记录版本 |
| 19 | `row_valid` | `TINYINT` | YES | `1` | 记录是否有效 |

枚举 `credential_type`：`email_password`

状态枚举 `credential_status`：

| 值 | 含义 |
|----|------|
| 1 | 有效 |
| 0 | 失效 |

索引策略：

| 索引名 | 类型 | 字段 |
|--------|------|------|
| `PRIMARY` | 主键 | `(id)` |
| `uk_credential_id` | 唯一索引 | `(credential_id)` |
| `uk_user_credential_type_valid` | 唯一索引 | `(user_id, credential_type, row_valid)` |
| `idx_locked_until` | 普通索引 | `(locked_until)` |

---

### 4. ciam_user_profile — 用户资料扩展表

用途：用户资料扩展信息，与用户主表一对一。

| # | 字段名 | 类型 | 可空 | 默认值 | 注释 |
|---|--------|------|------|--------|------|
| 1 | `id` | `BIGINT` | NO | AUTO_INCREMENT | 主键 |
| 2 | `profile_id` | `VARCHAR(64)` | NO | — | 资料业务唯一标识 |
| 3 | `user_id` | `VARCHAR(64)` | NO | — | 用户业务唯一标识 |
| 4 | `nickname` | `VARCHAR(64)` | YES | `NULL` | 昵称 |
| 5 | `avatar_url` | `VARCHAR(255)` | YES | `NULL` | 头像地址 |
| 6 | `real_name` | `VARCHAR(64)` | YES | `NULL` | 姓名 |
| 7 | `gender` | `TINYINT` | NO | `0` | 性别 |
| 8 | `birthday` | `DATE` | YES | `NULL` | 生日 |
| 9 | `region_code` | `VARCHAR(32)` | YES | `NULL` | 地区编码 |
| 10 | `region_name` | `VARCHAR(128)` | YES | `NULL` | 地区名称 |
| 11 | `description` | `VARCHAR(255)` | YES | `NULL` | 备注 |
| 12 | `create_time` | `TIMESTAMP` | NO | `CURRENT_TIMESTAMP` | 创建时间 |
| 13 | `create_by` | `VARCHAR(64)` | YES | `NULL` | 创建者 |
| 14 | `modify_time` | `TIMESTAMP` | NO | `CURRENT_TIMESTAMP` | 修改时间 |
| 15 | `modify_by` | `VARCHAR(64)` | YES | `NULL` | 修改者 |
| 16 | `row_version` | `INT` | YES | `1` | 记录版本 |
| 17 | `row_valid` | `TINYINT` | YES | `1` | 记录是否有效 |

枚举 `gender`：

| 值 | 含义 |
|----|------|
| 0 | 未知 |
| 1 | 男 |
| 2 | 女 |

索引策略：

| 索引名 | 类型 | 字段 |
|--------|------|------|
| `PRIMARY` | 主键 | `(id)` |
| `uk_profile_id` | 唯一索引 | `(profile_id)` |
| `uk_user_profile_valid` | 唯一索引 | `(user_id, row_valid)` |
| `idx_region_code` | 普通索引 | `(region_code)` |

---

### 5. ciam_user_tag — 认证标签表

用途：实名、车主认证等标签记录。

| # | 字段名 | 类型 | 可空 | 默认值 | 注释 |
|---|--------|------|------|--------|------|
| 1 | `id` | `BIGINT` | NO | AUTO_INCREMENT | 主键 |
| 2 | `tag_id` | `VARCHAR(64)` | NO | — | 标签业务唯一标识 |
| 3 | `user_id` | `VARCHAR(64)` | NO | — | 用户业务唯一标识 |
| 4 | `tag_code` | `VARCHAR(32)` | NO | — | 标签编码 |
| 5 | `tag_name` | `VARCHAR(64)` | NO | — | 标签名称 |
| 6 | `tag_status` | `TINYINT` | NO | `1` | 标签状态 |
| 7 | `tag_source` | `VARCHAR(32)` | YES | `NULL` | 标签来源 |
| 8 | `effective_time` | `TIMESTAMP` | NO | `CURRENT_TIMESTAMP` | 生效时间 |
| 9 | `expire_time` | `TIMESTAMP` | YES | `NULL` | 失效时间 |
| 10 | `description` | `VARCHAR(255)` | YES | `NULL` | 备注 |
| 11 | `create_time` | `TIMESTAMP` | NO | `CURRENT_TIMESTAMP` | 创建时间 |
| 12 | `create_by` | `VARCHAR(64)` | YES | `NULL` | 创建者 |
| 13 | `modify_time` | `TIMESTAMP` | NO | `CURRENT_TIMESTAMP` | 修改时间 |
| 14 | `modify_by` | `VARCHAR(64)` | YES | `NULL` | 修改者 |
| 15 | `row_version` | `INT` | YES | `1` | 记录版本 |
| 16 | `row_valid` | `TINYINT` | YES | `1` | 记录是否有效 |

枚举 `tag_code`：`real_name_verified`, `owner_verified`

状态枚举 `tag_status`：

| 值 | 含义 |
|----|------|
| 1 | 生效 |
| 0 | 失效 |

索引策略：

| 索引名 | 类型 | 字段 |
|--------|------|------|
| `PRIMARY` | 主键 | `(id)` |
| `uk_tag_id` | 唯一索引 | `(tag_id)` |
| `uk_user_tag_code_valid` | 唯一索引 | `(user_id, tag_code, row_valid)` |
| `idx_tag_code_status` | 普通索引 | `(tag_code, tag_status)` |

---

### 6. ciam_user_consent — 协议与营销同意表

用途：用户协议、隐私政策、营销同意记录。

| # | 字段名 | 类型 | 可空 | 默认值 | 注释 |
|---|--------|------|------|--------|------|
| 1 | `id` | `BIGINT` | NO | AUTO_INCREMENT | 主键 |
| 2 | `consent_id` | `VARCHAR(64)` | NO | — | 同意记录业务唯一标识 |
| 3 | `user_id` | `VARCHAR(64)` | NO | — | 用户业务唯一标识 |
| 4 | `consent_type` | `VARCHAR(32)` | NO | — | 同意类型 |
| 5 | `consent_status` | `TINYINT` | NO | — | 同意状态 |
| 6 | `policy_version` | `VARCHAR(32)` | YES | `NULL` | 协议版本 |
| 7 | `source_channel` | `VARCHAR(64)` | YES | `NULL` | 来源渠道 |
| 8 | `client_type` | `VARCHAR(32)` | YES | `NULL` | 客户端类型 |
| 9 | `operate_ip` | `VARCHAR(64)` | YES | `NULL` | 操作IP |
| 10 | `operate_time` | `TIMESTAMP` | NO | `CURRENT_TIMESTAMP` | 操作时间 |
| 11 | `description` | `VARCHAR(255)` | YES | `NULL` | 备注 |
| 12 | `create_time` | `TIMESTAMP` | NO | `CURRENT_TIMESTAMP` | 创建时间 |
| 13 | `create_by` | `VARCHAR(64)` | YES | `NULL` | 创建者 |
| 14 | `modify_time` | `TIMESTAMP` | NO | `CURRENT_TIMESTAMP` | 修改时间 |
| 15 | `modify_by` | `VARCHAR(64)` | YES | `NULL` | 修改者 |
| 16 | `row_version` | `INT` | YES | `1` | 记录版本 |
| 17 | `row_valid` | `TINYINT` | YES | `1` | 记录是否有效 |

枚举 `consent_type`：`user_agreement`, `privacy_policy`, `marketing`

状态枚举 `consent_status`：

| 值 | 含义 |
|----|------|
| 1 | 同意 |
| 0 | 撤回 |

索引策略：

| 索引名 | 类型 | 字段 |
|--------|------|------|
| `PRIMARY` | 主键 | `(id)` |
| `uk_consent_id` | 唯一索引 | `(consent_id)` |
| `idx_user_consent_type` | 普通索引 | `(user_id, consent_type)` |
| `idx_operate_time` | 普通索引 | `(operate_time)` |

---

### 7. ciam_session — 会话表

用途：用户登录会话主表。

| # | 字段名 | 类型 | 可空 | 默认值 | 注释 |
|---|--------|------|------|--------|------|
| 1 | `id` | `BIGINT` | NO | AUTO_INCREMENT | 主键 |
| 2 | `session_id` | `VARCHAR(64)` | NO | — | 会话业务唯一标识 |
| 3 | `user_id` | `VARCHAR(64)` | NO | — | 用户业务唯一标识 |
| 4 | `device_id` | `VARCHAR(64)` | YES | `NULL` | 设备业务唯一标识 |
| 5 | `client_id` | `VARCHAR(64)` | YES | `NULL` | OAuth客户端标识 |
| 6 | `client_type` | `VARCHAR(32)` | NO | — | 客户端类型 |
| 7 | `login_ip` | `VARCHAR(64)` | YES | `NULL` | 登录IP |
| 8 | `login_region` | `VARCHAR(128)` | YES | `NULL` | 登录地区 |
| 9 | `risk_level` | `TINYINT` | NO | `0` | 风险等级 |
| 10 | `session_status` | `TINYINT` | NO | `1` | 会话状态 |
| 11 | `login_time` | `TIMESTAMP` | NO | `CURRENT_TIMESTAMP` | 登录时间 |
| 12 | `last_active_time` | `TIMESTAMP` | NO | `CURRENT_TIMESTAMP` | 最后活跃时间 |
| 13 | `logout_time` | `TIMESTAMP` | YES | `NULL` | 退出时间 |
| 14 | `expire_time` | `TIMESTAMP` | NO | — | 过期时间 |
| 15 | `description` | `VARCHAR(255)` | YES | `NULL` | 备注 |
| 16 | `create_time` | `TIMESTAMP` | NO | `CURRENT_TIMESTAMP` | 创建时间 |
| 17 | `create_by` | `VARCHAR(64)` | YES | `NULL` | 创建者 |
| 18 | `modify_time` | `TIMESTAMP` | NO | `CURRENT_TIMESTAMP` | 修改时间 |
| 19 | `modify_by` | `VARCHAR(64)` | YES | `NULL` | 修改者 |
| 20 | `row_version` | `INT` | YES | `1` | 记录版本 |
| 21 | `row_valid` | `TINYINT` | YES | `1` | 记录是否有效 |

枚举 `client_type`：`app`, `mini_program`, `web`, `vehicle`, `admin`

状态枚举 `session_status`：

| 值 | 含义 |
|----|------|
| 1 | 有效 |
| 0 | 失效 |
| 2 | 下线 |
| 3 | 过期 |

枚举 `risk_level`：

| 值 | 含义 |
|----|------|
| 0 | 低 |
| 1 | 中 |
| 2 | 高 |

索引策略：

| 索引名 | 类型 | 字段 |
|--------|------|------|
| `PRIMARY` | 主键 | `(id)` |
| `uk_session_id` | 唯一索引 | `(session_id)` |
| `idx_user_session_status` | 普通索引 | `(user_id, session_status)` |
| `idx_device_id` | 普通索引 | `(device_id)` |
| `idx_expire_time` | 普通索引 | `(expire_time)` |

---

### 8. ciam_device — 设备表

用途：用户登录设备记录，用于设备管理与风险识别。

| # | 字段名 | 类型 | 可空 | 默认值 | 注释 |
|---|--------|------|------|--------|------|
| 1 | `id` | `BIGINT` | NO | AUTO_INCREMENT | 主键 |
| 2 | `device_id` | `VARCHAR(64)` | NO | — | 设备业务唯一标识 |
| 3 | `user_id` | `VARCHAR(64)` | NO | — | 用户业务唯一标识 |
| 4 | `client_type` | `VARCHAR(32)` | NO | — | 客户端类型 |
| 5 | `device_type` | `VARCHAR(32)` | YES | `NULL` | 设备类型 |
| 6 | `device_name` | `VARCHAR(64)` | YES | `NULL` | 设备名称 |
| 7 | `device_os` | `VARCHAR(64)` | YES | `NULL` | 设备操作系统 |
| 8 | `app_version` | `VARCHAR(32)` | YES | `NULL` | 应用版本 |
| 9 | `device_fingerprint` | `VARCHAR(128)` | YES | `NULL` | 设备指纹 |
| 10 | `trusted_flag` | `TINYINT` | NO | `0` | 是否受信任设备 |
| 11 | `first_login_time` | `TIMESTAMP` | NO | `CURRENT_TIMESTAMP` | 首次登录时间 |
| 12 | `last_login_time` | `TIMESTAMP` | NO | `CURRENT_TIMESTAMP` | 最后登录时间 |
| 13 | `device_status` | `TINYINT` | NO | `1` | 设备状态 |
| 14 | `description` | `VARCHAR(255)` | YES | `NULL` | 备注 |
| 15 | `create_time` | `TIMESTAMP` | NO | `CURRENT_TIMESTAMP` | 创建时间 |
| 16 | `create_by` | `VARCHAR(64)` | YES | `NULL` | 创建者 |
| 17 | `modify_time` | `TIMESTAMP` | NO | `CURRENT_TIMESTAMP` | 修改时间 |
| 18 | `modify_by` | `VARCHAR(64)` | YES | `NULL` | 修改者 |
| 19 | `row_version` | `INT` | YES | `1` | 记录版本 |
| 20 | `row_valid` | `TINYINT` | YES | `1` | 记录是否有效 |

标志位 `trusted_flag`：0-否，1-是

状态枚举 `device_status`：

| 值 | 含义 |
|----|------|
| 1 | 正常 |
| 0 | 失效 |

索引策略：

| 索引名 | 类型 | 字段 |
|--------|------|------|
| `PRIMARY` | 主键 | `(id)` |
| `uk_device_id` | 唯一索引 | `(device_id)` |
| `idx_user_device_status` | 普通索引 | `(user_id, device_status)` |
| `idx_device_fingerprint` | 普通索引 | `(device_fingerprint)` |

---

### 9. ciam_oauth_client — 接入应用表

用途：内部系统接入客户端配置。

| # | 字段名 | 类型 | 可空 | 默认值 | 注释 |
|---|--------|------|------|--------|------|
| 1 | `id` | `BIGINT` | NO | AUTO_INCREMENT | 主键 |
| 2 | `client_id` | `VARCHAR(64)` | NO | — | 客户端标识 |
| 3 | `client_name` | `VARCHAR(128)` | NO | — | 客户端名称 |
| 4 | `client_secret_hash` | `VARCHAR(255)` | YES | `NULL` | 客户端密钥哈希（公开客户端可为空） |
| 5 | `client_type` | `VARCHAR(32)` | NO | — | 客户端类型 |
| 6 | `redirect_uris` | `TEXT` | YES | `NULL` | 回调地址列表（序列化存储） |
| 7 | `grant_types` | `VARCHAR(255)` | NO | — | 授权类型列表 |
| 8 | `scopes` | `VARCHAR(255)` | YES | `NULL` | 授权范围列表 |
| 9 | `pkce_required` | `TINYINT` | NO | `1` | 是否强制PKCE |
| 10 | `access_token_ttl` | `INT` | NO | `1800` | 访问令牌有效期（秒） |
| 11 | `refresh_token_ttl` | `INT` | NO | `2592000` | 刷新令牌有效期（秒） |
| 12 | `client_status` | `TINYINT` | NO | `1` | 客户端状态 |
| 13 | `description` | `VARCHAR(255)` | YES | `NULL` | 备注 |
| 14 | `create_time` | `TIMESTAMP` | NO | `CURRENT_TIMESTAMP` | 创建时间 |
| 15 | `create_by` | `VARCHAR(64)` | YES | `NULL` | 创建者 |
| 16 | `modify_time` | `TIMESTAMP` | NO | `CURRENT_TIMESTAMP` | 修改时间 |
| 17 | `modify_by` | `VARCHAR(64)` | YES | `NULL` | 修改者 |
| 18 | `row_version` | `INT` | YES | `1` | 记录版本 |
| 19 | `row_valid` | `TINYINT` | YES | `1` | 记录是否有效 |

枚举 `client_type`（OAuth）：`public`, `confidential`, `internal`

标志位 `pkce_required`：0-否，1-是

状态枚举 `client_status`：

| 值 | 含义 |
|----|------|
| 1 | 启用 |
| 0 | 停用 |

索引策略：

| 索引名 | 类型 | 字段 |
|--------|------|------|
| `PRIMARY` | 主键 | `(id)` |
| `uk_client_id` | 唯一索引 | `(client_id)` |
| `idx_client_status` | 普通索引 | `(client_status)` |

---

### 10. ciam_auth_code — 授权码记录表

用途：Authorization Code + PKCE 授权码记录。

| # | 字段名 | 类型 | 可空 | 默认值 | 注释 |
|---|--------|------|------|--------|------|
| 1 | `id` | `BIGINT` | NO | AUTO_INCREMENT | 主键 |
| 2 | `auth_code_id` | `VARCHAR(64)` | NO | — | 授权码业务唯一标识 |
| 3 | `client_id` | `VARCHAR(64)` | NO | — | 客户端标识 |
| 4 | `user_id` | `VARCHAR(64)` | NO | — | 用户业务唯一标识 |
| 5 | `session_id` | `VARCHAR(64)` | YES | `NULL` | 会话业务唯一标识 |
| 6 | `code_hash` | `CHAR(64)` | NO | — | 授权码哈希值 |
| 7 | `redirect_uri` | `VARCHAR(255)` | NO | — | 回调地址 |
| 8 | `scope` | `VARCHAR(255)` | YES | `NULL` | 授权范围 |
| 9 | `code_challenge` | `VARCHAR(128)` | YES | `NULL` | PKCE challenge |
| 10 | `challenge_method` | `VARCHAR(16)` | YES | `NULL` | PKCE challenge method |
| 11 | `expire_time` | `TIMESTAMP` | NO | — | 过期时间 |
| 12 | `used_flag` | `TINYINT` | NO | `0` | 是否已使用 |
| 13 | `used_time` | `TIMESTAMP` | YES | `NULL` | 使用时间 |
| 14 | `description` | `VARCHAR(255)` | YES | `NULL` | 备注 |
| 15 | `create_time` | `TIMESTAMP` | NO | `CURRENT_TIMESTAMP` | 创建时间 |
| 16 | `create_by` | `VARCHAR(64)` | YES | `NULL` | 创建者 |
| 17 | `modify_time` | `TIMESTAMP` | NO | `CURRENT_TIMESTAMP` | 修改时间 |
| 18 | `modify_by` | `VARCHAR(64)` | YES | `NULL` | 修改者 |
| 19 | `row_version` | `INT` | YES | `1` | 记录版本 |
| 20 | `row_valid` | `TINYINT` | YES | `1` | 记录是否有效 |

标志位 `used_flag`：0-否，1-是

索引策略：

| 索引名 | 类型 | 字段 |
|--------|------|------|
| `PRIMARY` | 主键 | `(id)` |
| `uk_auth_code_id` | 唯一索引 | `(auth_code_id)` |
| `uk_code_hash` | 唯一索引 | `(code_hash)` |
| `idx_client_user` | 普通索引 | `(client_id, user_id)` |
| `idx_expire_time` | 普通索引 | `(expire_time)` |

---

### 11. ciam_refresh_token — 刷新令牌表

用途：Refresh Token 旋转链路与服务端存储。

| # | 字段名 | 类型 | 可空 | 默认值 | 注释 |
|---|--------|------|------|--------|------|
| 1 | `id` | `BIGINT` | NO | AUTO_INCREMENT | 主键 |
| 2 | `refresh_token_id` | `VARCHAR(64)` | NO | — | 刷新令牌业务唯一标识 |
| 3 | `user_id` | `VARCHAR(64)` | NO | — | 用户业务唯一标识 |
| 4 | `session_id` | `VARCHAR(64)` | NO | — | 会话业务唯一标识 |
| 5 | `client_id` | `VARCHAR(64)` | YES | `NULL` | 客户端标识 |
| 6 | `token_fingerprint` | `CHAR(64)` | NO | — | 刷新令牌指纹 |
| 7 | `parent_token_id` | `VARCHAR(64)` | YES | `NULL` | 上一个刷新令牌业务标识 |
| 8 | `token_status` | `TINYINT` | NO | `1` | 令牌状态 |
| 9 | `issue_time` | `TIMESTAMP` | NO | `CURRENT_TIMESTAMP` | 签发时间 |
| 10 | `used_time` | `TIMESTAMP` | YES | `NULL` | 使用时间 |
| 11 | `revoke_time` | `TIMESTAMP` | YES | `NULL` | 撤销时间 |
| 12 | `expire_time` | `TIMESTAMP` | NO | — | 过期时间 |
| 13 | `description` | `VARCHAR(255)` | YES | `NULL` | 备注 |
| 14 | `create_time` | `TIMESTAMP` | NO | `CURRENT_TIMESTAMP` | 创建时间 |
| 15 | `create_by` | `VARCHAR(64)` | YES | `NULL` | 创建者 |
| 16 | `modify_time` | `TIMESTAMP` | NO | `CURRENT_TIMESTAMP` | 修改时间 |
| 17 | `modify_by` | `VARCHAR(64)` | YES | `NULL` | 修改者 |
| 18 | `row_version` | `INT` | YES | `1` | 记录版本 |
| 19 | `row_valid` | `TINYINT` | YES | `1` | 记录是否有效 |

状态枚举 `token_status`：

| 值 | 含义 |
|----|------|
| 1 | 有效 |
| 2 | 已轮换 |
| 3 | 已撤销 |
| 4 | 已过期 |

索引策略：

| 索引名 | 类型 | 字段 |
|--------|------|------|
| `PRIMARY` | 主键 | `(id)` |
| `uk_refresh_token_id` | 唯一索引 | `(refresh_token_id)` |
| `uk_token_fingerprint` | 唯一索引 | `(token_fingerprint)` |
| `idx_user_token_status` | 普通索引 | `(user_id, token_status)` |
| `idx_session_status` | 普通索引 | `(session_id, token_status)` |
| `idx_expire_time` | 普通索引 | `(expire_time)` |

---

### 12. ciam_mfa_challenge — MFA 挑战表

用途：MFA 二次挑战记录。

| # | 字段名 | 类型 | 可空 | 默认值 | 注释 |
|---|--------|------|------|--------|------|
| 1 | `id` | `BIGINT` | NO | AUTO_INCREMENT | 主键 |
| 2 | `challenge_id` | `VARCHAR(64)` | NO | — | 挑战业务唯一标识 |
| 3 | `user_id` | `VARCHAR(64)` | NO | — | 用户业务唯一标识 |
| 4 | `session_id` | `VARCHAR(64)` | YES | `NULL` | 会话业务唯一标识 |
| 5 | `challenge_type` | `VARCHAR(32)` | NO | — | 挑战类型 |
| 6 | `challenge_scene` | `VARCHAR(32)` | NO | — | 挑战场景 |
| 7 | `receiver_mask` | `VARCHAR(128)` | YES | `NULL` | 脱敏接收目标 |
| 8 | `verify_code_hash` | `CHAR(64)` | YES | `NULL` | 验证码哈希值 |
| 9 | `send_time` | `TIMESTAMP` | NO | `CURRENT_TIMESTAMP` | 发送时间 |
| 10 | `expire_time` | `TIMESTAMP` | NO | — | 过期时间 |
| 11 | `verify_time` | `TIMESTAMP` | YES | `NULL` | 验证通过时间 |
| 12 | `challenge_status` | `TINYINT` | NO | `0` | 挑战状态 |
| 13 | `risk_event_id` | `VARCHAR(64)` | YES | `NULL` | 关联风险事件业务标识 |
| 14 | `description` | `VARCHAR(255)` | YES | `NULL` | 备注 |
| 15 | `create_time` | `TIMESTAMP` | NO | `CURRENT_TIMESTAMP` | 创建时间 |
| 16 | `create_by` | `VARCHAR(64)` | YES | `NULL` | 创建者 |
| 17 | `modify_time` | `TIMESTAMP` | NO | `CURRENT_TIMESTAMP` | 修改时间 |
| 18 | `modify_by` | `VARCHAR(64)` | YES | `NULL` | 修改者 |
| 19 | `row_version` | `INT` | YES | `1` | 记录版本 |
| 20 | `row_valid` | `TINYINT` | YES | `1` | 记录是否有效 |

枚举 `challenge_type`：`sms`, `email`

枚举 `challenge_scene`：`new_device`, `geo_change`, `high_risk`

状态枚举 `challenge_status`：

| 值 | 含义 |
|----|------|
| 0 | 待验证 |
| 1 | 通过 |
| 2 | 失败 |
| 3 | 过期 |
| 4 | 取消 |

索引策略：

| 索引名 | 类型 | 字段 |
|--------|------|------|
| `PRIMARY` | 主键 | `(id)` |
| `uk_challenge_id` | 唯一索引 | `(challenge_id)` |
| `idx_user_status` | 普通索引 | `(user_id, challenge_status)` |
| `idx_expire_time` | 普通索引 | `(expire_time)` |

---

### 13. ciam_risk_event — 风险事件表

用途：风险识别与处置事件。

| # | 字段名 | 类型 | 可空 | 默认值 | 注释 |
|---|--------|------|------|--------|------|
| 1 | `id` | `BIGINT` | NO | AUTO_INCREMENT | 主键 |
| 2 | `risk_event_id` | `VARCHAR(64)` | NO | — | 风险事件业务唯一标识 |
| 3 | `user_id` | `VARCHAR(64)` | YES | `NULL` | 用户业务唯一标识 |
| 4 | `session_id` | `VARCHAR(64)` | YES | `NULL` | 会话业务唯一标识 |
| 5 | `device_id` | `VARCHAR(64)` | YES | `NULL` | 设备业务唯一标识 |
| 6 | `risk_scene` | `VARCHAR(32)` | NO | — | 风险场景 |
| 7 | `risk_type` | `VARCHAR(32)` | NO | — | 风险类型 |
| 8 | `risk_level` | `TINYINT` | NO | `0` | 风险等级 |
| 9 | `client_type` | `VARCHAR(32)` | YES | `NULL` | 客户端类型 |
| 10 | `ip_address` | `VARCHAR(64)` | YES | `NULL` | IP地址 |
| 11 | `region_code` | `VARCHAR(32)` | YES | `NULL` | 地区编码 |
| 12 | `decision_result` | `VARCHAR(32)` | NO | — | 处置结果 |
| 13 | `hit_rules` | `VARCHAR(1024)` | YES | `NULL` | 命中规则列表 |
| 14 | `event_time` | `TIMESTAMP` | NO | `CURRENT_TIMESTAMP` | 事件时间 |
| 15 | `handled_flag` | `TINYINT` | NO | `0` | 是否已处理 |
| 16 | `description` | `VARCHAR(255)` | YES | `NULL` | 备注 |
| 17 | `create_time` | `TIMESTAMP` | NO | `CURRENT_TIMESTAMP` | 创建时间 |
| 18 | `create_by` | `VARCHAR(64)` | YES | `NULL` | 创建者 |
| 19 | `modify_time` | `TIMESTAMP` | NO | `CURRENT_TIMESTAMP` | 修改时间 |
| 20 | `modify_by` | `VARCHAR(64)` | YES | `NULL` | 修改者 |
| 21 | `row_version` | `INT` | YES | `1` | 记录版本 |
| 22 | `row_valid` | `TINYINT` | YES | `1` | 记录是否有效 |

枚举 `risk_level`：

| 值 | 含义 |
|----|------|
| 0 | 低 |
| 1 | 中 |
| 2 | 高 |

枚举 `decision_result`：`allow`, `challenge`, `block`, `kickout`

标志位 `handled_flag`：0-否，1-是

索引策略：

| 索引名 | 类型 | 字段 |
|--------|------|------|
| `PRIMARY` | 主键 | `(id)` |
| `uk_risk_event_id` | 唯一索引 | `(risk_event_id)` |
| `idx_user_event_time` | 普通索引 | `(user_id, event_time)` |
| `idx_risk_level` | 普通索引 | `(risk_level)` |
| `idx_decision_result` | 普通索引 | `(decision_result)` |

---

### 14. ciam_audit_log — 审计日志表

用途：关键审计日志，支持后续同步至 Elasticsearch。

| # | 字段名 | 类型 | 可空 | 默认值 | 注释 |
|---|--------|------|------|--------|------|
| 1 | `id` | `BIGINT` | NO | AUTO_INCREMENT | 主键 |
| 2 | `audit_id` | `VARCHAR(64)` | NO | — | 审计日志业务唯一标识 |
| 3 | `user_id` | `VARCHAR(64)` | YES | `NULL` | 用户业务唯一标识 |
| 4 | `session_id` | `VARCHAR(64)` | YES | `NULL` | 会话业务唯一标识 |
| 5 | `client_id` | `VARCHAR(64)` | YES | `NULL` | 客户端标识 |
| 6 | `client_type` | `VARCHAR(32)` | YES | `NULL` | 客户端类型 |
| 7 | `event_type` | `VARCHAR(32)` | NO | — | 事件类型 |
| 8 | `event_name` | `VARCHAR(64)` | NO | — | 事件名称 |
| 9 | `operation_result` | `TINYINT` | NO | — | 操作结果 |
| 10 | `request_uri` | `VARCHAR(255)` | YES | `NULL` | 请求URI |
| 11 | `request_method` | `VARCHAR(16)` | YES | `NULL` | 请求方法 |
| 12 | `response_code` | `VARCHAR(32)` | YES | `NULL` | 响应码 |
| 13 | `ip_address` | `VARCHAR(64)` | YES | `NULL` | 请求IP |
| 14 | `trace_id` | `VARCHAR(64)` | YES | `NULL` | 追踪标识 |
| 15 | `request_snapshot` | `TEXT` | YES | `NULL` | 请求快照 |
| 16 | `event_time` | `TIMESTAMP` | NO | `CURRENT_TIMESTAMP` | 事件时间 |
| 17 | `description` | `VARCHAR(255)` | YES | `NULL` | 备注 |
| 18 | `create_time` | `TIMESTAMP` | NO | `CURRENT_TIMESTAMP` | 创建时间 |
| 19 | `create_by` | `VARCHAR(64)` | YES | `NULL` | 创建者 |
| 20 | `modify_time` | `TIMESTAMP` | NO | `CURRENT_TIMESTAMP` | 修改时间 |
| 21 | `modify_by` | `VARCHAR(64)` | YES | `NULL` | 修改者 |
| 22 | `row_version` | `INT` | YES | `1` | 记录版本 |
| 23 | `row_valid` | `TINYINT` | YES | `1` | 记录是否有效 |

状态枚举 `operation_result`：

| 值 | 含义 |
|----|------|
| 1 | 成功 |
| 0 | 失败 |

索引策略：

| 索引名 | 类型 | 字段 |
|--------|------|------|
| `PRIMARY` | 主键 | `(id)` |
| `uk_audit_id` | 唯一索引 | `(audit_id)` |
| `idx_user_event_time` | 普通索引 | `(user_id, event_time)` |
| `idx_event_type_time` | 普通索引 | `(event_type, event_time)` |
| `idx_trace_id` | 普通索引 | `(trace_id)` |

---

### 15. ciam_merge_request — 账号合并申请表

用途：账号合并申请与审核记录。

| # | 字段名 | 类型 | 可空 | 默认值 | 注释 |
|---|--------|------|------|--------|------|
| 1 | `id` | `BIGINT` | NO | AUTO_INCREMENT | 主键 |
| 2 | `merge_request_id` | `VARCHAR(64)` | NO | — | 合并申请业务唯一标识 |
| 3 | `source_user_id` | `VARCHAR(64)` | NO | — | 源账号用户业务标识 |
| 4 | `target_user_id` | `VARCHAR(64)` | NO | — | 目标账号用户业务标识 |
| 5 | `conflict_identity_type` | `VARCHAR(32)` | NO | — | 冲突标识类型 |
| 6 | `conflict_identity_hash` | `CHAR(64)` | NO | — | 冲突标识哈希值 |
| 7 | `apply_source` | `VARCHAR(32)` | YES | `NULL` | 申请来源 |
| 8 | `review_status` | `TINYINT` | NO | `0` | 审核状态 |
| 9 | `reviewer` | `VARCHAR(64)` | YES | `NULL` | 审核人 |
| 10 | `review_time` | `TIMESTAMP` | YES | `NULL` | 审核时间 |
| 11 | `final_user_id` | `VARCHAR(64)` | YES | `NULL` | 最终保留用户业务标识 |
| 12 | `finish_time` | `TIMESTAMP` | YES | `NULL` | 完成时间 |
| 13 | `description` | `VARCHAR(255)` | YES | `NULL` | 备注 |
| 14 | `create_time` | `TIMESTAMP` | NO | `CURRENT_TIMESTAMP` | 创建时间 |
| 15 | `create_by` | `VARCHAR(64)` | YES | `NULL` | 创建者 |
| 16 | `modify_time` | `TIMESTAMP` | NO | `CURRENT_TIMESTAMP` | 修改时间 |
| 17 | `modify_by` | `VARCHAR(64)` | YES | `NULL` | 修改者 |
| 18 | `row_version` | `INT` | YES | `1` | 记录版本 |
| 19 | `row_valid` | `TINYINT` | YES | `1` | 记录是否有效 |

状态枚举 `review_status`（合并）：

| 值 | 含义 |
|----|------|
| 0 | 待审 |
| 1 | 通过 |
| 2 | 驳回 |
| 3 | 取消 |

索引策略：

| 索引名 | 类型 | 字段 |
|--------|------|------|
| `PRIMARY` | 主键 | `(id)` |
| `uk_merge_request_id` | 唯一索引 | `(merge_request_id)` |
| `idx_source_user` | 普通索引 | `(source_user_id)` |
| `idx_target_user` | 普通索引 | `(target_user_id)` |
| `idx_review_status` | 普通索引 | `(review_status)` |

---

### 16. ciam_deactivation_request — 注销申请表

用途：账号注销申请、审核与执行记录。

| # | 字段名 | 类型 | 可空 | 默认值 | 注释 |
|---|--------|------|------|--------|------|
| 1 | `id` | `BIGINT` | NO | AUTO_INCREMENT | 主键 |
| 2 | `deactivation_request_id` | `VARCHAR(64)` | NO | — | 注销申请业务唯一标识 |
| 3 | `user_id` | `VARCHAR(64)` | NO | — | 用户业务唯一标识 |
| 4 | `request_source` | `VARCHAR(32)` | YES | `NULL` | 申请来源 |
| 5 | `request_reason` | `VARCHAR(255)` | YES | `NULL` | 申请原因 |
| 6 | `check_status` | `TINYINT` | NO | `0` | 校验状态 |
| 7 | `review_status` | `TINYINT` | NO | `0` | 审核状态 |
| 8 | `execute_status` | `TINYINT` | NO | `0` | 执行状态 |
| 9 | `requested_time` | `TIMESTAMP` | NO | `CURRENT_TIMESTAMP` | 申请时间 |
| 10 | `reviewer` | `VARCHAR(64)` | YES | `NULL` | 审核人 |
| 11 | `review_time` | `TIMESTAMP` | YES | `NULL` | 审核时间 |
| 12 | `execute_time` | `TIMESTAMP` | YES | `NULL` | 执行时间 |
| 13 | `retain_audit_only` | `TINYINT` | NO | `1` | 是否仅保留脱敏审计凭证 |
| 14 | `description` | `VARCHAR(255)` | YES | `NULL` | 备注 |
| 15 | `create_time` | `TIMESTAMP` | NO | `CURRENT_TIMESTAMP` | 创建时间 |
| 16 | `create_by` | `VARCHAR(64)` | YES | `NULL` | 创建者 |
| 17 | `modify_time` | `TIMESTAMP` | NO | `CURRENT_TIMESTAMP` | 修改时间 |
| 18 | `modify_by` | `VARCHAR(64)` | YES | `NULL` | 修改者 |
| 19 | `row_version` | `INT` | YES | `1` | 记录版本 |
| 20 | `row_valid` | `TINYINT` | YES | `1` | 记录是否有效 |

状态枚举 `check_status`：

| 值 | 含义 |
|----|------|
| 0 | 待校验 |
| 1 | 通过 |
| 2 | 不通过 |

状态枚举 `review_status`（注销）：

| 值 | 含义 |
|----|------|
| 0 | 待审 |
| 1 | 通过 |
| 2 | 驳回 |

状态枚举 `execute_status`：

| 值 | 含义 |
|----|------|
| 0 | 待执行 |
| 1 | 已执行 |
| 2 | 失败 |

标志位 `retain_audit_only`：0-否，1-是

索引策略：

| 索引名 | 类型 | 字段 |
|--------|------|------|
| `PRIMARY` | 主键 | `(id)` |
| `uk_deactivation_request_id` | 唯一索引 | `(deactivation_request_id)` |
| `idx_user_review_status` | 普通索引 | `(user_id, review_status)` |
| `idx_execute_status` | 普通索引 | `(execute_status)` |

---

### 17. ciam_invitation_relation — 邀请关系表

用途：邀请关系与渠道归因留痕。

| # | 字段名 | 类型 | 可空 | 默认值 | 注释 |
|---|--------|------|------|--------|------|
| 1 | `id` | `BIGINT` | NO | AUTO_INCREMENT | 主键 |
| 2 | `relation_id` | `VARCHAR(64)` | NO | — | 邀请关系业务唯一标识 |
| 3 | `inviter_user_id` | `VARCHAR(64)` | YES | `NULL` | 邀请人用户业务标识 |
| 4 | `invitee_user_id` | `VARCHAR(64)` | NO | — | 被邀请人用户业务标识 |
| 5 | `invite_code` | `VARCHAR(64)` | YES | `NULL` | 邀请码 |
| 6 | `invite_channel_code` | `VARCHAR(64)` | YES | `NULL` | 渠道码 |
| 7 | `invite_activity_code` | `VARCHAR(64)` | YES | `NULL` | 活动码 |
| 8 | `relation_lock_flag` | `TINYINT` | NO | `1` | 关系是否固化 |
| 9 | `register_time` | `TIMESTAMP` | NO | `CURRENT_TIMESTAMP` | 注册成功时间 |
| 10 | `description` | `VARCHAR(255)` | YES | `NULL` | 备注 |
| 11 | `create_time` | `TIMESTAMP` | NO | `CURRENT_TIMESTAMP` | 创建时间 |
| 12 | `create_by` | `VARCHAR(64)` | YES | `NULL` | 创建者 |
| 13 | `modify_time` | `TIMESTAMP` | NO | `CURRENT_TIMESTAMP` | 修改时间 |
| 14 | `modify_by` | `VARCHAR(64)` | YES | `NULL` | 修改者 |
| 15 | `row_version` | `INT` | YES | `1` | 记录版本 |
| 16 | `row_valid` | `TINYINT` | YES | `1` | 记录是否有效 |

标志位 `relation_lock_flag`：0-否，1-是

索引策略：

| 索引名 | 类型 | 字段 |
|--------|------|------|
| `PRIMARY` | 主键 | `(id)` |
| `uk_relation_id` | 唯一索引 | `(relation_id)` |
| `idx_inviter_user_id` | 普通索引 | `(inviter_user_id)` |
| `idx_invitee_user_id` | 普通索引 | `(invitee_user_id)` |
| `idx_invite_channel_code` | 普通索引 | `(invite_channel_code)` |

---

### 18. ciam_owner_cert_state — 车主认证状态表

用途：车主认证状态承接表。

| # | 字段名 | 类型 | 可空 | 默认值 | 注释 |
|---|--------|------|------|--------|------|
| 1 | `id` | `BIGINT` | NO | AUTO_INCREMENT | 主键 |
| 2 | `owner_cert_id` | `VARCHAR(64)` | NO | — | 车主认证记录业务唯一标识 |
| 3 | `user_id` | `VARCHAR(64)` | NO | — | 用户业务唯一标识 |
| 4 | `vehicle_id` | `VARCHAR(64)` | YES | `NULL` | 车辆业务唯一标识 |
| 5 | `vin` | `VARCHAR(32)` | YES | `NULL` | 车架号 |
| 6 | `cert_status` | `TINYINT` | NO | `0` | 认证状态 |
| 7 | `cert_source` | `VARCHAR(32)` | YES | `NULL` | 认证来源系统 |
| 8 | `callback_time` | `TIMESTAMP` | YES | `NULL` | 回调时间 |
| 9 | `last_query_time` | `TIMESTAMP` | YES | `NULL` | 补偿查询时间 |
| 10 | `effective_time` | `TIMESTAMP` | YES | `NULL` | 生效时间 |
| 11 | `expire_time` | `TIMESTAMP` | YES | `NULL` | 失效时间 |
| 12 | `result_message` | `VARCHAR(255)` | YES | `NULL` | 认证结果说明 |
| 13 | `description` | `VARCHAR(255)` | YES | `NULL` | 备注 |
| 14 | `create_time` | `TIMESTAMP` | NO | `CURRENT_TIMESTAMP` | 创建时间 |
| 15 | `create_by` | `VARCHAR(64)` | YES | `NULL` | 创建者 |
| 16 | `modify_time` | `TIMESTAMP` | NO | `CURRENT_TIMESTAMP` | 修改时间 |
| 17 | `modify_by` | `VARCHAR(64)` | YES | `NULL` | 修改者 |
| 18 | `row_version` | `INT` | YES | `1` | 记录版本 |
| 19 | `row_valid` | `TINYINT` | YES | `1` | 记录是否有效 |

状态枚举 `cert_status`：

| 值 | 含义 |
|----|------|
| 0 | 未认证 |
| 1 | 认证中 |
| 2 | 已认证 |
| 3 | 认证失败 |
| 4 | 已失效 |

索引策略：

| 索引名 | 类型 | 字段 |
|--------|------|------|
| `PRIMARY` | 主键 | `(id)` |
| `uk_owner_cert_id` | 唯一索引 | `(owner_cert_id)` |
| `idx_user_cert_status` | 普通索引 | `(user_id, cert_status)` |
| `idx_vehicle_id` | 普通索引 | `(vehicle_id)` |
| `idx_vin` | 普通索引 | `(vin)` |

---

## 附录：状态枚举汇总

| 表名 | 字段 | 枚举值 |
|------|------|--------|
| `ciam_user` | `user_status` | 0-待验证, 1-正常, 2-已锁定, 3-已禁用, 4-注销处理中, 5-已注销 |
| `ciam_user_identity` | `identity_status` | 1-已绑定, 0-已解绑 |
| `ciam_user_identity` | `identity_type` | mobile, email, wechat, apple, google, local_mobile |
| `ciam_user_credential` | `credential_status` | 1-有效, 0-失效 |
| `ciam_user_credential` | `credential_type` | email_password |
| `ciam_user_profile` | `gender` | 0-未知, 1-男, 2-女 |
| `ciam_user_tag` | `tag_status` | 1-生效, 0-失效 |
| `ciam_user_tag` | `tag_code` | real_name_verified, owner_verified |
| `ciam_user_consent` | `consent_status` | 1-同意, 0-撤回 |
| `ciam_user_consent` | `consent_type` | user_agreement, privacy_policy, marketing |
| `ciam_session` | `session_status` | 1-有效, 0-失效, 2-下线, 3-过期 |
| `ciam_session` | `risk_level` | 0-低, 1-中, 2-高 |
| `ciam_session` | `client_type` | app, mini_program, web, vehicle, admin |
| `ciam_device` | `device_status` | 1-正常, 0-失效 |
| `ciam_oauth_client` | `client_status` | 1-启用, 0-停用 |
| `ciam_oauth_client` | `client_type` | public, confidential, internal |
| `ciam_refresh_token` | `token_status` | 1-有效, 2-已轮换, 3-已撤销, 4-已过期 |
| `ciam_mfa_challenge` | `challenge_status` | 0-待验证, 1-通过, 2-失败, 3-过期, 4-取消 |
| `ciam_mfa_challenge` | `challenge_type` | sms, email |
| `ciam_mfa_challenge` | `challenge_scene` | new_device, geo_change, high_risk |
| `ciam_risk_event` | `risk_level` | 0-低, 1-中, 2-高 |
| `ciam_risk_event` | `decision_result` | allow, challenge, block, kickout |
| `ciam_audit_log` | `operation_result` | 1-成功, 0-失败 |
| `ciam_merge_request` | `review_status` | 0-待审, 1-通过, 2-驳回, 3-取消 |
| `ciam_deactivation_request` | `check_status` | 0-待校验, 1-通过, 2-不通过 |
| `ciam_deactivation_request` | `review_status` | 0-待审, 1-通过, 2-驳回 |
| `ciam_deactivation_request` | `execute_status` | 0-待执行, 1-已执行, 2-失败 |
| `ciam_owner_cert_state` | `cert_status` | 0-未认证, 1-认证中, 2-已认证, 3-认证失败, 4-已失效 |

## 附录：标志位汇总

| 表名 | 字段 | 取值 |
|------|------|------|
| `ciam_user_identity` | `verified_flag` | 0-否, 1-是 |
| `ciam_user_identity` | `primary_flag` | 0-否, 1-是 |
| `ciam_device` | `trusted_flag` | 0-否, 1-是 |
| `ciam_oauth_client` | `pkce_required` | 0-否, 1-是 |
| `ciam_auth_code` | `used_flag` | 0-否, 1-是 |
| `ciam_risk_event` | `handled_flag` | 0-否, 1-是 |
| `ciam_invitation_relation` | `relation_lock_flag` | 0-否, 1-是 |
| `ciam_deactivation_request` | `retain_audit_only` | 0-否, 1-是 |
| 所有表 | `row_valid` | 1-有效, 0-无效 |

## 附录：双主键策略一览

| 表名 | 物理主键 | 业务主键 |
|------|----------|----------|
| `ciam_user` | `id` | `user_id` |
| `ciam_user_identity` | `id` | `identity_id` |
| `ciam_user_credential` | `id` | `credential_id` |
| `ciam_user_profile` | `id` | `profile_id` |
| `ciam_user_tag` | `id` | `tag_id` |
| `ciam_user_consent` | `id` | `consent_id` |
| `ciam_session` | `id` | `session_id` |
| `ciam_device` | `id` | `device_id` |
| `ciam_oauth_client` | `id` | `client_id` |
| `ciam_auth_code` | `id` | `auth_code_id` |
| `ciam_refresh_token` | `id` | `refresh_token_id` |
| `ciam_mfa_challenge` | `id` | `challenge_id` |
| `ciam_risk_event` | `id` | `risk_event_id` |
| `ciam_audit_log` | `id` | `audit_id` |
| `ciam_merge_request` | `id` | `merge_request_id` |
| `ciam_deactivation_request` | `id` | `deactivation_request_id` |
| `ciam_invitation_relation` | `id` | `relation_id` |
| `ciam_owner_cert_state` | `id` | `owner_cert_id` |

---

## 数据字典

本节汇总所有数据库字段的枚举取值与 Java 枚举类的映射关系，确保代码、DDL、接口文档之间的状态语义一致。

Java 枚举包路径：`net.hwyz.iov.cloud.sec.ciam.domain.enums`

### 数值编码枚举（TINYINT → CodeEnum）

| Java 枚举类 | 对应表.字段 | 编码 → 含义 |
|-------------|------------|-------------|
| `UserStatus` | `ciam_user.user_status` | 0-待验证, 1-正常, 2-已锁定, 3-已禁用, 4-注销处理中, 5-已注销 |
| `IdentityStatus` | `ciam_user_identity.identity_status` | 0-已解绑, 1-已绑定 |
| `CredentialStatus` | `ciam_user_credential.credential_status` | 0-失效, 1-有效 |
| `Gender` | `ciam_user_profile.gender` | 0-未知, 1-男, 2-女 |
| `TagStatus` | `ciam_user_tag.tag_status` | 0-失效, 1-生效 |
| `ConsentStatus` | `ciam_user_consent.consent_status` | 0-撤回, 1-同意 |
| `SessionStatus` | `ciam_session.session_status` | 0-失效, 1-有效, 2-下线, 3-过期 |
| `RiskLevel` | `ciam_session.risk_level`、`ciam_risk_event.risk_level` | 0-低, 1-中, 2-高 |
| `DeviceStatus` | `ciam_device.device_status` | 0-失效, 1-正常 |
| `ClientStatus` | `ciam_oauth_client.client_status` | 0-停用, 1-启用 |
| `TokenStatus` | `ciam_refresh_token.token_status` | 1-有效, 2-已轮换, 3-已撤销, 4-已过期 |
| `ChallengeStatus` | `ciam_mfa_challenge.challenge_status` | 0-待验证, 1-通过, 2-失败, 3-过期, 4-取消 |
| `OperationResult` | `ciam_audit_log.operation_result` | 0-失败, 1-成功 |
| `ReviewStatus` | `ciam_merge_request.review_status`、`ciam_deactivation_request.review_status` | 0-待审, 1-通过, 2-驳回, 3-取消（注销审核仅用 0/1/2） |
| `CheckStatus` | `ciam_deactivation_request.check_status` | 0-待校验, 1-通过, 2-不通过 |
| `ExecuteStatus` | `ciam_deactivation_request.execute_status` | 0-待执行, 1-已执行, 2-失败 |
| `CertStatus` | `ciam_owner_cert_state.cert_status` | 0-未认证, 1-认证中, 2-已认证, 3-认证失败, 4-已失效 |

### 字符串标签枚举（VARCHAR → LabelEnum）

| Java 枚举类 | 对应表.字段 | 取值 → 含义 |
|-------------|------------|-------------|
| `IdentityType` | `ciam_user_identity.identity_type`、`ciam_user.register_source` | mobile-手机号, email-邮箱, wechat-微信, apple-Apple, google-Google, local_mobile-本机手机号 |
| `CredentialType` | `ciam_user_credential.credential_type` | email_password-邮箱密码 |
| `ConsentType` | `ciam_user_consent.consent_type` | user_agreement-用户协议, privacy_policy-隐私政策, marketing-营销同意 |
| `ClientType` | `ciam_session.client_type`、`ciam_device.client_type` 等 | app-手机App, mini_program-小程序, web-官网, vehicle-车机, admin-运营后台 |
| `OAuthClientType` | `ciam_oauth_client.client_type` | public-公开客户端, confidential-机密客户端, internal-内部客户端 |
| `ChallengeType` | `ciam_mfa_challenge.challenge_type` | sms-短信验证码, email-邮箱验证码 |
| `ChallengeScene` | `ciam_mfa_challenge.challenge_scene` | new_device-新设备登录, geo_change-异地登录, high_risk-高风险操作 |
| `DecisionResult` | `ciam_risk_event.decision_result` | allow-放行, challenge-挑战, block-阻断, kickout-强制下线 |

### 标志位字段（TINYINT 0/1）

标志位字段未单独建立枚举类，统一约定 `0-否 / 1-是`：

| 对应表.字段 | 含义 |
|------------|------|
| `ciam_user_identity.verified_flag` | 是否已验证 |
| `ciam_user_identity.primary_flag` | 是否主标识 |
| `ciam_device.trusted_flag` | 是否受信任设备 |
| `ciam_oauth_client.pkce_required` | 是否强制PKCE |
| `ciam_auth_code.used_flag` | 是否已使用 |
| `ciam_risk_event.handled_flag` | 是否已处理 |
| `ciam_invitation_relation.relation_lock_flag` | 关系是否固化 |
| `ciam_deactivation_request.retain_audit_only` | 是否仅保留脱敏审计凭证 |
| 所有表 `row_valid` | 记录是否有效 |

### 通用接口说明

所有数值编码枚举实现 `CodeEnum` 接口，提供：
- `getCode()` — 返回数据库 TINYINT 值
- `getDescription()` — 返回中文描述
- `fromCode(int)` — 根据编码反查枚举，非法值抛出 `IllegalArgumentException`

所有字符串标签枚举实现 `LabelEnum` 接口，提供：
- `getValue()` — 返回数据库 VARCHAR 值
- `getDescription()` — 返回中文描述
- `fromValue(String)` — 根据字符串反查枚举，非法值抛出 `IllegalArgumentException`
