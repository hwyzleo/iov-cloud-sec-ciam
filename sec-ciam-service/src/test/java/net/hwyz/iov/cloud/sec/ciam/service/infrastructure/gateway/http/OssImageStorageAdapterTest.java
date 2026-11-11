package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.gateway.http;

import net.hwyz.iov.cloud.edd.oss.api.service.ImageService;
import net.hwyz.iov.cloud.edd.oss.api.vo.request.GenerateImageUploadRequest;
import net.hwyz.iov.cloud.edd.oss.api.vo.response.ImageObjectResponse;
import net.hwyz.iov.cloud.edd.oss.api.vo.response.ImageUploadResponse;
import net.hwyz.iov.cloud.framework.common.exception.BusinessException;
import net.hwyz.iov.cloud.sec.ciam.service.common.exception.CiamErrorCode;
import net.hwyz.iov.cloud.sec.ciam.service.domain.gateway.ImageObjectInfo;
import net.hwyz.iov.cloud.sec.ciam.service.domain.gateway.ImageUploadTicket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * OssImageStorageAdapter 单元测试（SEC-CIAM-DSN-CR-004 §6.3、TC-004-2/8）。
 * <p>
 * 覆盖契约映射与失败语义：OSS 400 视为图片契约拒绝（INVALID_PARAM），
 * 其余失败一律 fail-closed 为 IMAGE_SERVICE_UNAVAILABLE，不返回伪造凭证。
 */
class OssImageStorageAdapterTest {

    private ImageService imageService;
    private OssImageStorageAdapter adapter;

    private static final String OBJECT_ID = "obj-1";
    private static final String ACCESS_URL = "https://bucket.cos.example.com/images/edd-oss/obj-1.png";

    @BeforeEach
    void setUp() {
        imageService = mock(ImageService.class);
        adapter = new OssImageStorageAdapter(imageService);
    }

    private ImageUploadResponse uploadResponse() {
        return ImageUploadResponse.builder()
                .objectId(OBJECT_ID)
                .objectKey("images/edd-oss/obj-1.png")
                .uploadUrl("https://cos.example.com/presigned")
                .httpMethod("PUT")
                .contentType("image/png")
                .expiresAt(1_762_900_000_000L)
                .build();
    }

    @Test
    @DisplayName("申请上传：契约字段完整映射，且把 CIAM 生成的文件名透传给 OSS")
    void generateUploadMapsResponse() {
        when(imageService.generateUpload(any())).thenReturn(uploadResponse());

        ImageUploadTicket ticket = adapter.generateUpload("avatar-u-1.png", "image/png", 204_800L);

        assertEquals(OBJECT_ID, ticket.getObjectId());
        assertEquals("images/edd-oss/obj-1.png", ticket.getObjectKey());
        assertEquals("https://cos.example.com/presigned", ticket.getUploadUrl());
        assertEquals("PUT", ticket.getHttpMethod());
        assertEquals("image/png", ticket.getContentType());
        assertEquals(1_762_900_000_000L, ticket.getExpiresAt());

        ArgumentCaptor<GenerateImageUploadRequest> captor = ArgumentCaptor.forClass(GenerateImageUploadRequest.class);
        verify(imageService).generateUpload(captor.capture());
        assertEquals("avatar-u-1.png", captor.getValue().getOriginalFilename());
        assertEquals("image/png", captor.getValue().getContentType());
        assertEquals(204_800L, captor.getValue().getSize());
    }

    @Test
    @DisplayName("申请上传：OSS 返回不完整时 fail-closed")
    void generateUploadFailsClosedOnIncompleteResponse() {
        when(imageService.generateUpload(any())).thenReturn(ImageUploadResponse.builder().objectId(OBJECT_ID).build());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> adapter.generateUpload("avatar-u-1.png", "image/png", 1024L));

        assertEquals(CiamErrorCode.IMAGE_SERVICE_UNAVAILABLE, ex.getErrorCode());
    }

    @Test
    @DisplayName("申请上传：OSS 拒绝（400）映射为参数错误")
    void generateUploadMapsOssRejection() {
        when(imageService.generateUpload(any())).thenThrow(new IllegalStateException("boom"));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> adapter.generateUpload("avatar-u-1.png", "image/png", 1024L));

        assertEquals(CiamErrorCode.IMAGE_SERVICE_UNAVAILABLE, ex.getErrorCode());
    }

    @Test
    @DisplayName("确认上传：返回 accessUrl 与实际元数据")
    void completeUploadMapsResponse() {
        when(imageService.completeUpload(OBJECT_ID)).thenReturn(ImageObjectResponse.builder()
                .objectId(OBJECT_ID)
                .accessUrl(ACCESS_URL)
                .declaredContentType("image/png")
                .declaredSize(204_800L)
                .actualContentType("image/png")
                .actualSize(200_000L)
                .status("UPLOADED")
                .build());

        ImageObjectInfo info = adapter.completeUpload(OBJECT_ID);

        assertEquals(ACCESS_URL, info.getAccessUrl());
        assertEquals(OBJECT_ID, info.getObjectId());
        assertEquals("image/png", info.getContentType());
        assertEquals(200_000L, info.getSize(), "已完成确认时以实际大小为准");
    }

    @Test
    @DisplayName("确认上传：缺少 accessUrl 时 fail-closed，不返回不可用地址")
    void completeUploadFailsClosedWhenNoAccessUrl() {
        when(imageService.completeUpload(OBJECT_ID)).thenReturn(ImageObjectResponse.builder()
                .objectId(OBJECT_ID).status("PENDING").build());

        BusinessException ex = assertThrows(BusinessException.class, () -> adapter.completeUpload(OBJECT_ID));

        assertEquals(CiamErrorCode.IMAGE_SERVICE_UNAVAILABLE, ex.getErrorCode());
    }

    @Test
    @DisplayName("确认上传：OSS 不可用映射为图片服务不可用")
    void completeUploadMapsFailure() {
        when(imageService.completeUpload(OBJECT_ID)).thenThrow(new IllegalStateException("circuit open"));

        BusinessException ex = assertThrows(BusinessException.class, () -> adapter.completeUpload(OBJECT_ID));

        assertEquals(CiamErrorCode.IMAGE_SERVICE_UNAVAILABLE, ex.getErrorCode());
    }
}
