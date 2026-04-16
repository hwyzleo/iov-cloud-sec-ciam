package net.hwyz.iov.cloud.sec.ciam.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.exception.BusinessException;
import net.hwyz.iov.cloud.sec.ciam.common.audit.AuditEvent;
import net.hwyz.iov.cloud.sec.ciam.common.audit.AuditEventType;
import net.hwyz.iov.cloud.sec.ciam.common.audit.AuditLogger;
import net.hwyz.iov.cloud.sec.ciam.common.exception.CiamErrorCode;
import net.hwyz.iov.cloud.sec.ciam.common.security.FieldEncryptor;
import net.hwyz.iov.cloud.sec.ciam.common.util.DateTimeUtil;
import net.hwyz.iov.cloud.sec.ciam.common.util.UserIdGenerator;
import net.hwyz.iov.cloud.sec.ciam.domain.enums.IdentityType;
import net.hwyz.iov.cloud.sec.ciam.domain.enums.ReviewStatus;
import net.hwyz.iov.cloud.sec.ciam.domain.repository.CiamMergeRequestRepository;
import net.hwyz.iov.cloud.sec.ciam.domain.service.IdentityDomainService;
import net.hwyz.iov.cloud.sec.ciam.infrastructure.repository.dao.dataobject.CiamMergeRequestDo;
import net.hwyz.iov.cloud.sec.ciam.infrastructure.repository.dao.dataobject.CiamUserIdentityDo;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * 账号绑定、解绑与合并应用服务 — 编排标识绑定/解绑、冲突检测与账号合并流程。
 * <p>
 * 职责：
 * <ul>
 *   <li>绑定标识前执行冲突检测，冲突时自动创建合并申请</li>
 *   <li>解绑标识前校验至少保留一种可用登录方式</li>
 *   <li>创建、审核、执行账号合并申请</li>
 *   <li>记录审计日志</li>
 * </ul>
 *
 * @see IdentityDomainService
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountBindingAppService {

    private final IdentityDomainService identityDomainService;
    private final CiamMergeRequestRepository mergeRequestRepository;
    private final FieldEncryptor fieldEncryptor;
    private final AuditLogger auditLogger;

    /**
     * 绑定登录标识到用户。
     * <p>
     * 绑定前通过 {@link IdentityDomainService#checkConflictDetail} 检查冲突：
     * <ul>
     *   <li>若存在冲突 → 自动创建合并申请并抛出 {@code MERGE_REQUEST_PENDING}</li>
     *   <li>若无冲突 → 委托领域服务完成绑定并记录审计日志</li>
     * </ul>
     *
     * @param userId        用户业务唯一标识
     * @param identityType  标识类型
     * @param identityValue 标识原值（如手机号、邮箱）
     * @param countryCode   国家区号（手机号场景使用，其他可为 null）
     * @param bindSource    绑定来源
     * @return 新创建的标识数据对象
     */
    public CiamUserIdentityDo bindIdentity(String userId, IdentityType identityType,
                                           String identityValue, String countryCode,
                                           String bindSource) {
        // 冲突检测：检查标识是否已被其他用户绑定
        Optional<CiamUserIdentityDo> conflict = identityDomainService.checkConflictDetail(
                identityType, identityValue, userId);

        if (conflict.isPresent()) {
            String conflictUserId = conflict.get().getUserId();
            String identityHash = conflict.get().getIdentityHash();

            // 自动创建合并申请
            createMergeRequest(userId, conflictUserId,
                    identityType.getCode(), identityHash, bindSource);

            log.info("绑定标识冲突，已创建合并申请: userId={}, conflictUserId={}, identityType={}",
                    userId, conflictUserId, identityType.getCode());

            throw new BusinessException(CiamErrorCode.MERGE_REQUEST_PENDING);
        }

        // 无冲突，执行绑定
        CiamUserIdentityDo result = identityDomainService.bindIdentity(
                userId, identityType, identityValue, countryCode, bindSource);

        logAudit(userId, AuditEventType.BIND, true);

        log.info("标识绑定成功: userId={}, identityType={}", userId, identityType.getCode());
        return result;
    }

    /**
     * 解绑登录标识。
     * <p>
     * 解绑前校验用户已绑定标识数量 ≥ 2，禁止解绑后账号无可用登录方式。
     *
     * @param userId       用户业务唯一标识
     * @param identityType 标识类型
     * @param identityHash 标识哈希值
     */
    public void unbindIdentity(String userId, IdentityType identityType, String identityHash) {
        long boundCount = identityDomainService.countBoundIdentities(userId);
        if (boundCount < 2) {
            throw new BusinessException(CiamErrorCode.UNBIND_LAST_IDENTITY);
        }

        identityDomainService.unbindIdentity(userId, identityType, identityHash);

        logAudit(userId, AuditEventType.UNBIND, true);

        log.info("标识解绑成功: userId={}, identityType={}", userId, identityType.getCode());
    }

    /**
     * 创建账号合并申请。
     *
     * @param sourceUserId         源账号用户业务标识（发起绑定的用户）
     * @param targetUserId         目标账号用户业务标识（冲突标识所属用户）
     * @param conflictIdentityType 冲突标识类型
     * @param conflictIdentityHash 冲突标识哈希值
     * @param applySource          申请来源
     * @return 新创建的合并申请记录
     */
    public CiamMergeRequestDo createMergeRequest(String sourceUserId, String targetUserId,
                                                 String conflictIdentityType,
                                                 String conflictIdentityHash,
                                                 String applySource) {
        CiamMergeRequestDo request = new CiamMergeRequestDo();
        request.setMergeRequestId(UserIdGenerator.generate());
        request.setSourceUserId(sourceUserId);
        request.setTargetUserId(targetUserId);
        request.setConflictIdentityType(conflictIdentityType);
        request.setConflictIdentityHash(conflictIdentityHash);
        request.setApplySource(applySource);
        request.setReviewStatus(ReviewStatus.PENDING.getCode());
        request.setRowVersion(1);
        request.setRowValid(1);
        request.setCreateTime(DateTimeUtil.now());
        request.setModifyTime(DateTimeUtil.now());

        mergeRequestRepository.insert(request);

        logAudit(sourceUserId, AuditEventType.MERGE_APPLY, true);

        log.info("合并申请已创建: mergeRequestId={}, sourceUserId={}, targetUserId={}",
                request.getMergeRequestId(), sourceUserId, targetUserId);
        return request;
    }

    /**
     * 审核通过合并申请。
     *
     * @param mergeRequestId 合并申请业务唯一标识
     * @param reviewer       审核人
     */
    public void approveMergeRequest(String mergeRequestId, String reviewer) {
        CiamMergeRequestDo request = mergeRequestRepository.findByMergeRequestId(mergeRequestId)
                .orElseThrow(() -> new BusinessException(CiamErrorCode.INVALID_PARAM, "合并申请不存在"));

        request.setReviewStatus(ReviewStatus.APPROVED.getCode());
        request.setReviewer(reviewer);
        request.setReviewTime(DateTimeUtil.now());
        request.setModifyTime(DateTimeUtil.now());

        mergeRequestRepository.updateByMergeRequestId(request);

        logAudit(request.getSourceUserId(), AuditEventType.MERGE_REVIEW, true);

        log.info("合并申请审核通过: mergeRequestId={}, reviewer={}", mergeRequestId, reviewer);
    }

    /**
     * 驳回合并申请。
     *
     * @param mergeRequestId 合并申请业务唯一标识
     * @param reviewer       审核人
     */
    public void rejectMergeRequest(String mergeRequestId, String reviewer) {
        CiamMergeRequestDo request = mergeRequestRepository.findByMergeRequestId(mergeRequestId)
                .orElseThrow(() -> new BusinessException(CiamErrorCode.INVALID_PARAM, "合并申请不存在"));

        request.setReviewStatus(ReviewStatus.REJECTED.getCode());
        request.setReviewer(reviewer);
        request.setReviewTime(DateTimeUtil.now());
        request.setModifyTime(DateTimeUtil.now());

        mergeRequestRepository.updateByMergeRequestId(request);

        logAudit(request.getSourceUserId(), AuditEventType.MERGE_REVIEW, true);

        log.info("合并申请已驳回: mergeRequestId={}, reviewer={}", mergeRequestId, reviewer);
    }

    /**
     * 执行账号合并。
     * <p>
     * 将非最终保留用户的所有已绑定标识迁移到最终保留用户，并标记合并完成。
     *
     * @param mergeRequestId 合并申请业务唯一标识
     * @param finalUserId    最终保留的用户业务标识（由用户选择）
     */
    public void executeMerge(String mergeRequestId, String finalUserId) {
        CiamMergeRequestDo request = mergeRequestRepository.findByMergeRequestId(mergeRequestId)
                .orElseThrow(() -> new BusinessException(CiamErrorCode.INVALID_PARAM, "合并申请不存在"));

        if (request.getReviewStatus() != ReviewStatus.APPROVED.getCode()) {
            throw new BusinessException(CiamErrorCode.INVALID_PARAM, "合并申请未审核通过");
        }

        // 确定非最终保留用户
        String nonFinalUserId;
        if (finalUserId.equals(request.getSourceUserId())) {
            nonFinalUserId = request.getTargetUserId();
        } else if (finalUserId.equals(request.getTargetUserId())) {
            nonFinalUserId = request.getSourceUserId();
        } else {
            throw new BusinessException(CiamErrorCode.INVALID_PARAM,
                    "最终保留用户必须是合并申请中的源账号或目标账号");
        }

        // 迁移非最终用户的所有已绑定标识到最终用户
        List<CiamUserIdentityDo> identitiesToMigrate =
                identityDomainService.findByUserId(nonFinalUserId);

        for (CiamUserIdentityDo identity : identitiesToMigrate) {
            // 解绑旧用户的标识
            identityDomainService.unbindIdentity(nonFinalUserId,
                    IdentityType.fromCode(identity.getIdentityType()),
                    identity.getIdentityHash());

            // 解密存储的标识值，再绑定到最终用户
            String rawIdentityValue = fieldEncryptor.decrypt(identity.getIdentityValue());
            identityDomainService.bindIdentity(finalUserId,
                    IdentityType.fromCode(identity.getIdentityType()),
                    rawIdentityValue,
                    identity.getCountryCode(),
                    "merge");
        }

        // 标记合并完成
        request.setFinalUserId(finalUserId);
        request.setFinishTime(DateTimeUtil.now());
        request.setModifyTime(DateTimeUtil.now());
        mergeRequestRepository.updateByMergeRequestId(request);

        logAudit(finalUserId, AuditEventType.MERGE_COMPLETE, true);

        log.info("账号合并执行完成: mergeRequestId={}, finalUserId={}, migratedIdentities={}",
                mergeRequestId, finalUserId, identitiesToMigrate.size());
    }

    private void logAudit(String userId, AuditEventType eventType, boolean success) {
        auditLogger.log(AuditEvent.builder()
                .userId(userId)
                .eventType(eventType.getCategory())
                .eventName(eventType.getDescription())
                .success(success)
                .eventTime(DateTimeUtil.now())
                .build());
    }
}
