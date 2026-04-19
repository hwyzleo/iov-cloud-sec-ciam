package net.hwyz.iov.cloud.sec.ciam.controller.mobile.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 切换语言请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChangeLanguageRequest {
    /** 语言代码，如 zh-CN, en-US */
    @NotBlank private String language;
}
