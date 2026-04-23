package net.hwyz.iov.cloud.sec.ciam.service.domain.enums;
import net.hwyz.iov.cloud.sec.ciam.service.application.service.*;
import net.hwyz.iov.cloud.sec.ciam.service.domain.adapter.*;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 数值编码枚举（CodeEnum）统一测试。
 * <p>
 * 覆盖所有 TINYINT 状态枚举的 fromCode 正向查找与非法编码异常。
 */
class CodeEnumTest {

    // ---- UserStatus ----
    @Test
    void userStatus_fromCode_allValues() {
        assertEquals(UserStatus.PENDING, UserStatus.fromCode(0));
        assertEquals(UserStatus.ACTIVE, UserStatus.fromCode(1));
        assertEquals(UserStatus.LOCKED, UserStatus.fromCode(2));
        assertEquals(UserStatus.DISABLED, UserStatus.fromCode(3));
        assertEquals(UserStatus.DEACTIVATING, UserStatus.fromCode(4));
        assertEquals(UserStatus.DEACTIVATED, UserStatus.fromCode(5));
    }

    @Test
    void userStatus_fromCode_invalidThrows() {
        assertThrows(IllegalArgumentException.class, () -> UserStatus.fromCode(99));
    }

    // ---- IdentityStatus ----
    @Test
    void identityStatus_fromCode() {
        assertEquals(IdentityStatus.UNBOUND, IdentityStatus.fromCode(0));
        assertEquals(IdentityStatus.BOUND, IdentityStatus.fromCode(1));
    }

    // ---- CredentialStatus ----
    @Test
    void credentialStatus_fromCode() {
        assertEquals(CredentialStatus.INVALID, CredentialStatus.fromCode(0));
        assertEquals(CredentialStatus.VALID, CredentialStatus.fromCode(1));
    }

    // ---- Gender ----
    @Test
    void gender_fromCode() {
        assertEquals(Gender.UNKNOWN, Gender.fromCode(0));
        assertEquals(Gender.MALE, Gender.fromCode(1));
        assertEquals(Gender.FEMALE, Gender.fromCode(2));
    }

    // ---- TagStatus ----
    @Test
    void tagStatus_fromCode() {
        assertEquals(TagStatus.INVALID, TagStatus.fromCode(0));
        assertEquals(TagStatus.VALID, TagStatus.fromCode(1));
    }

    // ---- ConsentStatus ----
    @Test
    void consentStatus_fromCode() {
        assertEquals(ConsentStatus.REVOKED, ConsentStatus.fromCode(0));
        assertEquals(ConsentStatus.AGREED, ConsentStatus.fromCode(1));
    }

    // ---- SessionStatus ----
    @Test
    void sessionStatus_fromCode() {
        assertEquals(SessionStatus.INVALID, SessionStatus.fromCode(0));
        assertEquals(SessionStatus.ACTIVE, SessionStatus.fromCode(1));
        assertEquals(SessionStatus.KICKED, SessionStatus.fromCode(2));
        assertEquals(SessionStatus.EXPIRED, SessionStatus.fromCode(3));
    }

    // ---- RiskLevel ----
    @Test
    void riskLevel_fromCode() {
        assertEquals(RiskLevel.LOW, RiskLevel.fromCode(0));
        assertEquals(RiskLevel.MEDIUM, RiskLevel.fromCode(1));
        assertEquals(RiskLevel.HIGH, RiskLevel.fromCode(2));
    }

    // ---- DeviceStatus ----
    @Test
    void deviceStatus_fromCode() {
        assertEquals(DeviceStatus.INVALID, DeviceStatus.fromCode(0));
        assertEquals(DeviceStatus.ACTIVE, DeviceStatus.fromCode(1));
    }

    // ---- ClientStatus ----
    @Test
    void clientStatus_fromCode() {
        assertEquals(ClientStatus.DISABLED, ClientStatus.fromCode(0));
        assertEquals(ClientStatus.ENABLED, ClientStatus.fromCode(1));
    }

    // ---- TokenStatus ----
    @Test
    void tokenStatus_fromCode() {
        assertEquals(TokenStatus.ACTIVE, TokenStatus.fromCode(1));
        assertEquals(TokenStatus.ROTATED, TokenStatus.fromCode(2));
        assertEquals(TokenStatus.REVOKED, TokenStatus.fromCode(3));
        assertEquals(TokenStatus.EXPIRED, TokenStatus.fromCode(4));
    }

    @Test
    void tokenStatus_fromCode_invalidThrows() {
        assertThrows(IllegalArgumentException.class, () -> TokenStatus.fromCode(0));
    }

    // ---- ChallengeStatus ----
    @Test
    void challengeStatus_fromCode() {
        assertEquals(ChallengeStatus.PENDING, ChallengeStatus.fromCode(0));
        assertEquals(ChallengeStatus.PASSED, ChallengeStatus.fromCode(1));
        assertEquals(ChallengeStatus.FAILED, ChallengeStatus.fromCode(2));
        assertEquals(ChallengeStatus.EXPIRED, ChallengeStatus.fromCode(3));
        assertEquals(ChallengeStatus.CANCELLED, ChallengeStatus.fromCode(4));
    }

    // ---- OperationResult ----
    @Test
    void operationResult_fromCode() {
        assertEquals(OperationResult.FAILURE, OperationResult.fromCode(0));
        assertEquals(OperationResult.SUCCESS, OperationResult.fromCode(1));
    }

    // ---- ReviewStatus ----
    @Test
    void reviewStatus_fromCode() {
        assertEquals(ReviewStatus.PENDING, ReviewStatus.fromCode(0));
        assertEquals(ReviewStatus.APPROVED, ReviewStatus.fromCode(1));
        assertEquals(ReviewStatus.REJECTED, ReviewStatus.fromCode(2));
        assertEquals(ReviewStatus.CANCELLED, ReviewStatus.fromCode(3));
    }

    // ---- CheckStatus ----
    @Test
    void checkStatus_fromCode() {
        assertEquals(CheckStatus.PENDING, CheckStatus.fromCode(0));
        assertEquals(CheckStatus.PASSED, CheckStatus.fromCode(1));
        assertEquals(CheckStatus.FAILED, CheckStatus.fromCode(2));
    }

    // ---- ExecuteStatus ----
    @Test
    void executeStatus_fromCode() {
        assertEquals(ExecuteStatus.PENDING, ExecuteStatus.fromCode(0));
        assertEquals(ExecuteStatus.EXECUTED, ExecuteStatus.fromCode(1));
        assertEquals(ExecuteStatus.FAILED, ExecuteStatus.fromCode(2));
    }

    // ---- CertStatus ----
    @Test
    void certStatus_fromCode() {
        assertEquals(CertStatus.NOT_CERTIFIED, CertStatus.fromCode(0));
        assertEquals(CertStatus.CERTIFYING, CertStatus.fromCode(1));
        assertEquals(CertStatus.CERTIFIED, CertStatus.fromCode(2));
        assertEquals(CertStatus.CERT_FAILED, CertStatus.fromCode(3));
        assertEquals(CertStatus.CERT_EXPIRED, CertStatus.fromCode(4));
    }

    // ---- CodeEnum interface contract ----
    @Test
    void codeEnum_getCode_getDescription() {
        CodeEnum e = UserStatus.ACTIVE;
        assertEquals(1, e.getCode());
        assertEquals("正常", e.getDescription());
    }
}
