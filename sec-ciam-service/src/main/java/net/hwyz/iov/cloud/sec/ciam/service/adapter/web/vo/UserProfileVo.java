package net.hwyz.iov.cloud.sec.ciam.service.adapter.web.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.Instant;

/**
 * 用户个人资料 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileVo {

    private String userId;

    private String nickname;

    private String avatarUrl;

    private String realName;

    private Integer gender;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate birthday;

    private String regionCode;

    private String regionName;

    private String description;

    private Instant lastLoginTime;
}