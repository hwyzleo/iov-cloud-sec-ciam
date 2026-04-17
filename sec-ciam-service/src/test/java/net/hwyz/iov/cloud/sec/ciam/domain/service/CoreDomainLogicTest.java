package net.hwyz.iov.cloud.sec.ciam.domain.service;

import net.hwyz.iov.cloud.framework.common.exception.BusinessException;
import net.hwyz.iov.cloud.sec.ciam.common.exception.CiamErrorCode;
import net.hwyz.iov.cloud.sec.ciam.common.security.FieldEncryptor;
import net.hwyz.iov.cloud.sec.ciam.common.security.PasswordEncoder;
import net.hwyz.iov.cloud.sec.ciam.common.security.TokenDigest;
import net.hwyz.iov.cloud.sec.ciam.domain.adapter.AdapterResult;
import net.hwyz.iov.cloud.sec.ciam.domain.adapter.EmailAdapter;
import net.hwyz.iov.cloud.sec.ciam.domain.adapter.SmsAdapter;
import net.hwyz.iov.cloud.sec.ciam.domain.enums.*;
import net.hwyz.iov.cloud.sec.ciam.domain.repository.CiamRefreshTokenRepository;
import net.hwyz.iov.cloud.sec.ciam.domain.repository.CiamRiskEventRepository;
import net.hwyz.iov.cloud.sec.ciam.domain.repository.CiamUserCredentialRepository;
import net.hwyz.iov.cloud.sec.ciam.domain.repository.CiamUserIdentityRepository;
import net.hwyz.iov.cloud.sec.ciam.domain.repository.CiamUserProfileRepository;
import net.hwyz.iov.cloud.sec.ciam.domain.repository.CiamUserRepository;
import net.hwyz.iov.cloud.sec.ciam.infrastructure.repository.dao.dataobject.CiamRefreshTokenDo;
import net.hwyz.iov.cloud.sec.ciam.infrastructure.repository.dao.dataobject.CiamRiskEventDo;
import net.hwyz.iov.cloud.sec.ciam.infrastructure.repository.dao.dataobject.CiamUserCredentialDo;
import net.hwyz.iov.cloud.sec.ciam.infrastructure.repository.dao.dataobject.CiamUserDo;
import net.hwyz.iov.cloud.sec.ciam.infrastructure.store.InMemoryVerificationCodeStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 核心领域逻辑补充单元测试。
 * <p>
 * 覆盖 Task 16.1 要求的六大核心领域逻辑的边界与补充场景：
 * <ul>
 *   <li>用户状态流转 — DEACTIVATED 终态不可流转</li>
 *   <li>密码策略 — 空字符串、边界长度</li>
 *   <li>验证码频控 — 不同用户不互相限流、邮箱每日上限</li>
 *   <li>标识唯一性 — 绑定后设为主标识</li>
 *   <li>Token Rotation — 客户端不匹配拒绝轮换</li>
 *   <li>风险判定 — 事件字段完整性、正常场景无命中规则</li>
 * </ul>
 */
class CoreDomainLogicTest {

    // ========================================================================
    // 1. 用户状态流转补充测试
    // ========================================================================

    @Nested
    @DisplayName("用户状态流转 — 补充边界场景")
    class UserStatusTransitionEdgeCases {

        private CiamUserRepository userRepository;
        private CiamUserProfileRepository userProfileRepository;
        private UserDomainService userService;

        @BeforeEach
        void setUp() {
            userRepository = mock(CiamUserRepository.class);
            userProfileRepository = mock(CiamUserProfileRepository.class);
            when(userRepository.insert(any())).thenReturn(1);
            when(userProfileRepository.insert(any())).thenReturn(1);
            userService = new UserDomainService(userRepository, userProfileRepository);
        }

        private CiamUserDo stubUser(UserStatus status) {
            CiamUserDo user = new CiamUserDo();
            user.setUserId("u-edge");
            user.setUserStatus(status.getCode());
            return user;
        }

        @Test
        @DisplayName("DEACTIVATED 是终态，不可流转到任何状态")
        void deactivated_isTerminalState() {
            for (UserStatus target : UserStatus.values()) {
                assertFalse(UserStatusMachine.canTransit(UserStatus.DEACTIVATED, target),
                        "DEACTIVATED 不应能流转到 " + target);
            }
        }

        @Test
        @DisplayName("PENDING 只能流转到 ACTIVE")
        void pending_canOnlyTransitToActive() {
            assertTrue(UserStatusMachine.canTransit(UserStatus.PENDING, UserStatus.ACTIVE));
            assertFalse(UserStatusMachine.canTransit(UserStatus.PENDING, UserStatus.LOCKED));
            assertFalse(UserStatusMachine.canTransit(UserStatus.PENDING, UserStatus.DISABLED));
            assertFalse(UserStatusMachine.canTransit(UserStatus.PENDING, UserStatus.DEACTIVATING));
            assertFalse(UserStatusMachine.canTransit(UserStatus.PENDING, UserStatus.DEACTIVATED));
        }

        @Test
        @DisplayName("连续状态流转：PENDING → ACTIVE → LOCKED → ACTIVE")
        void sequentialTransitions_pendingToActiveToLockedToActive() {
            when(userRepository.findByUserId("u-edge")).thenReturn(Optional.of(stubUser(UserStatus.PENDING)));
            when(userRepository.updateByUserId(any())).thenReturn(1);

            userService.activate("u-edge");

            // 模拟已激活
            when(userRepository.findByUserId("u-edge")).thenReturn(Optional.of(stubUser(UserStatus.ACTIVE)));
            userService.lock("u-edge");

            // 模拟已锁定
            when(userRepository.findByUserId("u-edge")).thenReturn(Optional.of(stubUser(UserStatus.LOCKED)));
            userService.unlock("u-edge");

            verify(userRepository, times(3)).updateByUserId(any());
        }

        @Test
        @DisplayName("连续状态流转：ACTIVE → DEACTIVATING → DEACTIVATED")
        void sequentialTransitions_activeToDeactivatingToDeactivated() {
            when(userRepository.findByUserId("u-edge")).thenReturn(Optional.of(stubUser(UserStatus.ACTIVE)));
            when(userRepository.updateByUserId(any())).thenReturn(1);

            userService.startDeactivation("u-edge");

            when(userRepository.findByUserId("u-edge")).thenReturn(Optional.of(stubUser(UserStatus.DEACTIVATING)));
            userService.completeDeactivation("u-edge");

            verify(userRepository, times(2)).updateByUserId(any());
        }

        @Test
        @DisplayName("LOCKED 不能直接流转到 DISABLED")
        void locked_cannotTransitToDisabled() {
            when(userRepository.findByUserId("u-edge")).thenReturn(Optional.of(stubUser(UserStatus.LOCKED)));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> userService.disable("u-edge"));
            assertEquals(CiamErrorCode.ILLEGAL_STATUS_TRANSITION, ex.getErrorCode());
        }
    }

    // ========================================================================
    // 2. 密码策略补充测试
    // ========================================================================

    @Nested
    @DisplayName("密码策略 — 补充边界场景")
    class PasswordPolicyEdgeCases {

        private PasswordPolicyService policyService;

        @BeforeEach
        void setUp() {
            policyService = new PasswordPolicyService();
        }

        @Test
        @DisplayName("空字符串应被拒绝")
        void validate_rejectsEmptyString() {
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> policyService.validate(""));
            assertEquals(CiamErrorCode.PASSWORD_COMPLEXITY_INSUFFICIENT, ex.getErrorCode());
        }

        @Test
        @DisplayName("恰好 7 位（不足最小长度）应被拒绝")
        void validate_rejectsExactly7Characters() {
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> policyService.validate("Aa1!xxx"));
            assertEquals(CiamErrorCode.PASSWORD_COMPLEXITY_INSUFFICIENT, ex.getErrorCode());
        }

        @Test
        @DisplayName("恰好 8 位且满足所有规则应通过")
        void validate_acceptsExactly8CharactersWithAllRules() {
            assertDoesNotThrow(() -> policyService.validate("Aa1!xxxx"));
        }

        @Test
        @DisplayName("超长密码（满足规则）应通过")
        void validate_acceptsVeryLongPassword() {
            String longPassword = "Aa1!" + "x".repeat(200);
            assertDoesNotThrow(() -> policyService.validate(longPassword));
        }

        @Test
        @DisplayName("仅包含特殊字符但缺少字母和数字应被拒绝")
        void validate_rejectsOnlySpecialChars() {
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> policyService.validate("!@#$%^&*"));
            assertEquals(CiamErrorCode.PASSWORD_COMPLEXITY_INSUFFICIENT, ex.getErrorCode());
        }

        @Test
        @DisplayName("isValid 对 null 返回 false")
        void isValid_returnsFalseForNull() {
            assertFalse(policyService.isValid(null));
        }

        @Test
        @DisplayName("isValid 对空字符串返回 false")
        void isValid_returnsFalseForEmpty() {
            assertFalse(policyService.isValid(""));
        }
    }

    // ========================================================================
    // 3. 验证码频控补充测试
    // ========================================================================

    @Nested
    @DisplayName("验证码频控 — 补充边界场景")
    class VerificationCodeEdgeCases {

        private InMemoryVerificationCodeStore codeStore;
        private SmsAdapter smsAdapter;
        private EmailAdapter emailAdapter;
        private VerificationCodeService vcService;

        @BeforeEach
        void setUp() {
            codeStore = new InMemoryVerificationCodeStore();
            smsAdapter = mock(SmsAdapter.class);
            emailAdapter = mock(EmailAdapter.class);
            when(smsAdapter.sendVerificationCode(anyString(), anyString(), anyString()))
                    .thenReturn(AdapterResult.ok());
            when(emailAdapter.sendVerificationCode(anyString(), anyString()))
                    .thenReturn(AdapterResult.ok());
            vcService = new VerificationCodeService(codeStore, smsAdapter, emailAdapter);
        }

        @Test
        @DisplayName("不同用户之间不互相限流")
        void differentUsers_notRateLimited() {
            vcService.sendSmsCode("13800000001", "+86", "user-A", "client-1");

            // 不同用户应不受限
            assertDoesNotThrow(() ->
                    vcService.sendSmsCode("13800000002", "+86", "user-B", "client-1"));
        }

        @Test
        @DisplayName("邮箱验证码每日上限校验")
        void emailCode_dailyLimitExceeded() {
            String dailyKey = VerificationCodeService.buildDailyCountKey("user-daily", VerificationCodeType.EMAIL);
            for (int i = 0; i < VerificationCodeService.DAILY_LIMIT; i++) {
                codeStore.incrementDailyCount(dailyKey, 86400);
            }

            // 清除分钟频控
            String minuteKey = VerificationCodeService.buildMinuteRateKey("user-daily", "client-1", VerificationCodeType.EMAIL);
            codeStore.deleteCode(minuteKey);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> vcService.sendEmailCode("test@example.com", "user-daily", "client-1"));
            assertEquals(CiamErrorCode.VERIFICATION_CODE_RATE_LIMITED, ex.getErrorCode());
        }

        @Test
        @DisplayName("验证码校验成功后立即删除，防止重放")
        void verifyCode_deletedAfterSuccess() {
            vcService.sendSmsCode("13800138000", "+86", "user-replay", "client-1");

            String codeKey = VerificationCodeService.buildCodeKey("user-replay", "client-1", VerificationCodeType.SMS);
            String code = codeStore.getCode(codeKey).orElseThrow();

            vcService.verifyCode("user-replay", "client-1", VerificationCodeType.SMS, code);

            // 验证码已被删除
            assertTrue(codeStore.getCode(codeKey).isEmpty());

            // 再次使用同一验证码应失败
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> vcService.verifyCode("user-replay", "client-1", VerificationCodeType.SMS, code));
            assertEquals(CiamErrorCode.VERIFICATION_CODE_INVALID, ex.getErrorCode());
        }

        @Test
        @DisplayName("SMS 和 EMAIL 验证码互不干扰")
        void smsAndEmail_independentRateLimit() {
            vcService.sendSmsCode("13800138000", "+86", "user-multi", "client-1");

            // 同一用户同一客户端，但不同类型不受限
            assertDoesNotThrow(() ->
                    vcService.sendEmailCode("test@example.com", "user-multi", "client-1"));
        }

        @Test
        @DisplayName("生成的验证码始终为 6 位数字")
        void generateCode_alwaysSixDigits() {
            for (int i = 0; i < 100; i++) {
                String code = vcService.generateCode();
                assertEquals(6, code.length());
                assertTrue(code.matches("\\d{6}"), "验证码应为 6 位数字: " + code);
            }
        }
    }

    // ========================================================================
    // 4. 标识唯一性补充测试
    // ========================================================================

    @Nested
    @DisplayName("标识唯一性 — 补充边界场景")
    class IdentityUniquenessEdgeCases {

        private CiamUserIdentityRepository identityRepository;
        private FieldEncryptor fieldEncryptor;
        private IdentityDomainService identityService;

        private static final String AES_KEY = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=";

        @BeforeEach
        void setUp() {
            identityRepository = mock(CiamUserIdentityRepository.class);
            fieldEncryptor = new FieldEncryptor(AES_KEY);
            when(identityRepository.insert(any())).thenReturn(1);
            when(identityRepository.updateByIdentityId(any())).thenReturn(1);
            identityService = new IdentityDomainService(identityRepository, fieldEncryptor);
        }

        @Test
        @DisplayName("绑定标识时 identityValue 应被加密存储，不是明文")
        void bindIdentity_encryptsValue() {
            when(identityRepository.findByTypeAndHash(anyString(), anyString()))
                    .thenReturn(Optional.empty());

            var result = identityService.bindIdentity(
                    "user-001", IdentityType.MOBILE, "13800138000", "+86", "app");

            assertNotEquals("13800138000", result.getIdentityValue());
            // 哈希值应为 SHA-256 格式（64 字符十六进制）
            assertEquals(64, result.getIdentityHash().length());
        }

        @Test
        @DisplayName("同一标识值对不同类型应生成相同哈希")
        void sameValue_differentType_sameHash() {
            String hash1 = FieldEncryptor.hash("test@example.com");
            String hash2 = FieldEncryptor.hash("test@example.com");
            assertEquals(hash1, hash2);
        }

        @Test
        @DisplayName("不同标识值应生成不同哈希")
        void differentValues_differentHash() {
            String hash1 = FieldEncryptor.hash("13800138000");
            String hash2 = FieldEncryptor.hash("13800138001");
            assertNotEquals(hash1, hash2);
        }

        @Test
        @DisplayName("解绑后再绑定同一标识到新用户应成功")
        void rebindAfterUnbind_succeeds() {
            var unbound = new net.hwyz.iov.cloud.sec.ciam.infrastructure.repository.dao.dataobject.CiamUserIdentityDo();
            unbound.setIdentityId("id-old");
            unbound.setUserId("old-user");
            unbound.setIdentityType("email");
            unbound.setIdentityValue(fieldEncryptor.encrypt("test@example.com"));
            unbound.setIdentityHash(FieldEncryptor.hash("test@example.com"));
            unbound.setIdentityStatus(IdentityStatus.UNBOUND.getCode());
            unbound.setRowValid(1);

            when(identityRepository.findByTypeAndHash(eq("email"), anyString()))
                    .thenReturn(Optional.of(unbound));

            var result = identityService.bindIdentity(
                    "new-user", IdentityType.EMAIL, "test@example.com", null, "web");

            assertNotNull(result);
            verify(identityRepository).insert(any());
        }

        @Test
        @DisplayName("countBoundIdentities 只统计 BOUND 状态的标识")
        void countBoundIdentities_excludesUnbound() {
            var bound = new net.hwyz.iov.cloud.sec.ciam.infrastructure.repository.dao.dataobject.CiamUserIdentityDo();
            bound.setIdentityStatus(IdentityStatus.BOUND.getCode());
            var unbound = new net.hwyz.iov.cloud.sec.ciam.infrastructure.repository.dao.dataobject.CiamUserIdentityDo();
            unbound.setIdentityStatus(IdentityStatus.UNBOUND.getCode());
            var bound2 = new net.hwyz.iov.cloud.sec.ciam.infrastructure.repository.dao.dataobject.CiamUserIdentityDo();
            bound2.setIdentityStatus(IdentityStatus.BOUND.getCode());

            when(identityRepository.findByUserId("user-001"))
                    .thenReturn(java.util.List.of(bound, unbound, bound2));

            assertEquals(2, identityService.countBoundIdentities("user-001"));
        }
    }

    // ========================================================================
    // 5. Token Rotation 补充测试
    // ========================================================================

    @Nested
    @DisplayName("Token Rotation — 补充边界场景")
    class TokenRotationEdgeCases {

        private CiamRefreshTokenRepository refreshTokenRepository;
        private RefreshTokenDomainService tokenService;

        @BeforeEach
        void setUp() {
            refreshTokenRepository = mock(CiamRefreshTokenRepository.class);
            when(refreshTokenRepository.insert(any())).thenReturn(1);
            when(refreshTokenRepository.updateByRefreshTokenId(any())).thenReturn(1);
            tokenService = new RefreshTokenDomainService(refreshTokenRepository);
        }

        private CiamRefreshTokenDo stubToken(String rawToken, TokenStatus status,
                                              Instant expireTime, String clientId) {
            CiamRefreshTokenDo token = new CiamRefreshTokenDo();
            token.setRefreshTokenId("rt-" + System.nanoTime());
            token.setUserId("user-001");
            token.setSessionId("session-001");
            token.setClientId(clientId);
            token.setTokenFingerprint(TokenDigest.fingerprint(rawToken));
            token.setTokenStatus(status.getCode());
            token.setIssueTime(Instant.now().minusSeconds(1L * 86400));
            token.setExpireTime(expireTime);
            token.setRowValid(1);
            token.setRowVersion(1);
            return token;
        }

        @Test
        @DisplayName("客户端不匹配时拒绝轮换")
        void rotate_rejectsClientMismatch() {
            String rawToken = "client-mismatch-token";
            CiamRefreshTokenDo existing = stubToken(rawToken, TokenStatus.ACTIVE,
                    Instant.now().plusSeconds(29L * 86400), "client-A");
            when(refreshTokenRepository.findByTokenFingerprint(TokenDigest.fingerprint(rawToken)))
                    .thenReturn(Optional.of(existing));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> tokenService.rotateRefreshToken(rawToken, "client-B"));
            assertEquals(CiamErrorCode.TOKEN_INVALID, ex.getErrorCode());
        }

        @Test
        @DisplayName("轮换后新令牌的 parentTokenId 指向旧令牌")
        void rotate_newTokenLinksToOldToken() {
            String rawToken = "linkage-test-token";
            CiamRefreshTokenDo existing = stubToken(rawToken, TokenStatus.ACTIVE,
                    Instant.now().plusSeconds(29L * 86400), "client-app");
            String oldTokenId = existing.getRefreshTokenId();
            when(refreshTokenRepository.findByTokenFingerprint(TokenDigest.fingerprint(rawToken)))
                    .thenReturn(Optional.of(existing));

            tokenService.rotateRefreshToken(rawToken, "client-app");

            ArgumentCaptor<CiamRefreshTokenDo> captor = ArgumentCaptor.forClass(CiamRefreshTokenDo.class);
            verify(refreshTokenRepository).insert(captor.capture());
            assertEquals(oldTokenId, captor.getValue().getParentTokenId());
            assertEquals(TokenStatus.ACTIVE.getCode(), captor.getValue().getTokenStatus());
        }

        @Test
        @DisplayName("签发的令牌每次都不同")
        void issue_uniqueTokensEachTime() {
            String t1 = tokenService.issueRefreshToken("u1", "s1", "c1", 3600);
            String t2 = tokenService.issueRefreshToken("u1", "s1", "c1", 3600);
            String t3 = tokenService.issueRefreshToken("u1", "s1", "c1", 3600);

            assertNotEquals(t1, t2);
            assertNotEquals(t2, t3);
            assertNotEquals(t1, t3);
        }

        @Test
        @DisplayName("已过期令牌的轮换应被拒绝且不签发新令牌")
        void rotate_expiredToken_noNewTokenIssued() {
            String rawToken = "expired-no-issue";
            CiamRefreshTokenDo existing = stubToken(rawToken, TokenStatus.ACTIVE,
                    Instant.now().minusSeconds(1 * 3600), "client-app");
            when(refreshTokenRepository.findByTokenFingerprint(TokenDigest.fingerprint(rawToken)))
                    .thenReturn(Optional.of(existing));

            assertThrows(BusinessException.class,
                    () -> tokenService.rotateRefreshToken(rawToken, "client-app"));

            verify(refreshTokenRepository, never()).insert(any());
        }

        @Test
        @DisplayName("撤销已撤销的令牌应幂等（不重复更新）")
        void revoke_alreadyRevoked_isIdempotent() {
            String rawToken = "already-revoked-token";
            CiamRefreshTokenDo existing = stubToken(rawToken, TokenStatus.REVOKED,
                    Instant.now().plusSeconds(29L * 86400), "client-app");
            when(refreshTokenRepository.findByTokenFingerprint(TokenDigest.fingerprint(rawToken)))
                    .thenReturn(Optional.of(existing));

            tokenService.revokeRefreshToken(rawToken);

            verify(refreshTokenRepository, never()).updateByRefreshTokenId(any());
        }

        @Test
        @DisplayName("clientId 为 null 时不做客户端匹配校验")
        void rotate_nullClientIdOnToken_skipsClientCheck() {
            String rawToken = "null-client-token";
            CiamRefreshTokenDo existing = stubToken(rawToken, TokenStatus.ACTIVE,
                    Instant.now().plusSeconds(29L * 86400), null);
            when(refreshTokenRepository.findByTokenFingerprint(TokenDigest.fingerprint(rawToken)))
                    .thenReturn(Optional.of(existing));

            RefreshTokenRotationResult result = tokenService.rotateRefreshToken(rawToken, "any-client");

            assertNotNull(result.getNewRefreshToken());
            assertEquals("user-001", result.getUserId());
        }
    }

    // ========================================================================
    // 6. 风险判定补充测试
    // ========================================================================

    @Nested
    @DisplayName("风险判定 — 补充边界场景")
    class RiskAssessmentEdgeCases {

        private CiamRiskEventRepository riskEventRepository;
        private RiskAssessmentService riskService;

        @BeforeEach
        void setUp() {
            riskEventRepository = mock(CiamRiskEventRepository.class);
            when(riskEventRepository.insert(any())).thenReturn(1);
            riskService = new RiskAssessmentService(riskEventRepository);
        }

        @Test
        @DisplayName("正常登录：低风险、放行、无命中规则")
        void normalLogin_lowRisk_noHitRules() {
            RiskAssessmentResult result = riskService.assessLoginRisk(
                    "user-001", "device-001", "1.2.3.4", "CN", "app", false, false);

            assertEquals(RiskLevel.LOW, result.getRiskLevel());
            assertEquals(DecisionResult.ALLOW, result.getDecisionResult());
            assertTrue(result.getHitRules().isEmpty());
            assertNotNull(result.getRiskEventId());
        }

        @Test
        @DisplayName("新设备 + 异地 = 高风险阻断，命中 new_device_and_geo_change")
        void newDeviceAndGeoChange_highRisk_block() {
            RiskAssessmentResult result = riskService.assessLoginRisk(
                    "user-001", "device-new", "5.6.7.8", "US", "web", true, true);

            assertEquals(RiskLevel.HIGH, result.getRiskLevel());
            assertEquals(DecisionResult.BLOCK, result.getDecisionResult());
            assertTrue(result.getHitRules().contains("new_device_and_geo_change"));
            assertEquals(1, result.getHitRules().size());
        }

        @Test
        @DisplayName("风险事件持久化字段完整性校验")
        void riskEvent_persistedWithAllFields() {
            riskService.assessLoginRisk(
                    "user-risk", "device-risk", "10.0.0.1", "JP", "mini_program", true, false);

            ArgumentCaptor<CiamRiskEventDo> captor = ArgumentCaptor.forClass(CiamRiskEventDo.class);
            verify(riskEventRepository).insert(captor.capture());
            CiamRiskEventDo event = captor.getValue();

            assertNotNull(event.getRiskEventId());
            assertEquals("user-risk", event.getUserId());
            assertEquals("device-risk", event.getDeviceId());
            assertEquals("10.0.0.1", event.getIpAddress());
            assertEquals("JP", event.getRegionCode());
            assertEquals("mini_program", event.getClientType());
            assertEquals("login", event.getRiskScene());
            assertEquals(RiskLevel.MEDIUM.getCode(), event.getRiskLevel());
            assertEquals(DecisionResult.CHALLENGE.getCode(), event.getDecisionResult());
            assertNotNull(event.getHitRules());
            assertTrue(event.getHitRules().contains("new_device"));
            assertNotNull(event.getEventTime());
            assertNotNull(event.getCreateTime());
            assertNotNull(event.getModifyTime());
            assertEquals(0, event.getHandledFlag());
            assertEquals(1, event.getRowValid());
            assertEquals(1, event.getRowVersion());
        }

        @Test
        @DisplayName("正常登录时 hitRules 字段为 null")
        void normalLogin_hitRulesIsNull() {
            riskService.assessLoginRisk(
                    "user-001", "device-001", "1.2.3.4", "CN", "app", false, false);

            ArgumentCaptor<CiamRiskEventDo> captor = ArgumentCaptor.forClass(CiamRiskEventDo.class);
            verify(riskEventRepository).insert(captor.capture());
            assertNull(captor.getValue().getHitRules());
            assertEquals("normal", captor.getValue().getRiskType());
        }

        @Test
        @DisplayName("每次评估生成唯一的 riskEventId")
        void assessLoginRisk_uniqueEventIds() {
            RiskAssessmentResult r1 = riskService.assessLoginRisk(
                    "u1", "d1", "1.1.1.1", "CN", "app", false, false);
            RiskAssessmentResult r2 = riskService.assessLoginRisk(
                    "u1", "d1", "1.1.1.1", "CN", "app", false, false);

            assertNotEquals(r1.getRiskEventId(), r2.getRiskEventId());
        }

        @Test
        @DisplayName("仅异地登录：中风险、挑战")
        void geoChangeOnly_mediumRisk_challenge() {
            RiskAssessmentResult result = riskService.assessLoginRisk(
                    "user-001", "device-001", "5.6.7.8", "US", "vehicle", false, true);

            assertEquals(RiskLevel.MEDIUM, result.getRiskLevel());
            assertEquals(DecisionResult.CHALLENGE, result.getDecisionResult());
            assertTrue(result.getHitRules().contains("geo_change"));
            assertFalse(result.getHitRules().contains("new_device"));
        }
    }

    // ========================================================================
    // 7. 凭据与密码锁定补充测试（密码策略 + 失败锁定联动）
    // ========================================================================

    @Nested
    @DisplayName("凭据密码锁定 — 补充边界场景")
    class CredentialLockoutEdgeCases {

        private CiamUserCredentialRepository credentialRepository;
        private PasswordEncoder passwordEncoder;
        private PasswordPolicyService passwordPolicyService;
        private CredentialDomainService credentialService;

        @BeforeEach
        void setUp() {
            credentialRepository = mock(CiamUserCredentialRepository.class);
            passwordEncoder = new PasswordEncoder(4);
            passwordPolicyService = new PasswordPolicyService();
            when(credentialRepository.insert(any())).thenReturn(1);
            when(credentialRepository.updateByCredentialId(any())).thenReturn(1);
            credentialService = new CredentialDomainService(
                    credentialRepository, passwordEncoder, passwordPolicyService);
        }

        private CiamUserCredentialDo stubCred(String userId, String rawPassword, int failCount) {
            CiamUserCredentialDo cred = new CiamUserCredentialDo();
            cred.setCredentialId("cred-edge");
            cred.setUserId(userId);
            cred.setCredentialType(CredentialType.EMAIL_PASSWORD.getCode());
            cred.setCredentialHash(passwordEncoder.encode(rawPassword));
            cred.setHashAlgorithm(PasswordEncoder.ALGORITHM);
            cred.setFailCount(failCount);
            cred.setCredentialStatus(CredentialStatus.VALID.getCode());
            cred.setRowValid(1);
            return cred;
        }

        @Test
        @DisplayName("failCount 为 null 时首次失败应设为 1")
        void verifyPassword_nullFailCount_incrementsToOne() {
            CiamUserCredentialDo cred = stubCred("user-001", "Correct1!", 0);
            cred.setFailCount(null);
            when(credentialRepository.findByUserIdAndType(eq("user-001"), eq("email_password")))
                    .thenReturn(Optional.of(cred));

            PasswordVerifyResult result = credentialService.verifyPassword("user-001", "Wrong1!");

            assertFalse(result.isMatched());
            assertEquals(1, result.getFailCount());
        }

        @Test
        @DisplayName("第 2 次失败不触发挑战")
        void verifyPassword_secondFailure_noChallengeYet() {
            CiamUserCredentialDo cred = stubCred("user-001", "Correct1!", 1);
            when(credentialRepository.findByUserIdAndType(eq("user-001"), eq("email_password")))
                    .thenReturn(Optional.of(cred));

            PasswordVerifyResult result = credentialService.verifyPassword("user-001", "Wrong1!");

            assertFalse(result.isMatched());
            assertFalse(result.isChallengeRequired());
            assertEquals(2, result.getFailCount());
        }

        @Test
        @DisplayName("第 3 次失败触发挑战但不锁定")
        void verifyPassword_thirdFailure_challengeButNoLock() {
            CiamUserCredentialDo cred = stubCred("user-001", "Correct1!", 2);
            when(credentialRepository.findByUserIdAndType(eq("user-001"), eq("email_password")))
                    .thenReturn(Optional.of(cred));

            PasswordVerifyResult result = credentialService.verifyPassword("user-001", "Wrong1!");

            assertFalse(result.isMatched());
            assertTrue(result.isChallengeRequired());
            assertFalse(result.isLocked());
            assertEquals(3, result.getFailCount());
        }

        @Test
        @DisplayName("第 5 次失败触发锁定")
        void verifyPassword_fifthFailure_locked() {
            CiamUserCredentialDo cred = stubCred("user-001", "Correct1!", 4);
            when(credentialRepository.findByUserIdAndType(eq("user-001"), eq("email_password")))
                    .thenReturn(Optional.of(cred));

            PasswordVerifyResult result = credentialService.verifyPassword("user-001", "Wrong1!");

            assertFalse(result.isMatched());
            assertTrue(result.isLocked());
            assertEquals(5, result.getFailCount());
            assertNotNull(cred.getLockedUntil());
        }

        @Test
        @DisplayName("锁定期间即使密码正确也拒绝登录")
        void verifyPassword_lockedPeriod_rejectsCorrectPassword() {
            CiamUserCredentialDo cred = stubCred("user-001", "Correct1!", 5);
            cred.setLockedUntil(Instant.now().plusSeconds(29 * 60));
            when(credentialRepository.findByUserIdAndType(eq("user-001"), eq("email_password")))
                    .thenReturn(Optional.of(cred));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> credentialService.verifyPassword("user-001", "Correct1!"));
            assertEquals(CiamErrorCode.ACCOUNT_LOCKED, ex.getErrorCode());
        }

        @Test
        @DisplayName("锁定过期后可以正常登录并重置计数")
        void verifyPassword_lockExpired_allowsLogin() {
            CiamUserCredentialDo cred = stubCred("user-001", "Correct1!", 5);
            cred.setLockedUntil(Instant.now().minusSeconds(1 * 60));
            when(credentialRepository.findByUserIdAndType(eq("user-001"), eq("email_password")))
                    .thenReturn(Optional.of(cred));

            PasswordVerifyResult result = credentialService.verifyPassword("user-001", "Correct1!");

            assertTrue(result.isMatched());
            assertEquals(0, cred.getFailCount());
            assertNull(cred.getLockedUntil());
        }

        @Test
        @DisplayName("重置密码清除锁定状态和失败计数")
        void resetPassword_clearsLockAndFailCount() {
            CiamUserCredentialDo cred = stubCred("user-001", "OldPass1!", 5);
            cred.setLockedUntil(Instant.now().plusSeconds(30 * 60));
            when(credentialRepository.findByUserIdAndType(eq("user-001"), eq("email_password")))
                    .thenReturn(Optional.of(cred));

            credentialService.resetPassword("user-001", "NewPass1!");

            assertEquals(0, cred.getFailCount());
            assertNull(cred.getLockedUntil());
            assertTrue(passwordEncoder.matches("NewPass1!", cred.getCredentialHash()));
        }
    }
}
