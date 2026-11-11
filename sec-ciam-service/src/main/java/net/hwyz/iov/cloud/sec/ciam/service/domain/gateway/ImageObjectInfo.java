package net.hwyz.iov.cloud.sec.ciam.service.domain.gateway;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 已确认的图片对象（SEC-CIAM-DSN-CR-004 §6.2）。
 * <p>
 * accessUrl 为 EDD-OSS 返回的公有读访问地址，是写入 user_profile.avatar_url 的唯一取值来源。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageObjectInfo {

    /**
     * 上传任务与对象标识
     */
    private String objectId;
    /**
     * 已确认图片的公有读访问地址
     */
    private String accessUrl;
    /**
     * 实际 MIME
     */
    private String contentType;
    /**
     * 实际字节数
     */
    private Long size;
}
