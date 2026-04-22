package net.hwyz.iov.cloud.sec.ciam.service.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 邀请关系领域模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvitationRelation {
    private String relationId;
    private String inviterUserId;
    private String inviteeUserId;
    private String inviteCode;
    private String inviteChannelCode;
    private String inviteActivityCode;
    private Integer relationLockFlag;
    private Instant registerTime;
    private String description;
}
