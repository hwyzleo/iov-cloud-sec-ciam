package net.hwyz.iov.cloud.sec.ciam.service.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 头像上传申请结果（SEC-CIAM-DSN-CR-004 §6.1）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvatarUploadDto {

    /**
     * 上传任务标识
     */
    private String uploadId;
    /**
     * COS PUT 预签名地址
     */
    private String uploadUrl;
    /**
     * 上传方法，固定 PUT
     */
    private String httpMethod;
    /**
     * 上传时必须原样携带的 Content-Type
     */
    private String contentType;
    /**
     * 预签名过期时间，epoch milliseconds
     */
    private Long expiresAt;
}
