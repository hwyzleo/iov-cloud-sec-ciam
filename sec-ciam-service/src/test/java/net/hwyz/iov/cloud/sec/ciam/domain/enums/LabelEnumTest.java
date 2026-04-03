package net.hwyz.iov.cloud.sec.ciam.domain.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 字符串标签枚举（LabelEnum）统一测试。
 * <p>
 * 覆盖所有 VARCHAR 类型枚举的 fromValue 正向查找与非法值异常。
 */
class LabelEnumTest {

    // ---- IdentityType ----
    @Test
    void identityType_fromValue() {
        assertEquals(IdentityType.MOBILE, IdentityType.fromValue("mobile"));
        assertEquals(IdentityType.EMAIL, IdentityType.fromValue("email"));
        assertEquals(IdentityType.WECHAT, IdentityType.fromValue("wechat"));
        assertEquals(IdentityType.APPLE, IdentityType.fromValue("apple"));
        assertEquals(IdentityType.GOOGLE, IdentityType.fromValue("google"));
        assertEquals(IdentityType.LOCAL_MOBILE, IdentityType.fromValue("local_mobile"));
    }

    @Test
    void identityType_fromValue_invalidThrows() {
        assertThrows(IllegalArgumentException.class, () -> IdentityType.fromValue("unknown"));
    }

    // ---- CredentialType ----
    @Test
    void credentialType_fromValue() {
        assertEquals(CredentialType.EMAIL_PASSWORD, CredentialType.fromValue("email_password"));
    }

    // ---- ConsentType ----
    @Test
    void consentType_fromValue() {
        assertEquals(ConsentType.USER_AGREEMENT, ConsentType.fromValue("user_agreement"));
        assertEquals(ConsentType.PRIVACY_POLICY, ConsentType.fromValue("privacy_policy"));
        assertEquals(ConsentType.MARKETING, ConsentType.fromValue("marketing"));
    }

    // ---- ClientType ----
    @Test
    void clientType_fromValue() {
        assertEquals(ClientType.APP, ClientType.fromValue("app"));
        assertEquals(ClientType.MINI_PROGRAM, ClientType.fromValue("mini_program"));
        assertEquals(ClientType.WEB, ClientType.fromValue("web"));
        assertEquals(ClientType.VEHICLE, ClientType.fromValue("vehicle"));
        assertEquals(ClientType.ADMIN, ClientType.fromValue("admin"));
    }

    // ---- OAuthClientType ----
    @Test
    void oauthClientType_fromValue() {
        assertEquals(OAuthClientType.PUBLIC, OAuthClientType.fromValue("public"));
        assertEquals(OAuthClientType.CONFIDENTIAL, OAuthClientType.fromValue("confidential"));
        assertEquals(OAuthClientType.INTERNAL, OAuthClientType.fromValue("internal"));
    }

    // ---- ChallengeType ----
    @Test
    void challengeType_fromValue() {
        assertEquals(ChallengeType.SMS, ChallengeType.fromValue("sms"));
        assertEquals(ChallengeType.EMAIL, ChallengeType.fromValue("email"));
    }

    // ---- ChallengeScene ----
    @Test
    void challengeScene_fromValue() {
        assertEquals(ChallengeScene.NEW_DEVICE, ChallengeScene.fromValue("new_device"));
        assertEquals(ChallengeScene.GEO_CHANGE, ChallengeScene.fromValue("geo_change"));
        assertEquals(ChallengeScene.HIGH_RISK, ChallengeScene.fromValue("high_risk"));
    }

    // ---- DecisionResult ----
    @Test
    void decisionResult_fromValue() {
        assertEquals(DecisionResult.ALLOW, DecisionResult.fromValue("allow"));
        assertEquals(DecisionResult.CHALLENGE, DecisionResult.fromValue("challenge"));
        assertEquals(DecisionResult.BLOCK, DecisionResult.fromValue("block"));
        assertEquals(DecisionResult.KICKOUT, DecisionResult.fromValue("kickout"));
    }

    @Test
    void decisionResult_fromValue_invalidThrows() {
        assertThrows(IllegalArgumentException.class, () -> DecisionResult.fromValue("deny"));
    }

    // ---- LabelEnum interface contract ----
    @Test
    void labelEnum_getValue_getDescription() {
        LabelEnum e = IdentityType.MOBILE;
        assertEquals("mobile", e.getValue());
        assertEquals("手机号", e.getDescription());
    }
}
