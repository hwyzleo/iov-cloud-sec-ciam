package net.hwyz.iov.cloud.sec.ciam.common.response;

import net.hwyz.iov.cloud.framework.common.bean.ApiResponse;
import net.hwyz.iov.cloud.sec.ciam.common.exception.CiamErrorCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ApiResponseTest {

    @Test
    void ok_noData_returnsSuccessCode() {
        ApiResponse<Void> resp = ApiResponse.ok();
        assertEquals("000000", resp.getCode());
        assertEquals("成功", resp.getMessage());
        assertNull(resp.getData());
        assertTrue(resp.getTimestamp() > 0);
    }

    @Test
    void ok_withData_containsData() {
        ApiResponse<String> resp = ApiResponse.ok("hello");
        assertEquals("000000", resp.getCode());
        assertEquals("hello", resp.getData());
    }

    @Test
    void fail_withErrorCode_returnsErrorInfo() {
        ApiResponse<Void> resp = ApiResponse.fail(CiamErrorCode.USER_NOT_FOUND);
        assertEquals("200001", resp.getCode());
        assertEquals("用户不存在", resp.getMessage());
    }

    @Test
    void fail_withCustomMessage_overridesDefault() {
        ApiResponse<Void> resp = ApiResponse.fail(CiamErrorCode.INVALID_PARAM, "手机号格式错误");
        assertEquals("100001", resp.getCode());
        assertEquals("手机号格式错误", resp.getMessage());
    }
}
