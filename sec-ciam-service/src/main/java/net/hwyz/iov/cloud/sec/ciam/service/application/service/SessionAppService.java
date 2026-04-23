package net.hwyz.iov.cloud.sec.ciam.service.application.service;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.sec.ciam.api.vo.SessionValidateResult;
import net.hwyz.iov.cloud.sec.ciam.service.domain.enums.SessionStatus;
import net.hwyz.iov.cloud.sec.ciam.service.domain.model.Session;
import net.hwyz.iov.cloud.sec.ciam.service.domain.service.SessionDomainService;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 会话应用服务 — 编排会话校验与下线逻辑。
 */
@Service
@RequiredArgsConstructor
public class SessionAppService {

    private final SessionDomainService sessionDomainService;

    /**
     * 校验会话有效性。
     *
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
        boolean isValid = session.getSessionStatus() == SessionStatus.ACTIVE.getCode();

        return SessionValidateResult.builder()
                .valid(isValid)
                .sessionId(sessionId)
                .userId(session.getUserId())
                .clientId(session.getClientId())
                .build();
    }
}
