package net.hwyz.iov.cloud.sec.ciam.service.adapter.web.controller.service;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.sec.ciam.api.service.CiamUserService;
import net.hwyz.iov.cloud.sec.ciam.api.vo.UserBasicInfo;
import net.hwyz.iov.cloud.sec.ciam.service.application.service.AccountQueryAppService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 服务接口 — 用户信息查询。
 */
@RestController
@RequestMapping("/api/service/user/v1")
@RequiredArgsConstructor
public class ServiceUserController implements CiamUserService {

    private final AccountQueryAppService accountQueryAppService;

    @Override
    public UserBasicInfo getUserInfo(@RequestParam("userId") String userId) {
        return accountQueryAppService.getUserBasicInfo(userId);
    }
}

