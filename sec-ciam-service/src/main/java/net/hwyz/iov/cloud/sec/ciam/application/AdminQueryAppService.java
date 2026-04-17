package net.hwyz.iov.cloud.sec.ciam.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.exception.BusinessException;
import net.hwyz.iov.cloud.framework.common.util.DateTimeUtil;
import net.hwyz.iov.cloud.sec.ciam.common.exception.CiamErrorCode;
import net.hwyz.iov.cloud.sec.ciam.common.security.FieldEncryptor;
import net.hwyz.iov.cloud.sec.ciam.domain.enums.IdentityType;
import net.hwyz.iov.cloud.sec.ciam.domain.repository.*;
import net.hwyz.iov.cloud.sec.ciam.domain.search.SearchResult;
import net.hwyz.iov.cloud.sec.ciam.domain.search.SearchService;
import net.hwyz.iov.cloud.sec.ciam.infrastructure.repository.dao.dataobject.*;
import net.hwyz.iov.cloud.sec.ciam.infrastructure.search.document.AuditLogSearchDocument;
import net.hwyz.iov.cloud.sec.ciam.infrastructure.search.document.RiskEventSearchDocument;
import net.hwyz.iov.cloud.sec.ciam.infrastructure.search.document.UserSearchDocument;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
    private final CiamUserProfileRepository profileRepository;
    private final CiamUserTagRepository tagRepository;
    private final CiamMergeRequestRepository mergeRequestRepository;
    private final CiamDeactivationRequestRepository deactivationRequestRepository;
    private final SearchService searchService;
    private final FieldEncryptor fieldEncryptor;

    /**
     * 查询用户详情（含状态、标识列表、标签列表）。
     *
     * @param userId 用户业务唯一标识
     * @return 用户详情
     */
    public UserDetail queryUser(String userId) {
        CiamUserDo user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(CiamErrorCode.USER_NOT_FOUND));

        String identityType = null;
        String identityValue = null;

        List<CiamUserIdentityDo> identities = identityRepository.findByUserId(userId);
        for (CiamUserIdentityDo identity : identities) {
            if (IdentityType.fromCode(identity.getIdentityType()) == IdentityType.MOBILE ||
                    IdentityType.fromCode(identity.getIdentityType()) == IdentityType.EMAIL) {
                identityType = identity.getIdentityType();
                try {
                    identityValue = fieldEncryptor.decrypt(identity.getIdentityValue());
                } catch (Exception e) {
                    identityValue = identity.getIdentityValue();
                }
                break;
            }
        }

        String nickname = null;
        Integer gender = null;
        Optional<CiamUserProfileDo> profileOpt = profileRepository.findByUserId(userId);
        if (profileOpt.isPresent()) {
            nickname = profileOpt.get().getNickname();
            gender = profileOpt.get().getGender();
        }

        log.info("查询用户详情: userId={}", userId);
        return new UserDetail(
                user.getUserId(),
                user.getUserStatus(),
                user.getRegisterSource(),
                user.getRegisterChannel(),
                user.getPrimaryIdentityType(),
                DateTimeUtil.instantToOffsetDateTime(user.getLastLoginTime()),
                DateTimeUtil.instantToOffsetDateTime(user.getCreateTime()),
                user.getDescription(),
                nickname,
                gender,
                identityType,
                identityValue
        );
    }

    /**
     * 检索用户列表。
     *
     * @param userId         账号ID（精确）
     * @param identityType   身份类型（精确）
     * @param identityValue  账号（模糊）
     * @param nickname       昵称（模糊）
     * @param registerSource 注册来源（精确）
     * @param userStatus     状态（精确）
     * @param startTime      创建开始时间
     * @param endTime        创建结束时间
     * @param page           页码（从 0 开始）
     * @param size           每页大小
     * @return 用户检索结果
     */
    public SearchResult<UserSearchDocument> queryUserList(String userId, String identityType,
                                                          String identityValue, String nickname,
                                                          String registerSource, Integer userStatus,
                                                          OffsetDateTime startTime, OffsetDateTime endTime,
                                                          int page, int size) {
        log.info("检索用户列表: userId={}, identityType={}, identityValue={}, nickname={}, registerSource={}, userStatus={}, startTime={}, endTime={}",
                userId, identityType, identityValue, nickname, registerSource, userStatus, startTime, endTime);

        List<CiamUserDo> userList = userRepository.findAll();

        List<UserSearchDocument> resultList = userList.stream()
                .map(user -> {
                    UserSearchDocument doc = UserSearchDocument.builder()
                            .userId(user.getUserId())
                            .userStatus(user.getUserStatus())
                            .registerSource(user.getRegisterSource())
                            .registerChannel(user.getRegisterChannel())
                            .lastLoginTime(DateTimeUtil.instantToOffsetDateTime(user.getLastLoginTime()))
                            .createTime(DateTimeUtil.instantToOffsetDateTime(user.getCreateTime()))
                            .build();

                    // 查询身份信息
                    List<CiamUserIdentityDo> identities = identityRepository.findByUserId(user.getUserId());
                    for (CiamUserIdentityDo identity : identities) {
                        if (IdentityType.fromCode(identity.getIdentityType()) == IdentityType.MOBILE ||
                                IdentityType.fromCode(identity.getIdentityType()) == IdentityType.EMAIL) {
                            doc.setIdentityType(identity.getIdentityType());
                            try {
                                doc.setIdentityValue(fieldEncryptor.decrypt(identity.getIdentityValue()));
                            } catch (Exception e) {
                                doc.setIdentityValue(identity.getIdentityValue());
                            }
                            break;
                        }
                    }

                    // 查询昵称和性别
                    profileRepository.findByUserId(user.getUserId()).ifPresent(profile -> {
                        doc.setNickname(profile.getNickname());
                        doc.setGender(profile.getGender());
                    });

                    return doc;
                })
                .filter(doc -> {
                    // 账号ID精确查询
                    if (userId != null && !userId.isEmpty() && !userId.equals(doc.getUserId())) {
                        return false;
                    }
                    // 身份类型精确查询
                    if (identityType != null && !identityType.isEmpty() && !identityType.equals(doc.getIdentityType())) {
                        return false;
                    }
                    // 账号模糊查询
                    if (identityValue != null && !identityValue.isEmpty()) {
                        String value = doc.getIdentityValue();
                        if (value == null || !value.contains(identityValue)) {
                            return false;
                        }
                    }
                    // 昵称模糊查询
                    if (nickname != null && !nickname.isEmpty()) {
                        String nick = doc.getNickname();
                        if (nick == null || !nick.contains(nickname)) {
                            return false;
                        }
                    }
                    // 注册来源精确查询
                    if (registerSource != null && !registerSource.isEmpty() && !registerSource.equals(doc.getRegisterSource())) {
                        return false;
                    }
                    // 状态精确查询
                    if (userStatus != null && !userStatus.equals(doc.getUserStatus())) {
                        return false;
                    }
                    // 创建时间范围查询
                    if (startTime != null || endTime != null) {
                        OffsetDateTime createTime = doc.getCreateTime();
                        if (createTime == null) {
                            return false;
                        }
                        if (startTime != null && createTime.isBefore(startTime)) {
                            return false;
                        }
                        if (endTime != null && createTime.isAfter(endTime)) {
                            return false;
                        }
                    }
                    return true;
                })
                .collect(Collectors.toList());

        return paginate(resultList, page, size);
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
            String userId,
            Integer userStatus,
            String registerSource,
            String registerChannel,
            String primaryIdentityType,
            OffsetDateTime lastLoginTime,
            OffsetDateTime createTime,
            String description,
            String nickname,
            Integer gender,
            String identityType,
            String identityValue
    ) {
    }
}
