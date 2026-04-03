package net.hwyz.iov.cloud.sec.ciam.infrastructure.search.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户检索索引文档。
 * <p>
 * 对应 Elasticsearch 中的用户索引，字段从 MySQL ciam_user 表异步同步构建。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSearchDocument {

    /** 用户业务唯一标识 */
    private String userId;

    /** 用户状态：0-待验证，1-正常，2-已锁定，3-已禁用，4-注销处理中，5-已注销 */
    private Integer userStatus;

    /** 注册来源 */
    private String registerSource;

    /** 注册渠道 */
    private String registerChannel;

    /** 最后登录时间 */
    private LocalDateTime lastLoginTime;

    /** 创建时间 */
    private LocalDateTime createTime;
}
