package net.hwyz.iov.cloud.sec.ciam.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.sec.ciam.service.application.assembler.RiskEventAssembler;
import net.hwyz.iov.cloud.sec.ciam.service.application.dto.RiskEventDto;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.RiskEventRepository;
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

    private final RiskEventRepository riskEventRepository;

    /**
     * 查询用户在指定时间范围内的风险事件
     */
    public List<RiskEventDto> queryUserRiskEvents(String userId, LocalDateTime startTime, LocalDateTime endTime) {
        log.info("查询用户风险事件: userId={}, startTime={}, endTime={}", userId, startTime, endTime);
        return riskEventRepository.findByUserIdAndTimeRange(userId, startTime, endTime).stream()
                .map(doObj -> RiskEventAssembler.INSTANCE.toDto(RiskEventAssembler.INSTANCE.toDomain(doObj)))
                .collect(Collectors.toList());
    }
}
