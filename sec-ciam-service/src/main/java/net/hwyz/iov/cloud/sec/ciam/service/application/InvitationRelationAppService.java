package net.hwyz.iov.cloud.sec.ciam.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.exception.BusinessException;
import net.hwyz.iov.cloud.sec.ciam.service.common.audit.AuditEvent;
import net.hwyz.iov.cloud.sec.ciam.service.common.audit.AuditEventType;
import net.hwyz.iov.cloud.sec.ciam.service.common.audit.AuditLogger;
import net.hwyz.iov.cloud.sec.ciam.service.common.exception.CiamErrorCode;
import net.hwyz.iov.cloud.framework.common.util.DateTimeUtil;
import net.hwyz.iov.cloud.sec.ciam.service.common.util.UserIdGenerator;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.CiamInvitationRelationRepository;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.dataobject.InvitationRelationPo;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 邀请关系应用服务 — 编排注册时邀请关系与渠道留痕。
 * <p>
 * 职责：
 * <ul>
 *   <li>注册时记录邀请人、邀请码、活动渠道码等上下文</li>
 *   <li>注册成功后固化关系，不允许修改</li>
 *   <li>缺失邀请信息不阻断注册流程</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InvitationRelationAppService {

    private final CiamInvitationRelationRepository invitationRelationRepository;
    private final AuditLogger auditLogger;

    /**
     * 记录邀请关系。
     * <p>
     * 注册成功后调用，将邀请人、邀请码、渠道码等上下文固化为不可变记录。
     * 若该用户已存在邀请关系，则拒绝重复写入并抛出异常。
     * 若所有邀请字段均为空，则静默跳过，不阻断注册流程。
     *
     * @param userId            被邀请人用户 ID
     * @param inviterUserId     邀请人用户 ID（可为 null）
     * @param invitationCode    邀请码（可为 null）
     * @param channelCode       活动渠道码（可为 null）
     * @param channelName       渠道名称（可为 null）
     * @return 创建的邀请关系记录，若跳过则返回 null
     */
    public InvitationRelationPo recordInvitation(String userId,
                                                     String inviterUserId,
                                                     String invitationCode,
                                                     String channelCode,
                                                     String channelName) {
        if (userId == null || userId.isBlank()) {
            throw new BusinessException(CiamErrorCode.INVALID_PARAM, "userId 不能为空");
        }

        // 所有邀请字段均为空时静默跳过
        if (isAllBlank(inviterUserId, invitationCode, channelCode, channelName)) {
            log.debug("邀请信息全部为空，跳过记录: userId={}", userId);
            return null;
        }

        // 不可变性校验：已存在则拒绝
        Optional<InvitationRelationPo> existing =
                invitationRelationRepository.findByInviteeUserId(userId);
        if (existing.isPresent()) {
            log.warn("邀请关系已存在，拒绝重复写入: userId={}", userId);
            throw new BusinessException(CiamErrorCode.INVITATION_ALREADY_EXISTS);
        }

        InvitationRelationPo record = new InvitationRelationPo();
        record.setRelationId(UserIdGenerator.generate());
        record.setInviteeUserId(userId);
        record.setInviterUserId(inviterUserId);
        record.setInviteCode(invitationCode);
        record.setInviteChannelCode(channelCode);
        record.setInviteActivityCode(channelName);
        record.setRelationLockFlag(1);
        record.setRegisterTime(DateTimeUtil.getNowInstant());
        record.setRowVersion(1);
        record.setRowValid(1);
        record.setCreateTime(DateTimeUtil.getNowInstant());
        record.setModifyTime(DateTimeUtil.getNowInstant());

        invitationRelationRepository.insert(record);

        logAudit(userId, AuditEventType.INVITATION_RECORD, true);
        log.info("邀请关系记录成功: userId={}, inviterUserId={}, channelCode={}",
                userId, inviterUserId, channelCode);

        return record;
    }

    /**
     * 查询用户的邀请关系。
     *
     * @param userId 被邀请人用户 ID
     * @return 邀请关系记录（可能为空）
     */
    public Optional<InvitationRelationPo> getInvitationRelation(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new BusinessException(CiamErrorCode.INVALID_PARAM, "userId 不能为空");
        }
        return invitationRelationRepository.findByInviteeUserId(userId);
    }

    private boolean isAllBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return false;
            }
        }
        return true;
    }

    private void logAudit(String userId, AuditEventType eventType, boolean success) {
        auditLogger.log(AuditEvent.builder()
                .userId(userId)
                .eventType(eventType.getCategory())
                .eventName(eventType.getDescription())
                .success(success)
                .eventTime(DateTimeUtil.getNowInstant())
                .build());
    }
}
