package net.hwyz.iov.cloud.sec.ciam.service.domain.service;
import net.hwyz.iov.cloud.sec.ciam.service.application.service.*;
import net.hwyz.iov.cloud.sec.ciam.service.domain.adapter.*;

import net.hwyz.iov.cloud.framework.common.exception.BusinessException;
import net.hwyz.iov.cloud.sec.ciam.service.common.exception.CiamErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class PasswordPolicyServiceTest {

    private PasswordPolicyService service;

    @BeforeEach
    void setUp() {
        service = new PasswordPolicyService();
    }

    @Test
    void validate_acceptsCompliantPassword() {
        assertDoesNotThrow(() -> service.validate("P@ssw0rd!"));
    }

    @Test
    void validate_rejectsNull() {
        BusinessException ex = assertThrows(BusinessException.class, () -> service.validate(null));
        assertEquals(CiamErrorCode.PASSWORD_COMPLEXITY_INSUFFICIENT, ex.getErrorCode());
    }

    @Test
    void validate_rejectsTooShort() {
        BusinessException ex = assertThrows(BusinessException.class, () -> service.validate("P@ss1a"));
        assertEquals(CiamErrorCode.PASSWORD_COMPLEXITY_INSUFFICIENT, ex.getErrorCode());
    }

    @Test
    void validate_rejectsNoUppercase() {
        BusinessException ex = assertThrows(BusinessException.class, () -> service.validate("p@ssw0rd!"));
        assertEquals(CiamErrorCode.PASSWORD_COMPLEXITY_INSUFFICIENT, ex.getErrorCode());
    }

    @Test
    void validate_rejectsNoLowercase() {
        BusinessException ex = assertThrows(BusinessException.class, () -> service.validate("P@SSW0RD!"));
        assertEquals(CiamErrorCode.PASSWORD_COMPLEXITY_INSUFFICIENT, ex.getErrorCode());
    }

    @Test
    void validate_rejectsNoDigit() {
        BusinessException ex = assertThrows(BusinessException.class, () -> service.validate("P@sswOrd!"));
        assertEquals(CiamErrorCode.PASSWORD_COMPLEXITY_INSUFFICIENT, ex.getErrorCode());
    }

    @Test
    void validate_rejectsNoSpecialChar() {
        BusinessException ex = assertThrows(BusinessException.class, () -> service.validate("Passw0rdX"));
        assertEquals(CiamErrorCode.PASSWORD_COMPLEXITY_INSUFFICIENT, ex.getErrorCode());
    }

    @ParameterizedTest
    @ValueSource(strings = {"Abcdef1!", "Xy9#abcd", "Test123$longpassword"})
    void validate_acceptsVariousCompliantPasswords(String password) {
        assertDoesNotThrow(() -> service.validate(password));
    }

    @Test
    void validate_acceptsExactly8Characters() {
        assertDoesNotThrow(() -> service.validate("Aa1!xxxx"));
    }

    @Test
    void isValid_returnsTrueForCompliant() {
        assertTrue(service.isValid("P@ssw0rd!"));
    }

    @Test
    void isValid_returnsFalseForNonCompliant() {
        assertFalse(service.isValid("weak"));
    }
}
