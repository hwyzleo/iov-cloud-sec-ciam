package net.hwyz.iov.cloud.sec.ciam.domain.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 字符串标签枚举（LabelEnum）统一测试。
 * <p>
 * 覆盖所有 VARCHAR 类型枚举的 fromCode 正向查找与非法值异常。
 */
class LabelEnumTest {

    // ---- IdentityType ----
    @Test
    void identityType_fromCode() {
        assertEquals(IdentityType.MOBILE, IdentityType.fromCode("mobile"));
        assertEquals(IdentityType.EMAIL, IdentityType.fromCode("email"));
        assertEquals(IdentityType.WECHAT, IdentityType.fromCode("wechat"));
        assertEquals(IdentityType.APPLE, IdentityType.fromCode("apple"));
        assertEquals(IdentityType.GOOGLE, IdentityType.fromCode("google"));
        assertEquals(IdentityType.LOCAL_MOBILE, IdentityType.fromCode("local_mobile"));
    }

    @Test
    void identityType_fromCode_invalidThrows() {
        assertThrows(IllegalArgumentException.class, () -> IdentityType.fromCode("unknown"));
    }

    // ---- CredentialType ----
    @Test
    void credentialType_fromCode() {
        assertEquals(CredentialType.EMAIL_PASSWORD, CredentialType.fromCode("email_password"));
    }

    // ---- ConsentType ----
    @Test
    void consentType_fromCode() {
        assertEquals(ConsentType.USER_AGREEMENT, ConsentType.fromCode("user_agreement"));
        assertEquals(ConsentType.PRIVACY_POLICY, ConsentType.fromCode("privacy_policy"));
        assertEquals(ConsentType.MARKETING, ConsentType.fromCode("marketing"));
    }

    // ---- ClientType ----
    @Test
    void clientType_fromCode() {
        assertEquals(ClientType.APP, ClientType.fromCode("app"));
        assertEquals(ClientType.MINI_PROGRAM, ClientType.fromCode("mini_program"));
        assertEquals(ClientType.WEB, ClientType.fromCode("web"));
        assertEquals(ClientType.VEHICLE, ClientType.fromCode("vehicle"));
        assertEquals(ClientType.ADMIN, ClientType.fromCode("admin"));
    }

    // ---- OAuthClientType ----
    @Test
    void oauthClientType_fromCode() {
        assertEquals(OAuthClientType.PUBLIC, OAuthClientType.fromCode("public"));
        assertEquals(OAuthClientType.CONFIDENTIAL, OAuthClientType.fromCode("confidential"));
        assertEquals(OAuthClientType.INTERNAL, OAuthClientType.fromCode("internal"));
    }

    // ---- ChallengeType ----
    @Test
    void challengeType_fromCode() {
        assertEquals(ChallengeType.SMS, ChallengeType.fromCode("sms"));
        assertEquals(ChallengeType.EMAIL, ChallengeType.fromCode("email"));
    }

    // ---- ChallengeScene ----
    @Test
    void challengeScene_fromCode() {
        assertEquals(ChallengeScene.NEW_DEVICE, ChallengeScene.fromCode("new_device"));
        assertEquals(ChallengeScene.GEO_CHANGE, ChallengeScene.fromCode("geo_change"));
        assertEquals(ChallengeScene.HIGH_RISK, ChallengeScene.fromCode("high_risk"));
    }

    // ---- DecisionResult ----
    @Test
    void decisionResult_fromCode() {
        assertEquals(DecisionResult.ALLOW, DecisionResult.fromCode("allow"));
        assertEquals(DecisionResult.CHALLENGE, DecisionResult.fromCode("challenge"));
        assertEquals(DecisionResult.BLOCK, DecisionResult.fromCode("block"));
        assertEquals(DecisionResult.KICKOUT, DecisionResult.fromCode("kickout"));
    }

    @Test
    void decisionResult_fromCode_invalidThrows() {
        assertThrows(IllegalArgumentException.class, () -> DecisionResult.fromCode("deny"));
    }

    // ---- LabelEnum interface contract ----
    @Test
    void labelEnum_getCode_getDescription() {
        LabelEnum e = IdentityType.MOBILE;
        assertEquals("mobile", e.getCode());
        assertEquals("手机号", e.getDescription());
    }
}
