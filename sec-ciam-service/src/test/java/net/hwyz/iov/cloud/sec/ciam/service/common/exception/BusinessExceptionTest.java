package net.hwyz.iov.cloud.sec.ciam.service.common.exception;
import net.hwyz.iov.cloud.sec.ciam.service.application.service.*;
import net.hwyz.iov.cloud.sec.ciam.service.domain.adapter.*;

import net.hwyz.iov.cloud.framework.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BusinessExceptionTest {

    @Test
    void constructor_withErrorCode_setsCodeAndMessage() {
        BusinessException ex = new BusinessException(CiamErrorCode.USER_NOT_FOUND);
        assertEquals(CiamErrorCode.USER_NOT_FOUND, ex.getErrorCode());
        assertEquals("用户不存在", ex.getMessage());
    }

    @Test
    void constructor_withDetail_overridesMessage() {
        BusinessException ex = new BusinessException(CiamErrorCode.INVALID_PARAM, "手机号不能为空");
        assertEquals(CiamErrorCode.INVALID_PARAM, ex.getErrorCode());
        assertEquals("手机号不能为空", ex.getMessage());
    }

    @Test
    void constructor_withCause_preservesCause() {
        RuntimeException cause = new RuntimeException("root cause");
        BusinessException ex = new BusinessException(CiamErrorCode.INTERNAL_ERROR, cause);
        assertEquals(CiamErrorCode.INTERNAL_ERROR, ex.getErrorCode());
        assertSame(cause, ex.getCause());
    }
}
