package net.hwyz.iov.cloud.sec.ciam.service.controller.mpt;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.framework.common.bean.ApiResponse;
import net.hwyz.iov.cloud.framework.common.bean.PageResult;
import net.hwyz.iov.cloud.framework.web.controller.BaseController;
import net.hwyz.iov.cloud.sec.ciam.service.application.AccountBindingAppService;
import net.hwyz.iov.cloud.sec.ciam.service.application.AccountLifecycleAppService;
import net.hwyz.iov.cloud.sec.ciam.service.application.AdminAccountAppService;
import net.hwyz.iov.cloud.sec.ciam.service.application.AccountQueryAppService;
import net.hwyz.iov.cloud.sec.ciam.service.application.StatisticsAppService;
import net.hwyz.iov.cloud.sec.ciam.service.application.dto.StatisticsResultDTO;
import net.hwyz.iov.cloud.sec.ciam.service.application.dto.DeactivationRequestDTO;
import net.hwyz.iov.cloud.sec.ciam.service.application.dto.MergeRequestDTO;
import net.hwyz.iov.cloud.sec.ciam.service.application.mapper.DeactivationRequestMapper;
import net.hwyz.iov.cloud.sec.ciam.service.application.mapper.MergeRequestMapper;
import net.hwyz.iov.cloud.sec.ciam.service.application.mapper.UserIdentityMapper;
import net.hwyz.iov.cloud.sec.ciam.service.controller.mpt.vo.*;
import net.hwyz.iov.cloud.sec.ciam.service.controller.vo.DeactivationRequestVO;
import net.hwyz.iov.cloud.sec.ciam.service.controller.vo.MergeRequestVO;
import net.hwyz.iov.cloud.sec.ciam.service.controller.vo.UserIdentityVO;
import net.hwyz.iov.cloud.sec.ciam.service.domain.query.UserQuery;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.search.document.AuditLogSearchDocument;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.search.document.RiskEventSearchDocument;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.search.document.UserSearchDocument;
import net.hwyz.iov.cloud.framework.security.util.SecurityUtils;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 运营后台管理控制器
 */
@RestController
@RequestMapping("/api/mpt/account/v1")
@RequiredArgsConstructor
public class MptAccountController extends BaseController {

    private final AccountQueryAppService adminQueryAppService;
    private final AccountLifecycleAppService accountLifecycleAppService;
    private final AccountBindingAppService accountBindingAppService;
    private final StatisticsAppService statisticsAppService;
    private final AdminAccountAppService adminAccountAppService;

    // ---- 账号管理 ----

    /** 创建账号 */
    @PostMapping("/accounts")
    public ApiResponse<String> createAccount(@RequestBody @Valid CreateAccountRequest request) {
        String identityValue = request.getIdentityType().equals("MOBILE") 
                ? request.getMobile() 
                : request.getEmail();
        String adminId = SecurityUtils.getUsername();
        String userId = adminAccountAppService.createAccount(
                request.getIdentityType(),
                identityValue,
                request.getPassword(),
                request.getNickname(),
                request.getGender(),
                request.getRegisterSource(),
                request.getEnabled(),
                request.getRemark(),
                adminId);
        return ApiResponse.ok(userId);
    }

    /** 更新账号 */
    @PutMapping("/accounts")
    public ApiResponse<Void> updateAccount(@RequestBody @Valid UpdateAccountRequest request) {
        String identityValue = request.getIdentityType() != null && request.getIdentityType().equals("MOBILE")
                ? request.getMobile()
                : request.getEmail();
        String adminId = SecurityUtils.getUsername();
        adminAccountAppService.updateAccount(
                request.getUserId(),
                request.getIdentityType(),
                identityValue,
                request.getNickname(),
                request.getGender(),
                request.getEnabled(),
                request.getRemark(),
                adminId);
        return ApiResponse.ok();
    }

    /** 删除账号 */
    @DeleteMapping("/accounts")
    public ApiResponse<Void> deleteAccount(@RequestBody @Valid DeleteAccountRequest request) {
        String adminId = SecurityUtils.getUsername();
        adminAccountAppService.deleteAccounts(request.getUserId(), adminId);
        return ApiResponse.ok();
    }

    // ---- 用户查询 ----

    /** 检索账号列表 */
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
        
        UserQuery query = UserQuery.builder()
                .userId(userId)
                .identityType(identityType)
                .identityValue(identityValue)
                .nickname(nickname)
                .registerSource(registerSource)
                .userStatus(userStatus)
                .startTime(startTime)
                .endTime(endTime)
                .build();
        
        startPage();
        List<UserSearchDocument> list = adminQueryAppService.queryUserList(query);
        return ApiResponse.ok(getPageResult(list));
    }

    /** 查询账号详情 */
    @GetMapping("/accounts/detail")
    public ApiResponse<AccountQueryAppService.UserDetail> getUserDetail(@RequestParam String userId) {
        return ApiResponse.ok(adminQueryAppService.queryUser(userId));
    }

    /** 查询账号绑定关系 */
    @GetMapping("/accounts/bindings")
    public ApiResponse<List<UserIdentityVO>> getUserBindings(@RequestParam String userId) {
        List<UserIdentityVO> voList = adminQueryAppService.queryBindingRelations(userId).stream()
                .map(UserIdentityMapper.INSTANCE::toVo)
                .collect(Collectors.toList());
        return ApiResponse.ok(voList);
    }

    // ---- 账号状态管理 ----

    /** 锁定账号 */
    @PostMapping("/accounts/lock")
    public ApiResponse<Void> lockAccount(@RequestBody @Valid LockAccountRequest request) {
        accountLifecycleAppService.adminLockAccount(request.getUserId(), request.getAdminId(), request.isInvalidateSessions());
        return ApiResponse.ok();
    }

    /** 解锁账号 */
    @PostMapping("/accounts/unlock")
    public ApiResponse<Void> unlockAccount(@RequestBody @Valid UnlockAccountRequest request) {
        accountLifecycleAppService.adminUnlockAccount(request.getUserId(), request.getAdminId());
        return ApiResponse.ok();
    }

    /** 禁用账号 */
    @PostMapping("/accounts/disable")
    public ApiResponse<Void> disableAccount(@RequestBody @Valid DisableAccountRequest request) {
        accountLifecycleAppService.adminDisableAccount(request.getUserId(), request.getAdminId(), request.isInvalidateSessions());
        return ApiResponse.ok();
    }

    /** 启用账号 */
    @PostMapping("/accounts/enable")
    public ApiResponse<Void> enableAccount(@RequestBody @Valid EnableAccountRequest request) {
        accountLifecycleAppService.adminEnableAccount(request.getUserId(), request.getAdminId());
        return ApiResponse.ok();
    }

    // ---- 合并申请管理 ----

    /** 查询合并申请列表 */
    @GetMapping("/merge-requests")
    public ApiResponse<PageResult<MergeRequestVO>> listMergeRequests(
            @RequestParam(required = false, defaultValue = "0") int reviewStatus) {
        startPage();
        List<MergeRequestDTO> list = adminQueryAppService.queryMergeRequests(reviewStatus);
        List<MergeRequestVO> voList = list.stream()
                .map(MergeRequestMapper.INSTANCE::toVo)
                .collect(Collectors.toList());
        return ApiResponse.ok(getPageResult(voList, list));
    }

    /** 审核通过合并申请 */
    @PostMapping("/merge-requests/approve")
    public ApiResponse<Void> approveMergeRequest(@RequestBody @Valid ApproveMergeRequest request) {
        accountBindingAppService.approveMergeRequest(request.getMergeRequestId(), request.getReviewer());
        return ApiResponse.ok();
    }

    /** 驳回合并申请 */
    @PostMapping("/merge-requests/reject")
    public ApiResponse<Void> rejectMergeRequest(@RequestBody @Valid RejectMergeRequest request) {
        accountBindingAppService.rejectMergeRequest(request.getMergeRequestId(), request.getReviewer());
        return ApiResponse.ok();
    }

    // ---- 注销申请管理 ----

    /** 查询注销申请列表 */
    @GetMapping("/deactivation-requests")
    public ApiResponse<PageResult<DeactivationRequestVO>> listDeactivationRequests(
            @RequestParam(required = false, defaultValue = "0") int reviewStatus) {
        startPage();
        List<DeactivationRequestDTO> list = adminQueryAppService.queryDeactivationRequests(reviewStatus);
        List<DeactivationRequestVO> voList = list.stream()
                .map(DeactivationRequestMapper.INSTANCE::toVo)
                .collect(Collectors.toList());
        return ApiResponse.ok(getPageResult(voList, list));
    }

    /** 审核通过注销申请 */
    @PostMapping("/deactivation-requests/approve")
    public ApiResponse<Void> approveDeactivation(@RequestBody @Valid ApproveDeactivationRequest request) {
        accountLifecycleAppService.approveDeactivation(request.getDeactivationRequestId(), request.getReviewer());
        return ApiResponse.ok();
    }

    /** 驳回注销申请 */
    @PostMapping("/deactivation-requests/reject")
    public ApiResponse<Void> rejectDeactivation(@RequestBody @Valid RejectDeactivationRequest request) {
        accountLifecycleAppService.rejectDeactivation(request.getDeactivationRequestId(), request.getReviewer());
        return ApiResponse.ok();
    }

    // ---- 统计分析 ----

    /** 注册转化统计 */
    @GetMapping("/statistics/registration")
    public ApiResponse<StatisticsResultDTO> getRegistrationStats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(required = false) String channel) {
        return ApiResponse.ok(statisticsAppService.getRegistrationStats(startTime, endTime, channel));
    }

    /** 登录成功率统计 */
    @GetMapping("/statistics/login")
    public ApiResponse<StatisticsResultDTO> getLoginStats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(required = false) String clientType) {
        return ApiResponse.ok(statisticsAppService.getLoginStats(startTime, endTime, clientType));
    }

    /** 来源渠道分布统计 */
    @GetMapping("/statistics/channels")
    public ApiResponse<StatisticsResultDTO> getChannelDistribution(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        return ApiResponse.ok(statisticsAppService.getChannelDistribution(startTime, endTime));
    }

    /** 第三方登录占比统计 */
    @GetMapping("/statistics/third-party")
    public ApiResponse<StatisticsResultDTO> getThirdPartyLoginDistribution(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        return ApiResponse.ok(statisticsAppService.getThirdPartyLoginDistribution(startTime, endTime));
    }
}
