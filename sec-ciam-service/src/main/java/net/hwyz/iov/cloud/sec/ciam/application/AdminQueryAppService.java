package net.hwyz.iov.cloud.sec.ciam.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.exception.BusinessException;
import net.hwyz.iov.cloud.sec.ciam.common.exception.CiamErrorCode;
import net.hwyz.iov.cloud.sec.ciam.domain.repository.CiamDeactivationRequestRepository;
import net.hwyz.iov.cloud.sec.ciam.domain.repository.CiamMergeRequestRepository;
import net.hwyz.iov.cloud.sec.ciam.domain.repository.CiamUserIdentityRepository;
import net.hwyz.iov.cloud.sec.ciam.domain.repository.CiamUserRepository;
import net.hwyz.iov.cloud.sec.ciam.domain.repository.CiamUserTagRepository;
import net.hwyz.iov.cloud.sec.ciam.domain.search.SearchResult;
import net.hwyz.iov.cloud.sec.ciam.domain.search.SearchService;
import net.hwyz.iov.cloud.sec.ciam.infrastructure.repository.dao.dataobject.CiamDeactivationRequestDo;
import net.hwyz.iov.cloud.sec.ciam.infrastructure.repository.dao.dataobject.CiamMergeRequestDo;
import net.hwyz.iov.cloud.sec.ciam.infrastructure.repository.dao.dataobject.CiamUserDo;
import net.hwyz.iov.cloud.sec.ciam.infrastructure.repository.dao.dataobject.CiamUserIdentityDo;
import net.hwyz.iov.cloud.sec.ciam.infrastructure.repository.dao.dataobject.CiamUserTagDo;
import net.hwyz.iov.cloud.sec.ciam.infrastructure.search.document.AuditLogSearchDocument;
import net.hwyz.iov.cloud.sec.ciam.infrastructure.search.document.RiskEventSearchDocument;
import net.hwyz.iov.cloud.sec.ciam.infrastructure.search.document.UserSearchDocument;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 运营后台查询应用服务 — 提供用户、绑定关系、合并申请、注销申请、审计日志、风险事件的查询能力。
 * <p>
 * 职责：
 * <ul>
 *   <li>用户详情查询（含状态、标识、标签）</li>
 *   <li>用户列表检索（通过 SearchService）</li>
 *   <li>绑定关系查看</li>
 *   <li>合并申请查看</li>
 *   <li>注销申请查看</li>
 *   <li>审计日志检索</li>
 *   <li>风险事件检索</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminQueryAppService {

    private final CiamUserRepository userRepository;
    private final CiamUserIdentityRepository identityRepository;
    private final CiamUserTagRepository tagRepository;
    private final CiamMergeRequestRepository mergeRequestRepository;
    private final CiamDeactivationRequestRepository deactivationRequestRepository;
    private final SearchService searchService;

    /**
     * 查询用户详情（含状态、标识列表、标签列表）。
     *
     * @param userId 用户业务唯一标识
     * @return 用户详情
     */
    public UserDetail queryUser(String userId) {
        CiamUserDo user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(CiamErrorCode.USER_NOT_FOUND));
        List<CiamUserIdentityDo> identities = identityRepository.findByUserId(userId);
        List<CiamUserTagDo> tags = tagRepository.findByUserId(userId);
        log.info("查询用户详情: userId={}", userId);
        return new UserDetail(user, identities, tags);
    }

    /**
     * 检索用户列表。
     *
     * @param query 关键词
     * @param page  页码（从 0 开始）
     * @param size  每页大小
     * @return 用户检索结果
     */
    public SearchResult<UserSearchDocument> queryUserList(String query, int page, int size) {
        log.info("检索用户列表: query={}, page={}, size={}", query, page, size);
        return searchService.searchUsers(query, page, size);
    }

    /**
     * 查询用户绑定关系。
     *
     * @param userId 用户业务唯一标识
     * @return 该用户的所有登录标识
     */
    public List<CiamUserIdentityDo> queryBindingRelations(String userId) {
        userRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(CiamErrorCode.USER_NOT_FOUND));
        log.info("查询绑定关系: userId={}", userId);
        return identityRepository.findByUserId(userId);
    }

    /**
     * 查询合并申请列表。
     *
     * @param reviewStatus 审核状态编码
     * @param page         页码（从 0 开始）
     * @param size         每页大小
     * @return 合并申请列表（简单分页）
     */
    public SearchResult<CiamMergeRequestDo> queryMergeRequests(int reviewStatus, int page, int size) {
        List<CiamMergeRequestDo> all = mergeRequestRepository.findByReviewStatus(reviewStatus);
        log.info("查询合并申请: reviewStatus={}, total={}", reviewStatus, all.size());
        return paginate(all, page, size);
    }

    /**
     * 查询注销申请列表。
     *
     * @param reviewStatus 审核状态编码
     * @param page         页码（从 0 开始）
     * @param size         每页大小
     * @return 注销申请列表（简单分页）
     */
    public SearchResult<CiamDeactivationRequestDo> queryDeactivationRequests(int reviewStatus, int page, int size) {
        List<CiamDeactivationRequestDo> all = deactivationRequestRepository.findByReviewStatus(reviewStatus);
        log.info("查询注销申请: reviewStatus={}, total={}", reviewStatus, all.size());
        return paginate(all, page, size);
    }

    /**
     * 检索审计日志。
     *
     * @param userId    用户 ID（可为 null）
     * @param eventType 事件类型（可为 null）
     * @param startTime 开始时间（可为 null）
     * @param endTime   结束时间（可为 null）
     * @param page      页码（从 0 开始）
     * @param size      每页大小
     * @return 审计日志检索结果
     */
    public SearchResult<AuditLogSearchDocument> queryAuditLogs(String userId, String eventType,
                                                               LocalDateTime startTime, LocalDateTime endTime,
                                                               int page, int size) {
        log.info("检索审计日志: userId={}, eventType={}, page={}, size={}", userId, eventType, page, size);
        return searchService.searchAuditLogs(userId, eventType, startTime, endTime, page, size);
    }

    /**
     * 检索风险事件。
     *
     * @param userId    用户 ID（可为 null）
     * @param riskLevel 风险等级（可为 null）
     * @param startTime 开始时间（可为 null）
     * @param endTime   结束时间（可为 null）
     * @param page      页码（从 0 开始）
     * @param size      每页大小
     * @return 风险事件检索结果
     */
    public SearchResult<RiskEventSearchDocument> queryRiskEvents(String userId, Integer riskLevel,
                                                                 LocalDateTime startTime, LocalDateTime endTime,
                                                                 int page, int size) {
        log.info("检索风险事件: userId={}, riskLevel={}, page={}, size={}", userId, riskLevel, page, size);
        return searchService.searchRiskEvents(userId, riskLevel, startTime, endTime, page, size);
    }

    // ---- 内部方法 ----

    /**
     * 对内存列表做简单分页。
     */
    private <T> SearchResult<T> paginate(List<T> all, int page, int size) {
        int total = all.size();
        int fromIndex = Math.min(page * size, total);
        int toIndex = Math.min(fromIndex + size, total);
        List<T> pageItems = all.subList(fromIndex, toIndex);
        return SearchResult.<T>builder()
                .items(pageItems)
                .total(total)
                .page(page)
                .size(size)
                .build();
    }

    /**
     * 用户详情聚合对象。
     */
    public record UserDetail(
            CiamUserDo user,
            List<CiamUserIdentityDo> identities,
            List<CiamUserTagDo> tags
    ) {}
}
