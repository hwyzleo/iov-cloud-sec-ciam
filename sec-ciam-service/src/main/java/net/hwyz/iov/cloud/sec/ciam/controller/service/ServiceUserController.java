package net.hwyz.iov.cloud.sec.ciam.controller.service;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.sec.ciam.api.service.CiamUserService;
import net.hwyz.iov.cloud.sec.ciam.api.vo.UserBasicInfo;
import net.hwyz.iov.cloud.sec.ciam.domain.service.TagDomainService;
import net.hwyz.iov.cloud.sec.ciam.domain.service.UserDomainService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 服务接口 — 用户信息查询。
 */
@RestController
@RequestMapping("/api/service/v1")
@RequiredArgsConstructor
public class ServiceUserController implements CiamUserService {

    private final UserDomainService userDomainService;
    private final TagDomainService tagDomainService;

    @Override
    public UserBasicInfo getUserInfo(@RequestParam("userId") String userId) {
        var user = userDomainService.findByUserId(userId);
        if (user.isEmpty()) {
            return null;
        }
        var u = user.get();
        var tags = tagDomainService.getActiveTags(userId);
        return UserBasicInfo.builder()
                .userId(u.getUserId())
                .userStatus(u.getUserStatus())
                .nickname(null)
                .tags(tags.stream().map(t -> t.getTagCode()).toList())
                .build();
    }
}
