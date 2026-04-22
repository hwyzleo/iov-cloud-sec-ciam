package net.hwyz.iov.cloud.sec.ciam.service.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.Map;

/**
 * 统计分析结果 DTO。
 * <p>
 * 承载注册转化、登录成功率、来源渠道分布、第三方登录占比等统计指标。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatisticsResultDto {

    /** 注册总数 */
    private long registrationCount;

    /** 注册转化率（0.0 ~ 1.0） */
    private double conversionRate;

    /** 登录成功次数 */
    private long loginSuccessCount;

    /** 登录失败次数 */
    private long loginFailureCount;

    /** 登录成功率（0.0 ~ 1.0） */
    private double loginSuccessRate;

    /** 渠道分布：渠道名称 → 注册数量 */
    private Map<String, Long> channelDistribution;

    /** 第三方登录分布：登录类型 → 登录数量 */
    private Map<String, Long> thirdPartyDistribution;

    /**
     * 构建空统计结果。
     */
    public static StatisticsResultDto empty() {
        return StatisticsResultDto.builder()
                .registrationCount(0)
                .conversionRate(0.0)
                .loginSuccessCount(0)
                .loginFailureCount(0)
                .loginSuccessRate(0.0)
                .channelDistribution(Collections.emptyMap())
                .thirdPartyDistribution(Collections.emptyMap())
                .build();
    }
}
