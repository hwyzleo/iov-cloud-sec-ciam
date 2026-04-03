package net.hwyz.iov.cloud.sec.ciam.controller.service;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.sec.ciam.api.service.CiamSessionService;
import net.hwyz.iov.cloud.sec.ciam.api.vo.SessionValidateResult;
import net.hwyz.iov.cloud.sec.ciam.domain.service.SessionDomainService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 服务接口 — 会话校验。
 */
@RestController
@RequestMapping("/api/service/v1")
@RequiredArgsConstructor
public class ServiceSessionController implements CiamSessionService {

    private final SessionDomainService sessionDomainService;

    @Override
    public SessionValidateResult validateSession(@RequestParam("sessionId") String sessionId) {
        var session = sessionDomainService.findBySessionId(sessionId);
        return SessionValidateResult.builder()
                .valid(session.isPresent())
                .sessionId(sessionId)
                .userId(session.map(s -> s.getUserId()).orElse(null))
                .build();
    }
}
