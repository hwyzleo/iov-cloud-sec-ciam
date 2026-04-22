package net.hwyz.iov.cloud.sec.ciam.service.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.sec.ciam.service.application.dto.RiskEventDto;
import net.hwyz.iov.cloud.sec.ciam.service.application.mapper.RiskEventMapper;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.CiamRiskEventRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 风险事件应用服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RiskEventAppService {

    private final CiamRiskEventRepository riskEventRepository;

    /**
     * 查询用户在指定时间范围内的风险事件
     */
    public List<RiskEventDto> queryUserRiskEvents(String userId, LocalDateTime startTime, LocalDateTime endTime) {
        log.info("查询用户风险事件: userId={}, startTime={}, endTime={}", userId, startTime, endTime);
        return riskEventRepository.findByUserIdAndTimeRange(userId, startTime, endTime).stream()
                .map(doObj -> RiskEventMapper.INSTANCE.toDto(RiskEventMapper.INSTANCE.toDomain(doObj)))
                .collect(Collectors.toList());
    }
}
