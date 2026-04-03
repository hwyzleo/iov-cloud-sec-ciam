package net.hwyz.iov.cloud.sec.ciam.common.audit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AuditEventTypeTest {

    @Test
    void loginSuccess_hasCategoryAndDescription() {
        AuditEventType type = AuditEventType.LOGIN_SUCCESS;
        assertEquals("LOGIN", type.getCategory());
        assertEquals("登录成功", type.getDescription());
    }

    @Test
    void allValues_haveNonNullCategoryAndDescription() {
        for (AuditEventType type : AuditEventType.values()) {
            assertNotNull(type.getCategory(), type.name() + " category should not be null");
            assertNotNull(type.getDescription(), type.name() + " description should not be null");
            assertFalse(type.getCategory().isBlank(), type.name() + " category should not be blank");
            assertFalse(type.getDescription().isBlank(), type.name() + " description should not be blank");
        }
    }

    @Test
    void auditEventTypes_coverDesignRequiredCategories() {
        // Verify all categories from design.md module 12 are represented
        boolean hasRegister = false, hasLogin = false, hasLogout = false;
        boolean hasVerificationCode = false, hasPassword = false, hasMfa = false;
        boolean hasBind = false, hasUnbind = false, hasMerge = false;
        boolean hasAccount = false, hasDeactivation = false;
        boolean hasConsent = false, hasRisk = false;

        for (AuditEventType type : AuditEventType.values()) {
            switch (type.getCategory()) {
                case "REGISTER" -> hasRegister = true;
                case "LOGIN" -> hasLogin = true;
                case "LOGOUT" -> hasLogout = true;
                case "VERIFICATION_CODE" -> hasVerificationCode = true;
                case "PASSWORD" -> hasPassword = true;
                case "MFA" -> hasMfa = true;
                case "BIND" -> hasBind = true;
                case "UNBIND" -> hasUnbind = true;
                case "MERGE" -> hasMerge = true;
                case "ACCOUNT" -> hasAccount = true;
                case "DEACTIVATION" -> hasDeactivation = true;
                case "CONSENT" -> hasConsent = true;
                case "RISK" -> hasRisk = true;
            }
        }

        assertTrue(hasRegister, "Missing REGISTER category");
        assertTrue(hasLogin, "Missing LOGIN category");
        assertTrue(hasLogout, "Missing LOGOUT category");
        assertTrue(hasVerificationCode, "Missing VERIFICATION_CODE category");
        assertTrue(hasPassword, "Missing PASSWORD category");
        assertTrue(hasMfa, "Missing MFA category");
        assertTrue(hasBind, "Missing BIND category");
        assertTrue(hasUnbind, "Missing UNBIND category");
        assertTrue(hasMerge, "Missing MERGE category");
        assertTrue(hasAccount, "Missing ACCOUNT category");
        assertTrue(hasDeactivation, "Missing DEACTIVATION category");
        assertTrue(hasConsent, "Missing CONSENT category");
        assertTrue(hasRisk, "Missing RISK category");
    }
}
