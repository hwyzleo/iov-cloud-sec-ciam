package net.hwyz.iov.cloud.sec.ciam.service.domain.repository;

import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.dataobject.CiamInvitationRelationDo;

import java.util.List;
import java.util.Optional;

/**
 * 邀请关系表仓储接口。
 */
public interface CiamInvitationRelationRepository {

    /** 根据业务 ID 查询 */
    Optional<CiamInvitationRelationDo> findByRelationId(String relationId);

    /** 根据被邀请人用户 ID 查询 */
    Optional<CiamInvitationRelationDo> findByInviteeUserId(String inviteeUserId);

    /** 根据邀请人用户 ID 查询 */
    List<CiamInvitationRelationDo> findByInviterUserId(String inviterUserId);

    /** 根据渠道码查询 */
    List<CiamInvitationRelationDo> findByInviteChannelCode(String inviteChannelCode);

    /** 插入邀请关系记录 */
    int insert(CiamInvitationRelationDo entity);
}
