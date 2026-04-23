package net.hwyz.iov.cloud.sec.ciam.service.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 邀请关系 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvitationRelationDto2 {
    private String relationId;
    private String inviterUserId;
    private String inviteeUserId;
    private String inviteCode;
    private String inviteChannelCode;
    private String inviteActivityCode;
    private Integer relationLockFlag;
    private Instant registerTime;
}
