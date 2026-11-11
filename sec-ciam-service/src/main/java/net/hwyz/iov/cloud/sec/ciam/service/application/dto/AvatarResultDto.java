package net.hwyz.iov.cloud.sec.ciam.service.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 头像上传确认结果（SEC-CIAM-DSN-CR-004 §6.2）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvatarResultDto {

    /**
     * 已生效的头像地址（OSS accessUrl）
     */
    private String avatarUrl;
    /**
     * 图片对象标识
     */
    private String objectId;
    /**
     * 实际 MIME
     */
    private String contentType;
    /**
     * 实际字节数
     */
    private Long size;
}
