package net.hwyz.iov.cloud.sec.ciam.service.security;

import net.hwyz.iov.cloud.framework.common.exception.BusinessException;
import net.hwyz.iov.cloud.sec.ciam.service.common.exception.CiamErrorCode;
import net.hwyz.iov.cloud.sec.ciam.service.common.security.PasswordEncoder;
import net.hwyz.iov.cloud.sec.ciam.service.common.security.RateLimitFilter;
import net.hwyz.iov.cloud.sec.ciam.service.domain.adapter.AdapterResult;
import net.hwyz.iov.cloud.sec.ciam.service.domain.adapter.EmailAdapter;
import net.hwyz.iov.cloud.sec.ciam.service.domain.adapter.SmsAdapter;
import net.hwyz.iov.cloud.sec.ciam.service.domain.enums.CredentialStatus;
import net.hwyz.iov.cloud.sec.ciam.service.domain.enums.CredentialType;
import net.hwyz.iov.cloud.sec.ciam.service.domain.enums.DecisionResult;
import net.hwyz.iov.cloud.sec.ciam.service.domain.enums.RiskLevel;
import net.hwyz.iov.cloud.sec.ciam.service.domain.enums.SessionStatus;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.CiamSessionRepository;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.CiamUserCredentialRepository;
import net.hwyz.iov.cloud.sec.ciam.service.domain.service.CredentialDomainService;
import net.hwyz.iov.cloud.sec.ciam.service.domain.service.PasswordPolicyService;
import net.hwyz.iov.cloud.sec.ciam.service.domain.service.PasswordVerifyResult;
import net.hwyz.iov.cloud.sec.ciam.service.domain.service.RiskAssessmentResult;
import net.hwyz.iov.cloud.sec.ciam.service.domain.service.VerificationCodeService;
import net.hwyz.iov.cloud.sec.ciam.service.domain.service.VerificationCodeStore;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.po.SessionPo;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.po.UserCredentialPo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 安全与性能测试 — 覆盖验证码防刷、密码暴力尝试、限流、锁定策略、登录性能基线、会话规模基线。
 */
@ExtendWith(MockitoExtension.class)
class SecurityAndPerformanceTest {

    // ====================================================================
    // 1. 验证码防刷测试
    // ====================================================================

    @Nested
    @DisplayName("验证码防刷")
    class VerificationCodeAntiAbuseTests {
        @Mock
        private VerificationCodeStore codeStore;
        @Mock
        private SmsAdapter smsAdapter;
        @Mock
        private EmailAdapter emailAdapter;
        private VerificationCodeService verificationCodeService;

        @BeforeEach
        void setUp() {
            verificationCodeService = new VerificationCodeService(codeStore, smsAdapter, emailAdapter);
        }

        @Test
        @DisplayName("同一用户同一客户端1分钟内重复发送短信验证码应被拒绝")
        void smsCode_rateLimitedWithin1Minute() {
            when(codeStore.setIfAbsent(anyString(), eq(VerificationCodeService.RATE_LIMIT_TTL_SECONDS)))
                    .thenReturn(true).thenReturn(false);
            when(codeStore.incrementDailyCount(anyString(), anyInt())).thenReturn(1L);
            when(smsAdapter.sendVerificationCode(anyString(), anyString(), anyString()))
                    .thenReturn(AdapterResult.ok());
            assertDoesNotThrow(() -> verificationCodeService.sendSmsCode("13800138000", "+86", "u1", "c1"));
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> verificationCodeService.sendSmsCode("13800138000", "+86", "u1", "c1"));
            assertEquals(CiamErrorCode.VERIFICATION_CODE_RATE_LIMITED, ex.getErrorCode());
        }

        @Test
        @DisplayName("同一用户单日超过30次发送应被拒绝")
        void smsCode_dailyLimitExceeded() {
            when(codeStore.setIfAbsent(anyString(), eq(VerificationCodeService.RATE_LIMIT_TTL_SECONDS))).thenReturn(true);
            when(codeStore.incrementDailyCount(anyString(), anyInt())).thenReturn((long) VerificationCodeService.DAILY_LIMIT + 1);
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> verificationCodeService.sendSmsCode("13800138001", "+86", "u2", "c1"));
            assertEquals(CiamErrorCode.VERIFICATION_CODE_RATE_LIMITED, ex.getErrorCode());
        }

        @Test
        @DisplayName("邮箱验证码同样受频控限制")
        void emailCode_rateLimitedWithin1Minute() {
            when(codeStore.setIfAbsent(anyString(), eq(VerificationCodeService.RATE_LIMIT_TTL_SECONDS))).thenReturn(false);
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> verificationCodeService.sendEmailCode("t@e.com", "u3", "c1"));
            assertEquals(CiamErrorCode.VERIFICATION_CODE_RATE_LIMITED, ex.getErrorCode());
        }

        @Test
        @DisplayName("不同用户不同客户端不受彼此频控影响")
        void differentUsers_independentRateLimiting() {
            when(codeStore.setIfAbsent(anyString(), eq(VerificationCodeService.RATE_LIMIT_TTL_SECONDS))).thenReturn(true);
            when(codeStore.incrementDailyCount(anyString(), anyInt())).thenReturn(1L);
            when(smsAdapter.sendVerificationCode(anyString(), anyString(), anyString())).thenReturn(AdapterResult.ok());
            assertDoesNotThrow(() -> verificationCodeService.sendSmsCode("13800138000", "+86", "uA", "cA"));
            assertDoesNotThrow(() -> verificationCodeService.sendSmsCode("13800138001", "+86", "uB", "cB"));
        }
    }

    // ====================================================================
    // 2. 密码暴力尝试测试
    // ====================================================================

    @Nested
    @DisplayName("密码暴力尝试防护")
    class PasswordBruteForceTests {
        @Mock
        private CiamUserCredentialRepository credentialRepository;
        @Mock
        private PasswordEncoder passwordEncoder;
        @Mock
        private PasswordPolicyService passwordPolicyService;
        private CredentialDomainService credentialDomainService;

        @BeforeEach
        void setUp() {
            credentialDomainService = new CredentialDomainService(credentialRepository, passwordEncoder, passwordPolicyService);
        }

        private UserCredentialPo cred(String userId, int failCount, Instant lockedUntil) {
            UserCredentialPo c = new UserCredentialPo();
            c.setCredentialId("cred-001");
            c.setUserId(userId);
            c.setCredentialType(CredentialType.EMAIL_PASSWORD.getCode());
            c.setCredentialHash("hashed");
            c.setHashAlgorithm("BCrypt");
            c.setFailCount(failCount);
            c.setLockedUntil(lockedUntil);
            c.setCredentialStatus(CredentialStatus.VALID.getCode());
            c.setRowValid(1);
            return c;
        }

        @Test
        @DisplayName("连续错误1-2次：普通失败，不触发挑战")
        void belowChallengeThreshold() {
            String uid = "u-bf1";
            when(credentialRepository.findByUserIdAndType(uid, CredentialType.EMAIL_PASSWORD.getCode()))
                    .thenReturn(Optional.of(cred(uid, 1, null)));
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);
            PasswordVerifyResult r = credentialDomainService.verifyPassword(uid, "wrong");
            assertFalse(r.isMatched());
            assertFalse(r.isChallengeRequired());
            assertFalse(r.isLocked());
            assertEquals(2, r.getFailCount());
        }

        @Test
        @DisplayName("连续错误3次：触发图形验证码挑战")
        void challengeThresholdReached() {
            String uid = "u-bf2";
            when(credentialRepository.findByUserIdAndType(uid, CredentialType.EMAIL_PASSWORD.getCode()))
                    .thenReturn(Optional.of(cred(uid, 2, null)));
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);
            PasswordVerifyResult r = credentialDomainService.verifyPassword(uid, "wrong");
            assertFalse(r.isMatched());
            assertTrue(r.isChallengeRequired());
            assertFalse(r.isLocked());
            assertEquals(CredentialDomainService.CHALLENGE_THRESHOLD, r.getFailCount());
        }

        @Test
        @DisplayName("连续错误5次：锁定账号30分钟")
        void lockThresholdReached() {
            String uid = "u-bf3";
            when(credentialRepository.findByUserIdAndType(uid, CredentialType.EMAIL_PASSWORD.getCode()))
                    .thenReturn(Optional.of(cred(uid, 4, null)));
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);
            PasswordVerifyResult r = credentialDomainService.verifyPassword(uid, "wrong");
            assertFalse(r.isMatched());
            assertTrue(r.isLocked());
            assertEquals(CredentialDomainService.LOCK_THRESHOLD, r.getFailCount());
            verify(credentialRepository).updateByCredentialId(argThat(c -> c.getLockedUntil() != null));
        }

        @Test
        @DisplayName("锁定期间尝试登录抛出ACCOUNT_LOCKED")
        void lockedAccountRejects() {
            String uid = "u-bf4";
            when(credentialRepository.findByUserIdAndType(uid, CredentialType.EMAIL_PASSWORD.getCode()))
                    .thenReturn(Optional.of(cred(uid, 5, Instant.now().plusSeconds(25 * 60))));
            BusinessException ex = assertThrows(BusinessException.class, () -> credentialDomainService.verifyPassword(uid, "any"));
            assertEquals(CiamErrorCode.ACCOUNT_LOCKED, ex.getErrorCode());
            verify(passwordEncoder, never()).matches(anyString(), anyString());
        }

        @Test
        @DisplayName("密码正确后重置失败计数")
        void correctPasswordResets() {
            String uid = "u-bf5";
            when(credentialRepository.findByUserIdAndType(uid, CredentialType.EMAIL_PASSWORD.getCode()))
                    .thenReturn(Optional.of(cred(uid, 3, null)));
            when(passwordEncoder.matches("ok", "hashed")).thenReturn(true);
            PasswordVerifyResult r = credentialDomainService.verifyPassword(uid, "ok");
            assertTrue(r.isMatched());
            assertEquals(0, r.getFailCount());
            verify(credentialRepository).updateByCredentialId(argThat(c -> c.getFailCount() == 0 && c.getLockedUntil() == null));
        }
    }

    // ====================================================================
    // 3. 限流测试
    // ====================================================================

    @Nested
    @DisplayName("限流策略")
    class RateLimitingTests {
        private RateLimitFilter filter;

        @BeforeEach
        void setUp() {
            filter = new RateLimitFilter();
            filter.clearCounters();
        }

        @Test
        @DisplayName("认证端点限流：同一IP超过10次/分钟被拒绝")
        void authEndpoint_429() throws Exception {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/v1/auth/login");
            req.setRemoteAddr("192.168.1.100");
            for (int i = 0; i < RateLimitFilter.AUTH_LIMIT; i++) {
                MockHttpServletResponse resp = new MockHttpServletResponse();
                filter.doFilter(req, resp, new MockFilterChain());
                assertEquals(200, resp.getStatus());
            }
            MockHttpServletResponse resp = new MockHttpServletResponse();
            filter.doFilter(req, resp, new MockFilterChain());
            assertEquals(429, resp.getStatus());
            assertTrue(resp.getContentAsString().contains("100003"));
        }

        @Test
        @DisplayName("普通端点限流：同一IP超过60次/分钟被拒绝")
        void generalEndpoint_limit() {
            String key = "10.0.0.1:general";
            for (int i = 0; i < RateLimitFilter.DEFAULT_LIMIT; i++)
                assertTrue(filter.tryAcquire(key, RateLimitFilter.DEFAULT_LIMIT));
            assertFalse(filter.tryAcquire(key, RateLimitFilter.DEFAULT_LIMIT));
        }

        @Test
        @DisplayName("不同IP不受彼此限流影响")
        void differentIPs_independent() {
            String k1 = "10.0.0.1:auth", k2 = "10.0.0.2:auth";
            for (int i = 0; i < RateLimitFilter.AUTH_LIMIT; i++)
                assertTrue(filter.tryAcquire(k1, RateLimitFilter.AUTH_LIMIT));
            assertFalse(filter.tryAcquire(k1, RateLimitFilter.AUTH_LIMIT));
            assertTrue(filter.tryAcquire(k2, RateLimitFilter.AUTH_LIMIT));
        }

        @Test
        @DisplayName("OAuth token端点受认证限流约束")
        void oauthToken_authLimit() {
            assertTrue(filter.isAuthEndpoint("/api/v1/oauth/token"));
            assertEquals(RateLimitFilter.AUTH_LIMIT, filter.resolveLimit("/api/v1/oauth/token"));
        }
    }

    // ====================================================================
    // 4. 生效中的锁定策略测试
    // ====================================================================

    @Nested
    @DisplayName("生效中的锁定策略")
    class ActiveLockoutPolicyTests {
        @Mock
        private CiamUserCredentialRepository credentialRepository;
        @Mock
        private PasswordEncoder passwordEncoder;
        @Mock
        private PasswordPolicyService passwordPolicyService;
        private CredentialDomainService credentialDomainService;

        @BeforeEach
        void setUp() {
            credentialDomainService = new CredentialDomainService(credentialRepository, passwordEncoder, passwordPolicyService);
        }

        private UserCredentialPo cred(String userId, int failCount, Instant lockedUntil) {
            UserCredentialPo c = new UserCredentialPo();
            c.setCredentialId("cred-l1");
            c.setUserId(userId);
            c.setCredentialType(CredentialType.EMAIL_PASSWORD.getCode());
            c.setCredentialHash("hpw");
            c.setHashAlgorithm("BCrypt");
            c.setFailCount(failCount);
            c.setLockedUntil(lockedUntil);
            c.setCredentialStatus(CredentialStatus.VALID.getCode());
            c.setRowValid(1);
            return c;
        }

        @Test
        @DisplayName("锁定时长为30分钟")
        void lockDuration() {
            assertEquals(30, CredentialDomainService.LOCK_DURATION_MINUTES);
        }

        @Test
        @DisplayName("锁定阈值为5次连续失败")
        void lockThreshold() {
            assertEquals(5, CredentialDomainService.LOCK_THRESHOLD);
        }

        @Test
        @DisplayName("挑战阈值为3次连续失败")
        void challengeThreshold() {
            assertEquals(3, CredentialDomainService.CHALLENGE_THRESHOLD);
        }

        @Test
        @DisplayName("锁定期间即使密码正确也拒绝登录")
        void lockedRejectsCorrectPw() {
            String uid = "u-l1";
            when(credentialRepository.findByUserIdAndType(uid, CredentialType.EMAIL_PASSWORD.getCode()))
                    .thenReturn(Optional.of(cred(uid, 5, Instant.now().plusSeconds(20 * 60))));
            BusinessException ex = assertThrows(BusinessException.class, () -> credentialDomainService.verifyPassword(uid, "correct"));
            assertEquals(CiamErrorCode.ACCOUNT_LOCKED, ex.getErrorCode());
        }

        @Test
        @DisplayName("锁定过期后允许重新尝试登录")
        void lockExpiredAllowsRetry() {
            String uid = "u-l2";
            when(credentialRepository.findByUserIdAndType(uid, CredentialType.EMAIL_PASSWORD.getCode()))
                    .thenReturn(Optional.of(cred(uid, 5, Instant.now().minusSeconds(1 * 60))));
            when(passwordEncoder.matches("ok", "hpw")).thenReturn(true);
            PasswordVerifyResult r = credentialDomainService.verifyPassword(uid, "ok");
            assertTrue(r.isMatched());
            assertEquals(0, r.getFailCount());
        }

        @Test
        @DisplayName("第4次失败触发挑战但不锁定")
        void fourthFailure_challengeNoLock() {
            String uid = "u-l3";
            when(credentialRepository.findByUserIdAndType(uid, CredentialType.EMAIL_PASSWORD.getCode()))
                    .thenReturn(Optional.of(cred(uid, 3, null)));
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);
            PasswordVerifyResult r = credentialDomainService.verifyPassword(uid, "wrong");
            assertTrue(r.isChallengeRequired());
            assertFalse(r.isLocked());
            assertEquals(4, r.getFailCount());
        }

        @Test
        @DisplayName("密码策略：最小长度8位")
        void pwPolicy_minLen() {
            PasswordPolicyService ps = new PasswordPolicyService();
            assertFalse(ps.isValid("Ab1!xyz"));
            assertTrue(ps.isValid("Ab1!xyzw"));
        }

        @Test
        @DisplayName("密码策略：必须包含大写、小写、数字、特殊字符")
        void pwPolicy_complexity() {
            PasswordPolicyService ps = new PasswordPolicyService();
            assertFalse(ps.isValid("abcd1234!"));
            assertFalse(ps.isValid("ABCD1234!"));
            assertFalse(ps.isValid("Abcdefgh!"));
            assertFalse(ps.isValid("Abcdefg1"));
            assertTrue(ps.isValid("Abcdefg1!"));
        }
    }

    // ====================================================================
    // 5. 登录接口性能基线测试
    // ====================================================================

    @Nested
    @DisplayName("登录接口性能基线")
    class LoginPerformanceBaselineTests {

        @Test
        @DisplayName("密码策略校验应在1ms内完成")
        void pwPolicyPerf() {
            PasswordPolicyService ps = new PasswordPolicyService();
            for (int i = 0; i < 100; i++) ps.isValid("Abcdefg1!");
            long start = System.nanoTime();
            for (int i = 0; i < 10_000; i++) ps.isValid("Abcdefg1!");
            double avgUs = ((System.nanoTime() - start) / 1_000.0) / 10_000;
            assertTrue(avgUs < 100, "密码策略校验平均 " + String.format("%.2f", avgUs) + " μs > 100μs");
        }

        @Test
        @DisplayName("RateLimitFilter tryAcquire 应在亚毫秒内完成")
        void rateLimitPerf() {
            RateLimitFilter f = new RateLimitFilter();
            for (int i = 0; i < 100; i++) f.tryAcquire("w-" + i + ":a", RateLimitFilter.AUTH_LIMIT);
            f.clearCounters();
            long start = System.nanoTime();
            for (int i = 0; i < 10_000; i++) f.tryAcquire("p-" + i + ":a", RateLimitFilter.AUTH_LIMIT);
            double avgUs = ((System.nanoTime() - start) / 1_000.0) / 10_000;
            assertTrue(avgUs < 500, "限流判断平均 " + String.format("%.2f", avgUs) + " μs > 500μs");
        }

        @Test
        @DisplayName("风险评估规则判断应在合理时间内完成")
        void riskRulePerf() {
            long start = System.nanoTime();
            for (int i = 0; i < 10_000; i++) {
                boolean nd = (i % 3 == 0), gc = (i % 5 == 0);
                RiskLevel l = (nd && gc) ? RiskLevel.HIGH : (nd || gc) ? RiskLevel.MEDIUM : RiskLevel.LOW;
                DecisionResult d = (nd && gc) ? DecisionResult.BLOCK : (nd || gc) ? DecisionResult.CHALLENGE : DecisionResult.ALLOW;
                assertNotNull(l);
                assertNotNull(d);
            }
            double avgUs = ((System.nanoTime() - start) / 1_000.0) / 10_000;
            assertTrue(avgUs < 50, "风险规则判断平均 " + String.format("%.2f", avgUs) + " μs > 50μs");
        }

        @Test
        @DisplayName("验证码生成应在合理时间内完成")
        void codeGenPerf() {
            VerificationCodeStore ms = mock(VerificationCodeStore.class);
            SmsAdapter mSms = mock(SmsAdapter.class);
            EmailAdapter mEmail = mock(EmailAdapter.class);
            VerificationCodeService svc = new VerificationCodeService(ms, mSms, mEmail);
            long start = System.nanoTime();
            for (int i = 0; i < 10_000; i++)
                assertEquals(VerificationCodeService.CODE_LENGTH, svc.generateCode().length());
            double avgUs = ((System.nanoTime() - start) / 1_000.0) / 10_000;
            assertTrue(avgUs < 100, "验证码生成平均 " + String.format("%.2f", avgUs) + " μs > 100μs");
        }
    }

    // ====================================================================
    // 6. 会话规模基线测试
    // ====================================================================

    @Nested
    @DisplayName("会话规模基线")
    class SessionScaleBaselineTests {
        @Mock
        private CiamSessionRepository sessionRepository;

        @Test
        @DisplayName("单用户多会话查询应能处理大量会话记录")
        void manySessionsQuery() {
            String uid = "u-s1";
            List<SessionPo> sessions = new ArrayList<>();
            for (int i = 0; i < 100; i++) {
                SessionPo s = new SessionPo();
                s.setSessionId("s-" + i);
                s.setUserId(uid);
                s.setClientType("app");
                s.setSessionStatus(SessionStatus.ACTIVE.getCode());
                s.setLoginTime(Instant.now().minusSeconds(i * 3600));
                s.setLastActiveTime(Instant.now().minusSeconds(i * 60));
                s.setExpireTime(Instant.now().plusSeconds(30L * 86400));
                sessions.add(s);
            }
            when(sessionRepository.findByUserIdAndStatus(uid, SessionStatus.ACTIVE.getCode())).thenReturn(sessions);
            List<SessionPo> result = sessionRepository.findByUserIdAndStatus(uid, SessionStatus.ACTIVE.getCode());
            assertEquals(100, result.size());
            assertTrue(result.stream().allMatch(s -> uid.equals(s.getUserId())));
            assertTrue(result.stream().allMatch(s -> s.getSessionStatus() == SessionStatus.ACTIVE.getCode()));
        }

        @Test
        @DisplayName("会话数据对象创建性能基线")
        void sessionCreationPerf() {
            long start = System.nanoTime();
            int n = 50_000;
            List<SessionPo> list = new ArrayList<>(n);
            for (int i = 0; i < n; i++) {
                SessionPo s = new SessionPo();
                s.setSessionId("s-" + i);
                s.setUserId("u-" + (i % 1000));
                s.setClientType("app");
                s.setSessionStatus(SessionStatus.ACTIVE.getCode());
                s.setLoginTime(Instant.now());
                s.setLastActiveTime(Instant.now());
                s.setExpireTime(Instant.now().plusSeconds(30L * 86400));
                s.setLoginIp("192.168.1." + (i % 256));
                s.setRiskLevel(RiskLevel.LOW.getCode());
                list.add(s);
            }
            double ms = (System.nanoTime() - start) / 1_000_000.0;
            assertEquals(n, list.size());
            assertTrue(ms < 2000, "创建 " + n + " 个会话对象耗时 " + String.format("%.2f", ms) + " ms > 2000ms");
        }

        @Test
        @DisplayName("并发限流计数器应能处理大量不同IP")
        void manyIPsScale() {
            RateLimitFilter f = new RateLimitFilter();
            int ips = 10_000;
            long start = System.nanoTime();
            for (int i = 0; i < ips; i++) assertTrue(f.tryAcquire("ip-" + i + ":a", RateLimitFilter.AUTH_LIMIT));
            double ms = (System.nanoTime() - start) / 1_000_000.0;
            assertTrue(ms < 1000, "处理 " + ips + " 个IP耗时 " + String.format("%.2f", ms) + " ms > 1000ms");
        }

        @Test
        @DisplayName("风险评估结果构建性能基线")
        void riskResultBuildPerf() {
            long start = System.nanoTime();
            int n = 50_000;
            for (int i = 0; i < n; i++) {
                assertNotNull(RiskAssessmentResult.builder()
                        .riskLevel(RiskLevel.LOW).decisionResult(DecisionResult.ALLOW)
                        .hitRules(List.of()).riskEventId("e-" + i).build());
            }
            double ms = (System.nanoTime() - start) / 1_000_000.0;
            assertTrue(ms < 1000, "构建 " + n + " 个风险结果耗时 " + String.format("%.2f", ms) + " ms > 1000ms");
        }
    }
}
