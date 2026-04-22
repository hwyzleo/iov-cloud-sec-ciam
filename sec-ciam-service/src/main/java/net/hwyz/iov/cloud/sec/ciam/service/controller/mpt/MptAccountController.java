package net.hwyz.iov.cloud.sec.ciam.service.controller.mpt;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.bean.ApiResponse;
import net.hwyz.iov.cloud.framework.common.bean.PageResult;
import net.hwyz.iov.cloud.framework.web.controller.BaseController;
import net.hwyz.iov.cloud.sec.ciam.service.application.AccountBindingAppService;
import net.hwyz.iov.cloud.sec.ciam.service.application.AccountLifecycleAppService;
import net.hwyz.iov.cloud.sec.ciam.service.application.AdminAccountAppService;
import net.hwyz.iov.cloud.sec.ciam.service.application.AccountQueryAppService;
import net.hwyz.iov.cloud.sec.ciam.service.application.StatisticsAppService;
import net.hwyz.iov.cloud.sec.ciam.service.application.dto.StatisticsResultDto;
import net.hwyz.iov.cloud.sec.ciam.service.application.dto.DeactivationRequestDto;
import net.hwyz.iov.cloud.sec.ciam.service.application.dto.MergeRequestDto;
import net.hwyz.iov.cloud.sec.ciam.service.application.mapper.DeactivationRequestMapper;
import net.hwyz.iov.cloud.sec.ciam.service.application.mapper.MergeRequestMapper;
import net.hwyz.iov.cloud.sec.ciam.service.application.mapper.UserIdentityMapper;
import net.hwyz.iov.cloud.sec.ciam.service.controller.mpt.vo.*;
import net.hwyz.iov.cloud.sec.ciam.service.controller.vo.DeactivationRequestVo;
import net.hwyz.iov.cloud.sec.ciam.service.controller.vo.MergeRequestVo;
import net.hwyz.iov.cloud.sec.ciam.service.controller.vo.UserIdentityVo;
import net.hwyz.iov.cloud.sec.ciam.service.domain.query.UserQuery;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.search.document.UserSearchDocument;
import net.hwyz.iov.cloud.framework.security.util.SecurityUtils;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 运营后台管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/mpt/account/v1")
@RequiredArgsConstructor
public class MptAccountController extends BaseController {

    private final AccountQueryAppService adminQueryAppService;
    private final AccountLifecycleAppService accountLifecycleAppService;
    private final AccountBindingAppService accountBindingAppService;
    private final StatisticsAppService statisticsAppService;
    private final AdminAccountAppService adminAccountAppService;

    // ---- 用户查询 ----

    @GetMapping("/accounts")
    public ApiResponse<PageResult<UserSearchDocument>> searchUsers(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String identityType,
            @RequestParam(required = false) String identityValue,
            @RequestParam(required = false) String nickname,
            @RequestParam(required = false) String registerSource,
            @RequestParam(required = false) Integer userStatus,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endTime) {
        
        UserQuery query = UserQuery.builder().userId(userId).identityType(identityType).identityValue(identityValue).nickname(nickname).registerSource(registerSource).userStatus(userStatus).startTime(startTime).endTime(endTime).build();
        startPage();
        List<UserSearchDocument> list = adminQueryAppService.queryUserList(query);
        return ApiResponse.ok(getPageResult(list));
    }

    @GetMapping("/merge-requests")
    public ApiResponse<PageResult<MergeRequestVo>> listMergeRequests(@RequestParam(required = false, defaultValue = "0") int reviewStatus) {
        startPage();
        List<MergeRequestDto> list = adminQueryAppService.queryMergeRequests(reviewStatus);
        List<MergeRequestVo> voList = list.stream().map(MergeRequestMapper.INSTANCE::toVo).collect(Collectors.toList());
        return ApiResponse.ok(getPageResult(voList));
    }

    @GetMapping("/deactivation-requests")
    public ApiResponse<PageResult<DeactivationRequestVo>> listDeactivationRequests(@RequestParam(required = false, defaultValue = "0") int reviewStatus) {
        startPage();
        List<DeactivationRequestDto> list = adminQueryAppService.queryDeactivationRequests(reviewStatus);
        List<DeactivationRequestVo> voList = list.stream().map(DeactivationRequestMapper.INSTANCE::toVo).collect(Collectors.toList());
        return ApiResponse.ok(getPageResult(voList));
    }

    // 其他方法保持不变...

    @GetMapping("/accounts/detail")
    public ApiResponse<AccountQueryAppService.UserDetail> getUserDetail(@RequestParam String userId) {
        return ApiResponse.ok(adminQueryAppService.queryUser(userId));
    }

    @GetMapping("/accounts/bindings")
    public ApiResponse<List<UserIdentityVo>> getUserBindings(@RequestParam String userId) {
        List<UserIdentityVo> voList = adminQueryAppService.queryBindingRelations(userId).stream()
                .map(UserIdentityMapper.INSTANCE::toVo)
                .collect(Collectors.toList());
        return ApiResponse.ok(voList);
    }
}
