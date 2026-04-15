package net.hwyz.iov.cloud.sec.ciam.controller.mp.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 更新账号请求
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateAccountRequest {

    @NotBlank
    private String userId;

    private String identityType;

    private String mobile;

    private String email;

    private String nickname;

    private Integer gender;

    private Boolean enabled;

    private String remark;

    private String adminId;
}
