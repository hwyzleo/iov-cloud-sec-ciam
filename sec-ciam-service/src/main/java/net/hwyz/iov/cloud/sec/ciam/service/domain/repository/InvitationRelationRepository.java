package net.hwyz.iov.cloud.sec.ciam.service.domain.repository;

import net.hwyz.iov.cloud.sec.ciam.service.domain.model.InvitationRelation;

import java.util.List;
import java.util.Optional;

/**
 * 邀请关系表仓储接口。
 */
public interface InvitationRelationRepository {

    /** 根据业务 ID 查询 */
    Optional<InvitationRelation> findByRelationId(String relationId);

    /** 根据被邀请人用户 ID 查询 */
    Optional<InvitationRelation> findByInviteeUserId(String inviteeUserId);

    /** 根据邀请人用户 ID 查询 */
    List<InvitationRelation> findByInviterUserId(String inviterUserId);

    /** 根据渠道码查询 */
    List<InvitationRelation> findByInviteChannelCode(String inviteChannelCode);

    /** 插入邀请关系记录 */
    int insert(InvitationRelation entity);
}
