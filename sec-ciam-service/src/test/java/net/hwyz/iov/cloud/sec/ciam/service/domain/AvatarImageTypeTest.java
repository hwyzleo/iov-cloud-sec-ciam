package net.hwyz.iov.cloud.sec.ciam.service.domain;

import net.hwyz.iov.cloud.framework.common.exception.BusinessException;
import net.hwyz.iov.cloud.sec.ciam.service.common.exception.CiamErrorCode;
import net.hwyz.iov.cloud.sec.ciam.service.domain.enums.AvatarImageType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AvatarImageType 单元测试（SEC-CIAM-DSN-CR-004 §5.1、RD-004-11、TC-004-3/4）。
 */
class AvatarImageTypeTest {

    @Test
    @DisplayName("TC-004-3 按 MIME 精确匹配，忽略大小写与空白")
    void resolvesContentType() {
        assertEquals(AvatarImageType.PNG, AvatarImageType.fromContentType("image/png").orElseThrow());
        assertEquals(AvatarImageType.JPEG, AvatarImageType.fromContentType(" IMAGE/JPEG ").orElseThrow());
        assertEquals(AvatarImageType.WEBP, AvatarImageType.fromContentType("image/webp").orElseThrow());
        assertEquals(Optional.empty(), AvatarImageType.fromContentType("image/gif"));
        assertEquals(Optional.empty(), AvatarImageType.fromContentType(null));
    }

    @Test
    @DisplayName("TC-004-3 大小必须大于 0 且不超过 10 MiB")
    void validatesSize() {
        assertEquals(1L, AvatarImageType.requireSize(1L));
        assertEquals(AvatarImageType.MAX_SIZE_BYTES, AvatarImageType.requireSize(AvatarImageType.MAX_SIZE_BYTES));
        assertEquals(CiamErrorCode.INVALID_PARAM,
                assertThrows(BusinessException.class, () -> AvatarImageType.requireSize(0L)).getErrorCode());
        assertEquals(CiamErrorCode.INVALID_PARAM,
                assertThrows(BusinessException.class,
                        () -> AvatarImageType.requireSize(AvatarImageType.MAX_SIZE_BYTES + 1)).getErrorCode());
        assertEquals(CiamErrorCode.INVALID_PARAM,
                assertThrows(BusinessException.class, () -> AvatarImageType.requireSize(null)).getErrorCode());
    }

    @Test
    @DisplayName("TC-004-4 文件名固定为 avatar-{userId}.{ext}，扩展名由 MIME 派生")
    void buildsConventionFilename() {
        assertEquals("avatar-u-1001.png", AvatarImageType.buildOriginalFilename("u-1001", "image/png"));
        assertEquals("avatar-u1001.jpg", AvatarImageType.buildOriginalFilename("u1001", "image/jpeg"));
        assertEquals("avatar-u_1.webp", AvatarImageType.buildOriginalFilename("u_1", "image/webp"));
    }

    @Test
    @DisplayName("TC-004-4 非法 userId 拒绝生成文件名")
    void rejectsUnsafeUserId() {
        assertEquals(CiamErrorCode.INVALID_PARAM,
                assertThrows(BusinessException.class,
                        () -> AvatarImageType.buildOriginalFilename("../etc/passwd", "image/png")).getErrorCode());
        assertEquals(CiamErrorCode.INVALID_PARAM,
                assertThrows(BusinessException.class,
                        () -> AvatarImageType.buildOriginalFilename(null, "image/png")).getErrorCode());
    }

    @Test
    @DisplayName("TC-004-3 客户端文件名仅作快速失败校验")
    void validatesClientFilename() {
        assertDoesNotThrow(() -> AvatarImageType.validateClientFilename("photo.png", "image/png"));
        assertDoesNotThrow(() -> AvatarImageType.validateClientFilename("photo.JPEG", "image/jpeg"));

        assertEquals(CiamErrorCode.INVALID_PARAM,
                assertThrows(BusinessException.class,
                        () -> AvatarImageType.validateClientFilename("photo.png", "image/jpeg")).getErrorCode());
        assertEquals(CiamErrorCode.INVALID_PARAM,
                assertThrows(BusinessException.class,
                        () -> AvatarImageType.validateClientFilename("photo", "image/png")).getErrorCode());
        assertEquals(CiamErrorCode.INVALID_PARAM,
                assertThrows(BusinessException.class,
                        () -> AvatarImageType.validateClientFilename("", "image/png")).getErrorCode());
        assertEquals(CiamErrorCode.INVALID_PARAM,
                assertThrows(BusinessException.class,
                        () -> AvatarImageType.validateClientFilename("a/b.png", "image/png")).getErrorCode());
        assertEquals(CiamErrorCode.INVALID_PARAM,
                assertThrows(BusinessException.class,
                        () -> AvatarImageType.validateClientFilename("photo.gif", "image/gif")).getErrorCode());
    }
}
