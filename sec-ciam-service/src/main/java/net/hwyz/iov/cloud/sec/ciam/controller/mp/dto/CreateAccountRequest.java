package net.hwyz.iov.cloud.sec.ciam.controller.mp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建账号请求
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateAccountRequest {

    @NotBlank
    private String identityType;

    private String mobile;

    private String email;

    private String password;

    private String nickname;

    private Integer gender;

    private String registerSource;

    private Boolean enabled;

    private String remark;

    private String adminId;
}
