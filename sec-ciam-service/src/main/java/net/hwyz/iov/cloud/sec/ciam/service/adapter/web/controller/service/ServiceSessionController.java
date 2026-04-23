package net.hwyz.iov.cloud.sec.ciam.service.adapter.web.controller.service;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.framework.web.controller.BaseController;
import net.hwyz.iov.cloud.sec.ciam.api.service.CiamSessionService;
import net.hwyz.iov.cloud.sec.ciam.api.vo.SessionValidateResult;
import net.hwyz.iov.cloud.sec.ciam.service.application.service.SessionAppService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 服务接口 — 会话校验。
 */
@RestController
@RequestMapping("/api/service/session/v1")
@RequiredArgsConstructor
public class ServiceSessionController extends BaseController implements CiamSessionService {

    private final SessionAppService sessionAppService;

    @Override
    public SessionValidateResult validateSession(@RequestParam("sessionId") String sessionId) {
        return sessionAppService.validateSession(sessionId);
    }
}
