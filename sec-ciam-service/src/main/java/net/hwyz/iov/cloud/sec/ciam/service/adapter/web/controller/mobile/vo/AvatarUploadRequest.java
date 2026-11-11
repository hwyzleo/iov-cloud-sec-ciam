package net.hwyz.iov.cloud.sec.ciam.service.adapter.web.controller.mobile.vo;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 申请头像上传请求（SEC-CIAM-DSN-CR-004 §6.1）。
 * <p>
 * originalFilename 仅用于快速失败校验，传给 OSS 的原始文件名由 CIAM 按
 * avatar-{userId}.{ext} 生成，不使用本字段的值。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AvatarUploadRequest {

    /**
     * APP 侧原始文件名（仅校验扩展名与 MIME 是否匹配）
     */
    @NotBlank(message = "originalFilename 不能为空")
    @Size(max = 255, message = "originalFilename 长度不能超过 255")
    private String originalFilename;

    /**
     * 声明 MIME，仅支持 image/jpeg、image/png、image/webp
     */
    @NotBlank(message = "contentType 不能为空")
    private String contentType;

    /**
     * 声明字节数，0 &lt; size &lt;= 10485760
     */
    @NotNull(message = "size 不能为空")
    @Min(value = 1, message = "size 必须大于 0")
    @Max(value = 10485760, message = "size 不能超过 10485760")
    private Long size;
}
