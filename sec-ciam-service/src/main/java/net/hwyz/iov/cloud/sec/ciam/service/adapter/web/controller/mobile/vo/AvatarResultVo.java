package net.hwyz.iov.cloud.sec.ciam.service.adapter.web.controller.mobile.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 头像上传确认响应（SEC-CIAM-DSN-CR-004 §6.2）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AvatarResultVo {

    private String avatarUrl;
    private String objectId;
    private String contentType;
    private Long size;
}
