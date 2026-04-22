package net.hwyz.iov.cloud.sec.ciam.service.domain.repository;

import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.dataobject.InvitationRelationPo;

import java.util.List;
import java.util.Optional;

/**
 * 邀请关系表仓储接口。
 */
public interface CiamInvitationRelationRepository {

    /** 根据业务 ID 查询 */
    Optional<InvitationRelationPo> findByRelationId(String relationId);

    /** 根据被邀请人用户 ID 查询 */
    Optional<InvitationRelationPo> findByInviteeUserId(String inviteeUserId);

    /** 根据邀请人用户 ID 查询 */
    List<InvitationRelationPo> findByInviterUserId(String inviterUserId);

    /** 根据渠道码查询 */
    List<InvitationRelationPo> findByInviteChannelCode(String inviteChannelCode);

    /** 插入邀请关系记录 */
    int insert(InvitationRelationPo entity);
}
