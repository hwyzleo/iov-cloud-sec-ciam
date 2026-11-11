package net.hwyz.iov.cloud.sec.ciam.service.application;

import net.hwyz.iov.cloud.framework.common.exception.BusinessException;
import net.hwyz.iov.cloud.sec.ciam.service.application.dto.AvatarResultDto;
import net.hwyz.iov.cloud.sec.ciam.service.application.dto.AvatarUploadDto;
import net.hwyz.iov.cloud.sec.ciam.service.application.service.AvatarUploadAppService;
import net.hwyz.iov.cloud.sec.ciam.service.application.service.UserProfileAppService;
import net.hwyz.iov.cloud.sec.ciam.service.common.audit.AuditEvent;
import net.hwyz.iov.cloud.sec.ciam.service.common.audit.AuditLogger;
import net.hwyz.iov.cloud.sec.ciam.service.common.exception.CiamErrorCode;
import net.hwyz.iov.cloud.sec.ciam.service.domain.gateway.AvatarUploadRegistry;
import net.hwyz.iov.cloud.sec.ciam.service.domain.gateway.ImageObjectInfo;
import net.hwyz.iov.cloud.sec.ciam.service.domain.gateway.ImageStorageGateway;
import net.hwyz.iov.cloud.sec.ciam.service.domain.gateway.ImageUploadTicket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AvatarUploadAppService 单元测试（SEC-CIAM-DSN-CR-004 TC-004-1～9）。
 */
class AvatarUploadAppServiceTest {

    private static final String USER_ID = "u-1001";
    private static final String OTHER_USER = "u-2002";
    private static final String OBJECT_ID = "6f1b2c3d-0000-4000-8000-000000000001";
    private static final String ACCESS_URL = "https://bucket.cos.example.com/images/sec-ciam/" + OBJECT_ID + ".png";

    private UserProfileAppService userProfileAppService;
    private ImageStorageGateway imageStorageGateway;
    private AvatarUploadRegistry avatarUploadRegistry;
    private AuditLogger auditLogger;
    private AvatarUploadAppService service;

    @BeforeEach
    void setUp() {
        userProfileAppService = mock(UserProfileAppService.class);
        imageStorageGateway = mock(ImageStorageGateway.class);
        avatarUploadRegistry = mock(AvatarUploadRegistry.class);
        auditLogger = mock(AuditLogger.class);
        service = new AvatarUploadAppService(userProfileAppService, imageStorageGateway,
                avatarUploadRegistry, auditLogger);
        ReflectionTestUtils.setField(service, "uploadEnabled", true);
        ReflectionTestUtils.setField(service, "registryTtlSeconds", 960L);
    }

    private ImageUploadTicket stubTicket(String contentType) {
        return ImageUploadTicket.builder()
                .objectId(OBJECT_ID)
                .objectKey("images/sec-ciam/" + OBJECT_ID + ".png")
                .uploadUrl("https://cos.example.com/presigned?sig=x")
                .httpMethod("PUT")
                .contentType(contentType)
                .expiresAt(1_762_900_000_000L)
                .build();
    }

    private ImageObjectInfo stubObject() {
        return ImageObjectInfo.builder()
                .objectId(OBJECT_ID)
                .accessUrl(ACCESS_URL)
                .contentType("image/png")
                .size(204_800L)
                .build();
    }

    // ---- requestUpload ----

    @Nested
    class RequestUploadTests {

        @Test
        @DisplayName("TC-004-1 合法申请返回凭证并登记归属")
        void returnsTicketAndBindsOwner() {
            when(imageStorageGateway.generateUpload(anyString(), anyString(), anyLong()))
                    .thenReturn(stubTicket("image/png"));

            AvatarUploadDto dto = service.requestUpload(USER_ID, "my-photo.png", "image/png", 204_800L);

            assertEquals(OBJECT_ID, dto.getUploadId());
            assertNotNull(dto.getUploadUrl());
            assertEquals("PUT", dto.getHttpMethod());
            assertEquals("image/png", dto.getContentType());
            assertEquals(1_762_900_000_000L, dto.getExpiresAt());
            verify(avatarUploadRegistry).bind(OBJECT_ID, USER_ID, 960L);
        }

        @Test
        @DisplayName("TC-004-4 传给 OSS 的文件名恒为 avatar-{userId}.{ext}")
        void sendsConventionFilenameToOss() {
            when(imageStorageGateway.generateUpload(anyString(), anyString(), anyLong()))
                    .thenReturn(stubTicket("image/jpeg"));

            service.requestUpload(USER_ID, "我的头像.JPEG", "image/jpeg", 1024L);

            ArgumentCaptor<String> filename = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> contentType = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<Long> size = ArgumentCaptor.forClass(Long.class);
            verify(imageStorageGateway).generateUpload(filename.capture(), contentType.capture(), size.capture());
            assertEquals("avatar-" + USER_ID + ".jpg", filename.getValue());
            assertEquals("image/jpeg", contentType.getValue());
            assertEquals(1024L, size.getValue());
        }

        @Test
        @DisplayName("TC-004-3 不支持的 MIME 快速失败且不调用 OSS")
        void rejectsUnsupportedContentType() {
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.requestUpload(USER_ID, "a.gif", "image/gif", 100L));

            assertEquals(CiamErrorCode.INVALID_PARAM, ex.getErrorCode());
            verifyNoInteractions(imageStorageGateway, avatarUploadRegistry);
        }

        @Test
        @DisplayName("TC-004-3 扩展名与 MIME 不匹配快速失败")
        void rejectsExtensionMismatch() {
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.requestUpload(USER_ID, "a.png", "image/jpeg", 100L));

            assertEquals(CiamErrorCode.INVALID_PARAM, ex.getErrorCode());
            verifyNoInteractions(imageStorageGateway);
        }

        @Test
        @DisplayName("TC-004-3 大小为 0、越界或缺失快速失败")
        void rejectsInvalidSize() {
            assertEquals(CiamErrorCode.INVALID_PARAM,
                    assertThrows(BusinessException.class,
                            () -> service.requestUpload(USER_ID, "a.png", "image/png", 0L)).getErrorCode());
            assertEquals(CiamErrorCode.INVALID_PARAM,
                    assertThrows(BusinessException.class,
                            () -> service.requestUpload(USER_ID, "a.png", "image/png", 10_485_761L)).getErrorCode());
            assertEquals(CiamErrorCode.INVALID_PARAM,
                    assertThrows(BusinessException.class,
                            () -> service.requestUpload(USER_ID, "a.png", "image/png", null)).getErrorCode());
            verifyNoInteractions(imageStorageGateway);
        }

        @Test
        @DisplayName("TC-004-3 文件名为空或含路径分隔符快速失败")
        void rejectsInvalidFilename() {
            assertEquals(CiamErrorCode.INVALID_PARAM,
                    assertThrows(BusinessException.class,
                            () -> service.requestUpload(USER_ID, "  ", "image/png", 10L)).getErrorCode());
            assertEquals(CiamErrorCode.INVALID_PARAM,
                    assertThrows(BusinessException.class,
                            () -> service.requestUpload(USER_ID, "../a.png", "image/png", 10L)).getErrorCode());
            verifyNoInteractions(imageStorageGateway);
        }

        @Test
        @DisplayName("TC-004-2 OSS 不可用时 fail-closed，不返回凭证也不登记归属")
        void failsClosedWhenOssUnavailable() {
            when(imageStorageGateway.generateUpload(anyString(), anyString(), anyLong()))
                    .thenThrow(new BusinessException(CiamErrorCode.IMAGE_SERVICE_UNAVAILABLE));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.requestUpload(USER_ID, "a.png", "image/png", 100L));

            assertEquals(CiamErrorCode.IMAGE_SERVICE_UNAVAILABLE, ex.getErrorCode());
            verify(avatarUploadRegistry, never()).bind(anyString(), anyString(), anyLong());
        }

        @Test
        @DisplayName("TC-004-2 归属登记失败时拒绝申请")
        void failsClosedWhenRegistryBindFails() {
            when(imageStorageGateway.generateUpload(anyString(), anyString(), anyLong()))
                    .thenReturn(stubTicket("image/png"));
            doThrow(new IllegalStateException("redis down"))
                    .when(avatarUploadRegistry).bind(anyString(), anyString(), anyLong());

            assertThrows(IllegalStateException.class,
                    () -> service.requestUpload(USER_ID, "a.png", "image/png", 100L));
        }

        @Test
        @DisplayName("功能关闭时申请端点不可用")
        void rejectsWhenDisabled() {
            ReflectionTestUtils.setField(service, "uploadEnabled", false);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.requestUpload(USER_ID, "a.png", "image/png", 100L));

            assertEquals(CiamErrorCode.INVALID_PARAM, ex.getErrorCode());
            verifyNoInteractions(imageStorageGateway, avatarUploadRegistry);
        }
    }

    // ---- completeUpload ----

    @Nested
    class CompleteUploadTests {

        @Test
        @DisplayName("TC-004-5 本人确认成功：写入 accessUrl、标记完成并审计")
        void writesAvatarOnSuccess() {
            when(avatarUploadRegistry.findOwner(OBJECT_ID)).thenReturn(Optional.of(USER_ID));
            when(avatarUploadRegistry.findCompletion(OBJECT_ID)).thenReturn(Optional.empty());
            when(imageStorageGateway.completeUpload(OBJECT_ID)).thenReturn(stubObject());

            AvatarResultDto dto = service.completeUpload(USER_ID, OBJECT_ID);

            assertEquals(ACCESS_URL, dto.getAvatarUrl());
            assertEquals(OBJECT_ID, dto.getObjectId());
            assertEquals(204_800L, dto.getSize());
            verify(userProfileAppService).updateAvatarUrl(USER_ID, ACCESS_URL);
            verify(avatarUploadRegistry).markCompleted(eq(OBJECT_ID), eq(USER_ID), any(ImageObjectInfo.class), eq(960L));

            ArgumentCaptor<AuditEvent> audit = ArgumentCaptor.forClass(AuditEvent.class);
            verify(auditLogger).log(audit.capture());
            assertEquals("AVATAR", audit.getValue().getEventType());
            assertEquals("头像上传完成", audit.getValue().getEventName());
        }

        @Test
        @DisplayName("TC-004-6 他人 uploadId 被拒绝，不调用 OSS 也不写库")
        void rejectsForeignUpload() {
            when(avatarUploadRegistry.findOwner(OBJECT_ID)).thenReturn(Optional.of(OTHER_USER));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.completeUpload(USER_ID, OBJECT_ID));

            assertEquals(CiamErrorCode.AVATAR_UPLOAD_FORBIDDEN, ex.getErrorCode());
            verifyNoInteractions(imageStorageGateway, userProfileAppService);
            verify(avatarUploadRegistry, never()).markCompleted(anyString(), anyString(), any(), anyLong());
        }

        @Test
        @DisplayName("TC-004-7 任务不存在或已过期返回 AVATAR_UPLOAD_NOT_FOUND")
        void rejectsMissingUpload() {
            when(avatarUploadRegistry.findOwner(OBJECT_ID)).thenReturn(Optional.empty());

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.completeUpload(USER_ID, OBJECT_ID));

            assertEquals(CiamErrorCode.AVATAR_UPLOAD_NOT_FOUND, ex.getErrorCode());
            verifyNoInteractions(imageStorageGateway, userProfileAppService);
        }

        @Test
        @DisplayName("TC-004-7 重复确认幂等返回同一地址，不再调用 OSS")
        void returnsIdempotentlyWhenAlreadyCompleted() {
            when(avatarUploadRegistry.findOwner(OBJECT_ID)).thenReturn(Optional.of(USER_ID));
            when(avatarUploadRegistry.findCompletion(OBJECT_ID)).thenReturn(Optional.of(stubObject()));

            AvatarResultDto dto = service.completeUpload(USER_ID, OBJECT_ID);

            assertEquals(ACCESS_URL, dto.getAvatarUrl());
            verifyNoInteractions(imageStorageGateway, userProfileAppService);
            verify(avatarUploadRegistry, never()).markCompleted(anyString(), anyString(), any(), anyLong());
        }

        @Test
        @DisplayName("TC-004-8 OSS 确认失败时不写库、不标记完成")
        void keepsBindingWhenOssFails() {
            when(avatarUploadRegistry.findOwner(OBJECT_ID)).thenReturn(Optional.of(USER_ID));
            when(avatarUploadRegistry.findCompletion(OBJECT_ID)).thenReturn(Optional.empty());
            when(imageStorageGateway.completeUpload(OBJECT_ID))
                    .thenThrow(new BusinessException(CiamErrorCode.INVALID_PARAM, "对象不存在"));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.completeUpload(USER_ID, OBJECT_ID));

            assertEquals(CiamErrorCode.INVALID_PARAM, ex.getErrorCode());
            verify(userProfileAppService, never()).updateAvatarUrl(anyString(), anyString());
            verify(avatarUploadRegistry, never()).markCompleted(anyString(), anyString(), any(), anyLong());
        }

        @Test
        @DisplayName("TC-004-8 写库失败时不标记完成，允许重试")
        void keepsBindingWhenWriteFails() {
            when(avatarUploadRegistry.findOwner(OBJECT_ID)).thenReturn(Optional.of(USER_ID));
            when(avatarUploadRegistry.findCompletion(OBJECT_ID)).thenReturn(Optional.empty());
            when(imageStorageGateway.completeUpload(OBJECT_ID)).thenReturn(stubObject());
            doThrow(new BusinessException(CiamErrorCode.PROFILE_NOT_FOUND))
                    .when(userProfileAppService).updateAvatarUrl(USER_ID, ACCESS_URL);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.completeUpload(USER_ID, OBJECT_ID));

            assertEquals(CiamErrorCode.PROFILE_NOT_FOUND, ex.getErrorCode());
            verify(avatarUploadRegistry, never()).markCompleted(anyString(), anyString(), any(), anyLong());
        }

        @Test
        @DisplayName("功能关闭时确认端点不可用")
        void rejectsWhenDisabled() {
            ReflectionTestUtils.setField(service, "uploadEnabled", false);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.completeUpload(USER_ID, OBJECT_ID));

            assertEquals(CiamErrorCode.INVALID_PARAM, ex.getErrorCode());
            verifyNoInteractions(avatarUploadRegistry, imageStorageGateway, userProfileAppService);
        }
    }
}
