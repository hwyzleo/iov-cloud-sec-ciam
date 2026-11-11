package net.hwyz.iov.cloud.sec.ciam.service.application.assembler;

import net.hwyz.iov.cloud.sec.ciam.service.adapter.web.controller.mobile.vo.AvatarResultVo;
import net.hwyz.iov.cloud.sec.ciam.service.adapter.web.controller.mobile.vo.AvatarUploadVo;
import net.hwyz.iov.cloud.sec.ciam.service.application.dto.AvatarResultDto;
import net.hwyz.iov.cloud.sec.ciam.service.application.dto.AvatarUploadDto;

/**
 * 头像上传 DTO -> VO 转换（SEC-CIAM-DSN-CR-004 §6）。
 */
public final class AvatarAssembler {

    private AvatarAssembler() {
    }

    /**
     * 上传申请 DTO 转 VO。
     *
     * @param dto 应用层结果
     * @return 接口响应
     */
    public static AvatarUploadVo toVo(AvatarUploadDto dto) {
        if (dto == null) {
            return null;
        }
        return AvatarUploadVo.builder()
                .uploadId(dto.getUploadId())
                .uploadUrl(dto.getUploadUrl())
                .httpMethod(dto.getHttpMethod())
                .contentType(dto.getContentType())
                .expiresAt(dto.getExpiresAt())
                .build();
    }

    /**
     * 上传确认 DTO 转 VO。
     *
     * @param dto 应用层结果
     * @return 接口响应
     */
    public static AvatarResultVo toVo(AvatarResultDto dto) {
        if (dto == null) {
            return null;
        }
        return AvatarResultVo.builder()
                .avatarUrl(dto.getAvatarUrl())
                .objectId(dto.getObjectId())
                .contentType(dto.getContentType())
                .size(dto.getSize())
                .build();
    }
}
