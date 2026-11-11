package net.hwyz.iov.cloud.sec.ciam.service.adapter.web.controller.mobile.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 头像上传申请响应（SEC-CIAM-DSN-CR-004 §6.1）。
 * <p>
 * 不含任何 COS 长期密钥；uploadUrl 为预签名地址，不应写入客户端日志。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AvatarUploadVo {

    private String uploadId;
    private String uploadUrl;
    private String httpMethod;
    private String contentType;
    private Long expiresAt;
}
