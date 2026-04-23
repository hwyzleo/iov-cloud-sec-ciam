package net.hwyz.iov.cloud.sec.ciam.service.application.assembler;

import net.hwyz.iov.cloud.sec.ciam.service.adapter.web.vo.UserSearchResponse;
import net.hwyz.iov.cloud.sec.ciam.service.application.dto.UserSearchDto2;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.util.List;

@Component
public class UserSearchAssembler {

    public UserSearchResponse toVo(UserSearchDto2 dto) {
        if (dto == null) {
            return null;
        }
        return UserSearchResponse.builder()
                .userId(dto.getUserId())
                .userStatus(dto.getUserStatus())
                .registerSource(dto.getRegisterSource())
                .registerChannel(dto.getRegisterChannel())
                .lastLoginTime(convertTime(dto.getLastLoginTime()))
                .createTime(convertTime(dto.getCreateTime()))
                .nickname(dto.getNickname())
                .gender(dto.getGender())
                .identityType(dto.getIdentityType())
                .identityValue(dto.getIdentityValue())
                .build();
    }

    public List<UserSearchResponse> toVoList(List<UserSearchDto2> dtoList) {
        if (dtoList == null) {
            return null;
        }
        return dtoList.stream()
                .map(this::toVo)
                .toList();
    }

    private java.time.Instant convertTime(java.time.OffsetDateTime offsetDateTime) {
        if (offsetDateTime == null) {
            return null;
        }
        return offsetDateTime.atZoneSameInstant(ZoneId.systemDefault()).toInstant();
    }
}