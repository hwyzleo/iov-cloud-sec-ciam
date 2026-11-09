package net.hwyz.iov.cloud.sec.ciam.service.domain.service;

import net.hwyz.iov.cloud.framework.common.exception.BusinessException;
import net.hwyz.iov.cloud.sec.ciam.service.common.exception.CiamErrorCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * scope 规范化测试（SEC-CIAM-DSN-CR-003 §4.3，RD-003-2）。
 */
class OAuthScopeNormalizerTest {

    @Test
    void spaceSeparated_isPreservedInDeclarationOrder() {
        assertEquals("openid profile phone", OAuthScopeNormalizer.normalize("openid profile phone"));
    }

    @Test
    void commaSeparated_isAccepted() {
        assertEquals("openid profile phone", OAuthScopeNormalizer.normalize("openid,profile,phone"));
    }

    @Test
    void mixedSeparatorsAndExtraWhitespace_areNormalized() {
        assertEquals("openid profile phone",
                OAuthScopeNormalizer.normalize("  openid ,  profile\tphone  "));
    }

    @Test
    void blankEntries_areRemoved() {
        assertEquals("openid profile", OAuthScopeNormalizer.normalize("openid,, ,profile,"));
    }

    @Test
    void duplicateEntries_areRejected() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> OAuthScopeNormalizer.normalize("openid profile openid"));
        assertEquals(CiamErrorCode.SCOPE_EXCEEDED, ex.getErrorCode());
    }

    @Test
    void emptyScope_isRejectedInsteadOfDefaulting() {
        assertEquals(CiamErrorCode.SCOPE_EXCEEDED,
                assertThrows(BusinessException.class, () -> OAuthScopeNormalizer.normalize(null)).getErrorCode());
        assertEquals(CiamErrorCode.SCOPE_EXCEEDED,
                assertThrows(BusinessException.class, () -> OAuthScopeNormalizer.normalize("   ")).getErrorCode());
        assertEquals(CiamErrorCode.SCOPE_EXCEEDED,
                assertThrows(BusinessException.class, () -> OAuthScopeNormalizer.normalize(" , ")).getErrorCode());
    }

    @Test
    void split_returnsOrderedEntries() {
        assertEquals(3, OAuthScopeNormalizer.split("a,b c").size());
        assertTrue(OAuthScopeNormalizer.split(null).isEmpty());
    }
}
