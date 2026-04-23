package net.hwyz.iov.cloud.sec.ciam.service.common.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MaskUtilTest {

    @Test
    void maskPhone_normalPhone_masksMiddleDigits() {
        assertEquals("138****1234", MaskUtil.maskPhone("13812341234"));
    }

    @Test
    void maskPhone_shortPhone_returnsAsIs() {
        assertEquals("123456", MaskUtil.maskPhone("123456"));
    }

    @Test
    void maskPhone_null_returnsNull() {
        assertNull(MaskUtil.maskPhone(null));
    }

    @Test
    void maskEmail_normalEmail_masksLocalPart() {
        assertEquals("t***@example.com", MaskUtil.maskEmail("test@example.com"));
    }

    @Test
    void maskEmail_singleCharLocal_masksAll() {
        assertEquals("*@example.com", MaskUtil.maskEmail("t@example.com"));
    }

    @Test
    void maskEmail_null_returnsNull() {
        assertNull(MaskUtil.maskEmail(null));
    }

    @Test
    void maskEmail_noAtSign_returnsAsIs() {
        assertEquals("noemail", MaskUtil.maskEmail("noemail"));
    }

    @Test
    void maskName_normalName_keepsFirstChar() {
        assertEquals("张**", MaskUtil.maskName("张三丰"));
    }

    @Test
    void maskName_singleChar_returnsStar() {
        assertEquals("*", MaskUtil.maskName("张"));
    }

    @Test
    void maskName_null_returnsNull() {
        assertNull(MaskUtil.maskName(null));
    }

    @Test
    void maskName_empty_returnsEmpty() {
        assertEquals("", MaskUtil.maskName(""));
    }
}
