package net.hwyz.iov.cloud.sec.ciam.infrastructure.search.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 安全事件检索索引文档。
 * <p>
 * 对应 Elasticsearch 中的风险事件索引，可通过 MySQL 或 Kafka 异步写入。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskEventSearchDocument {

    /** 风险事件业务唯一标识 */
    private String riskEventId;

    /** 用户业务唯一标识 */
    private String userId;

    /** 风险场景 */
    private String riskScene;

    /** 风险类型 */
    private String riskType;

    /** 风险等级：0-低，1-中，2-高 */
    private Integer riskLevel;

    /** 处置结果：allow,challenge,block,kickout */
    private String decisionResult;

    /** 事件时间 */
    private LocalDateTime eventTime;
}
