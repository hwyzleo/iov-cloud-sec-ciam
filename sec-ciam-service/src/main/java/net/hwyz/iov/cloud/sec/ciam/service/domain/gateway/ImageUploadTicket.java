package net.hwyz.iov.cloud.sec.ciam.service.domain.gateway;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 图片上传凭证（SEC-CIAM-DSN-CR-004 §6.1）。
 * <p>
 * 由 EDD-OSS 生成，包含预签名上传地址；不含任何 COS 长期密钥。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageUploadTicket {

    /**
     * 上传任务与对象标识（UUID）
     */
    private String objectId;
    /**
     * OSS 生成的唯一对象键
     */
    private String objectKey;
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
