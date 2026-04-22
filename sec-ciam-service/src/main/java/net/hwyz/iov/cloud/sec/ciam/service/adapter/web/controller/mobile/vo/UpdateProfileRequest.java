package net.hwyz.iov.cloud.sec.ciam.service.adapter.web.controller.mobile.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateProfileRequest {
    private String nickname;
    private String avatarUrl;
    private Integer gender;
    private LocalDate birthday;
    private String regionCode;
    private String regionName;
}
