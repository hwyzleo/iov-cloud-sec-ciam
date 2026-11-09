package net.hwyz.iov.cloud.sec.ciam.service.application.service;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.sec.ciam.api.vo.SessionValidateResult;
import net.hwyz.iov.cloud.sec.ciam.service.application.assembler.DeviceAssembler;
import net.hwyz.iov.cloud.sec.ciam.service.application.assembler.SessionAssembler;
import net.hwyz.iov.cloud.sec.ciam.service.application.dto.DeviceInfoDto;
import net.hwyz.iov.cloud.sec.ciam.service.application.dto.SessionDto;
import net.hwyz.iov.cloud.sec.ciam.service.domain.enums.SessionStatus;
import net.hwyz.iov.cloud.sec.ciam.service.domain.model.Session;
import net.hwyz.iov.cloud.sec.ciam.service.domain.service.SessionDomainService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 会话应用服务 — 编排会话校验与下线逻辑。
 */
@Service
@RequiredArgsConstructor
public class SessionAppService {

    private final SessionDomainService sessionDomainService;

    /**
     * 查询用户活跃会话
     */
    public List<SessionDto> findUserSessions(String userId) {
        return sessionDomainService.findUserSessions(userId).stream()
                .map(SessionAssembler.INSTANCE::toDto)
                .collect(Collectors.toList());
    }

    /**
     * 查询用户活跃设备
     */
    public List<DeviceInfoDto> findUserDevices(String userId) {
        return sessionDomainService.findUserDevices(userId).stream()
                .map(DeviceAssembler.INSTANCE::toDto)
                .collect(Collectors.toList());
    }

    /**
     * 强制下线会话
     */
    public void kickSession(String sessionId, String userId) {
        sessionDomainService.kickSession(sessionId, userId);
    }

    /**
     * 强制下线设备
     */
    public void kickDevice(String deviceId, String userId) {
        sessionDomainService.kickDevice(deviceId, userId);
    }

    /**
     * 校验会话有效性。
...
     * @param sessionId 会话 ID
     * @return 校验结果
     */
    public SessionValidateResult validateSession(String sessionId) {
        Optional<Session> sessionOpt = sessionDomainService.findBySessionId(sessionId);
        if (sessionOpt.isEmpty()) {
            return SessionValidateResult.builder()
                    .valid(false)
                    .sessionId(sessionId)
                    .build();
        }

        Session session = sessionOpt.get();
        // SEC-CIAM-DSN-CR-003 §5.4：不能只看 ACTIVE 状态，必须同时校验绝对到期时间
        boolean active = session.getSessionStatus() != null
                && session.getSessionStatus() == SessionStatus.ACTIVE.getCode();
        boolean notExpired = session.getExpireTime() != null
                && net.hwyz.iov.cloud.framework.common.util.DateTimeUtil.getNowInstant()
                .isBefore(session.getExpireTime());
        boolean isValid = active && notExpired;

        return SessionValidateResult.builder()
                .valid(isValid)
                .sessionId(sessionId)
                .userId(session.getUserId())
                .clientId(session.getClientId())
                .build();
    }
}
