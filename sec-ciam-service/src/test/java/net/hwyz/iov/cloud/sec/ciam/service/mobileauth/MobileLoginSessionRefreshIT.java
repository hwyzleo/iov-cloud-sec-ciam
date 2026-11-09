package net.hwyz.iov.cloud.sec.ciam.service.mobileauth;

import net.hwyz.iov.cloud.framework.common.exception.BusinessException;
import net.hwyz.iov.cloud.sec.ciam.api.vo.SessionValidateResult;
import net.hwyz.iov.cloud.sec.ciam.api.vo.TokenVerifyResult;
import net.hwyz.iov.cloud.sec.ciam.service.application.dto.DeviceInfoDto;
import net.hwyz.iov.cloud.sec.ciam.service.application.dto.LoginResultDto;
import net.hwyz.iov.cloud.sec.ciam.service.application.dto.cmd.LoginByMobileCodeCmd;
import net.hwyz.iov.cloud.sec.ciam.service.application.service.AuthenticationAppService;
import net.hwyz.iov.cloud.sec.ciam.service.application.service.SessionAppService;
import net.hwyz.iov.cloud.sec.ciam.service.application.service.TokenAppService;
import net.hwyz.iov.cloud.sec.ciam.service.common.exception.CiamErrorCode;
import net.hwyz.iov.cloud.sec.ciam.service.common.security.FieldEncryptor;
import net.hwyz.iov.cloud.sec.ciam.service.common.security.TokenDigest;
import net.hwyz.iov.cloud.sec.ciam.service.domain.enums.SessionStatus;
import net.hwyz.iov.cloud.sec.ciam.service.domain.enums.TokenStatus;
import net.hwyz.iov.cloud.sec.ciam.service.domain.service.JwtTokenService;
import net.hwyz.iov.cloud.sec.ciam.service.domain.service.TokenClaims;
import net.hwyz.iov.cloud.sec.ciam.service.domain.service.VerificationCodeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 手机登录 / 会话 / 刷新 / 登出 真实持久化集成测试
 * （SEC-CIAM-DSN-CR-003 TC-003-2/5/6/7/8/9/14）。
 * <p>
 * 证据要求（US-003-8）：使用真实 MySQL 与真实 Redis，不 Mock Repository。
 */
@SpringBootTest(classes = CiamMobileAuthItTestApplication.class,
        properties = {
                "ciam.authentication.mobile.allowed-client-ids=ciam-mobile-app",
                "ciam.authentication.verification-code.fixed-enabled=true"
        })
class MobileLoginSessionRefreshIT extends CiamMobileAuthItBase {

    private static final String CLIENT_ID = "ciam-mobile-app";
    private static final String SEEDED_SCOPE = "openid profile phone email";
    private static final long SEEDED_REFRESH_TTL = 2592000L;

    @Autowired
    private AuthenticationAppService authenticationAppService;
    @Autowired
    private VerificationCodeService verificationCodeService;
    @Autowired
    private JwtTokenService jwtTokenService;
    @Autowired
    private SessionAppService sessionAppService;
    @Autowired
    private TokenAppService tokenAppService;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    // ---- helpers ----

    private String uniqueMobile() {
        return "139" + String.format("%08d", Math.abs(System.nanoTime() % 100_000_000));
    }

    private String uniqueDevice() {
        return "DEV-" + UUID.randomUUID();
    }

    private void sendCode(String mobile, String deviceId) {
        verificationCodeService.sendSmsCode(mobile, "+86", FieldEncryptor.hash(mobile), deviceId);
    }

    private LoginResultDto login(String mobile, String deviceId) {
        sendCode(mobile, deviceId);
        return authenticationAppService.loginByMobileCode(LoginByMobileCodeCmd.builder()
                .mobile(mobile)
                .countryCode("+86")
                .code("111111")
                .deviceId(deviceId)
                .deviceInfo(DeviceInfoDto.builder()
                        .clientId(CLIENT_ID)
                        .clientType("MOBILE")
                        .deviceId(deviceId)
                        .deviceOs("iOS")
                        .appVersion("1.0.0")
                        .build())
                .build());
    }

    private Map<String, Object> session(String sessionId) {
        return jdbcTemplate.queryForMap(
                "SELECT * FROM ciam_session WHERE session_id = ?", sessionId);
    }

    private List<Map<String, Object>> refreshTokens(String sessionId) {
        return jdbcTemplate.queryForList(
                "SELECT * FROM ciam_refresh_token WHERE session_id = ? ORDER BY id", sessionId);
    }

    private static Instant instant(Object timestamp) {
        return ((Timestamp) timestamp).toInstant();
    }

    // ---- TC-003-5 / TC-003-4 ----

    @Test
    @DisplayName("TC-003-5 登录成功后持久化 ACTIVE 会话，各标识与 scope 一致，Token 携带完整声明")
    void login_persistsActiveSessionWithFrozenContext() {
        String mobile = uniqueMobile();
        String deviceId = uniqueDevice();

        LoginResultDto result = login(mobile, deviceId);

        assertNotNull(result.getAccessToken());
        assertNotNull(result.getRefreshToken());
        assertNotNull(result.getSessionId());
        assertTrue(result.isNewUser());
        assertEquals(1800, result.getAccessTokenTtl());

        Map<String, Object> row = session(result.getSessionId());
        assertEquals(result.getUserId(), row.get("user_id"));
        assertEquals(CLIENT_ID, row.get("client_id"));
        assertEquals("MOBILE", row.get("client_type"));
        assertEquals(deviceId, row.get("device_id"));
        assertEquals(SEEDED_SCOPE, row.get("scope"));
        assertEquals(SessionStatus.ACTIVE.getCode(), ((Number) row.get("session_status")).intValue());
        assertNotNull(row.get("login_time"));
        assertNotNull(row.get("last_active_time"));

        Instant expireTime = instant(row.get("expire_time"));
        long ttl = expireTime.getEpochSecond() - Instant.now().getEpochSecond();
        assertTrue(Math.abs(ttl - SEEDED_REFRESH_TTL) < 120,
                "会话 expire_time 应为登录时间 + 客户端 refreshTokenTtl，实际剩余 " + ttl);

        List<Map<String, Object>> tokens = refreshTokens(result.getSessionId());
        assertEquals(1, tokens.size());
        Map<String, Object> token = tokens.get(0);
        assertEquals(TokenStatus.ACTIVE.getCode(), ((Number) token.get("token_status")).intValue());
        assertEquals(CLIENT_ID, token.get("client_id"));
        // Refresh Token 与会话共享同一绝对到期边界
        assertEquals(expireTime.getEpochSecond(), instant(token.get("expire_time")).getEpochSecond());

        TokenClaims claims = jwtTokenService.validateAccessToken(result.getAccessToken());
        assertEquals(result.getUserId(), claims.getSub());
        assertEquals(CLIENT_ID, claims.getClientId());
        assertEquals("MOBILE", claims.getClientType());
        assertEquals(deviceId, claims.getDeviceId());
        assertEquals(result.getSessionId(), claims.getSessionId());
        assertEquals(SEEDED_SCOPE, claims.getScope());

        SessionValidateResult validate = sessionAppService.validateSession(result.getSessionId());
        assertTrue(validate.isValid());
        assertEquals(result.getUserId(), validate.getUserId());

        TokenVerifyResult verify = tokenAppService.verifyToken(result.getAccessToken());
        assertEquals("MOBILE", verify.getClaims().get("client_type"));
    }

    // ---- TC-003-2 ----

    @Test
    @DisplayName("TC-003-2 真实 MySQL 并发首次登录：仅一个有效身份，无重复用户/资料")
    void concurrentFirstLogin_createsSingleIdentityAndUser() throws Exception {
        String mobile = uniqueMobile();
        String deviceA = uniqueDevice();
        String deviceB = uniqueDevice();
        sendCode(mobile, deviceA);
        sendCode(mobile, deviceB);

        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        Callable<Object> taskA = () -> {
            start.await();
            try {
                return loginWithoutSend(mobile, deviceA);
            } catch (BusinessException e) {
                return e.getErrorCode();
            }
        };
        Callable<Object> taskB = () -> {
            start.await();
            try {
                return loginWithoutSend(mobile, deviceB);
            } catch (BusinessException e) {
                return e.getErrorCode();
            }
        };

        Future<Object> a = pool.submit(taskA);
        Future<Object> b = pool.submit(taskB);
        start.countDown();
        Object resultA = a.get(60, TimeUnit.SECONDS);
        Object resultB = b.get(60, TimeUnit.SECONDS);
        pool.shutdownNow();

        String hash = FieldEncryptor.hash(mobile);
        Integer identities = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ciam_user_identity WHERE identity_hash = ? AND row_valid = 1",
                Integer.class, hash);
        assertEquals(1, identities, "并发首次登录不得产生重复手机标识");

        String userId = jdbcTemplate.queryForObject(
                "SELECT user_id FROM ciam_user_identity WHERE identity_hash = ? AND row_valid = 1",
                String.class, hash);
        Integer users = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ciam_user WHERE user_id = ? AND row_valid = 1",
                Integer.class, userId);
        assertEquals(1, users);
        Integer profiles = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ciam_user_profile WHERE user_id = ? AND row_valid = 1",
                Integer.class, userId);
        assertEquals(1, profiles, "失败事务必须回滚临时用户资料");

        // 竞争失败方必须是 IDENTITY_CONFLICT 或成功登录，且不得返回部分凭据
        assertTrue(resultA instanceof LoginResultDto || CiamErrorCode.IDENTITY_CONFLICT.equals(resultA),
                "并发失败方必须是 IDENTITY_CONFLICT，实际: " + resultA);
        assertTrue(resultB instanceof LoginResultDto || CiamErrorCode.IDENTITY_CONFLICT.equals(resultB),
                "并发失败方必须是 IDENTITY_CONFLICT，实际: " + resultB);
        // 成功方的会话必须真实存在
        for (Object result : List.of(resultA, resultB)) {
            if (result instanceof LoginResultDto dto) {
                assertEquals(1, refreshTokens(dto.getSessionId()).size());
            }
        }
    }

    private LoginResultDto loginWithoutSend(String mobile, String deviceId) {
        return authenticationAppService.loginByMobileCode(LoginByMobileCodeCmd.builder()
                .mobile(mobile)
                .countryCode("+86")
                .code("111111")
                .deviceId(deviceId)
                .deviceInfo(DeviceInfoDto.builder()
                        .clientId(CLIENT_ID)
                        .clientType("MOBILE")
                        .deviceId(deviceId)
                        .build())
                .build());
    }

    // ---- TC-003-6 ----

    @Test
    @DisplayName("TC-003-6 刷新恢复完整上下文，父令牌 ROTATED，子令牌不延后到期")
    void refresh_rotatesAndRestoresContextWithoutExtendingExpiry() {
        String mobile = uniqueMobile();
        String deviceId = uniqueDevice();
        LoginResultDto login = login(mobile, deviceId);

        LoginResultDto refreshed = authenticationAppService.refreshToken(login.getRefreshToken(), CLIENT_ID);

        assertEquals(login.getSessionId(), refreshed.getSessionId());
        assertFalse(refreshed.isNewUser());
        assertNotEquals(login.getRefreshToken(), refreshed.getRefreshToken());

        List<Map<String, Object>> tokens = refreshTokens(login.getSessionId());
        assertEquals(2, tokens.size());
        Map<String, Object> parent = tokens.get(0);
        Map<String, Object> child = tokens.get(1);
        assertEquals(TokenStatus.ROTATED.getCode(), ((Number) parent.get("token_status")).intValue());
        assertNotNull(parent.get("used_time"));
        assertEquals(TokenStatus.ACTIVE.getCode(), ((Number) child.get("token_status")).intValue());
        assertEquals(parent.get("refresh_token_id"), child.get("parent_token_id"));
        assertEquals(instant(parent.get("expire_time")).getEpochSecond(),
                instant(child.get("expire_time")).getEpochSecond(),
                "子令牌必须继承绝对到期边界");

        TokenClaims claims = jwtTokenService.validateAccessToken(refreshed.getAccessToken());
        assertEquals("MOBILE", claims.getClientType());
        assertEquals(deviceId, claims.getDeviceId());
        assertEquals(SEEDED_SCOPE, claims.getScope());
        assertEquals(login.getSessionId(), claims.getSessionId());
        assertTrue(claims.getExp().getEpochSecond()
                        <= instant(child.get("expire_time")).getEpochSecond(),
                "Access Token exp 不得超过会话绝对到期");
    }

    // ---- TC-003-7 ----

    @Test
    @DisplayName("TC-003-7 已轮换令牌重放：撤销会话令牌链并拒绝")
    void replayOfRotatedToken_revokesChain() {
        String mobile = uniqueMobile();
        String deviceId = uniqueDevice();
        LoginResultDto login = login(mobile, deviceId);
        authenticationAppService.refreshToken(login.getRefreshToken(), CLIENT_ID);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authenticationAppService.refreshToken(login.getRefreshToken(), CLIENT_ID));
        assertEquals(CiamErrorCode.TOKEN_INVALID, ex.getErrorCode());

        List<Map<String, Object>> tokens = refreshTokens(login.getSessionId());
        assertTrue(tokens.stream().noneMatch(t ->
                        ((Number) t.get("token_status")).intValue() == TokenStatus.ACTIVE.getCode()),
                "重放后该会话不得存在 ACTIVE Refresh Token");
    }

    @Test
    @DisplayName("TC-003-7 真实数据库并发刷新：仅一个请求成功，无多个有效子令牌")
    void concurrentRefresh_allowsSingleWinner() throws Exception {
        String mobile = uniqueMobile();
        String deviceId = uniqueDevice();
        LoginResultDto login = login(mobile, deviceId);
        String rawToken = login.getRefreshToken();

        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        Callable<String> refresh = () -> {
            start.await();
            try {
                authenticationAppService.refreshToken(rawToken, CLIENT_ID);
                return "OK";
            } catch (BusinessException e) {
                return e.getErrorCode().getCode();
            }
        };
        Future<String> first = pool.submit(refresh);
        Future<String> second = pool.submit(refresh);
        start.countDown();
        String r1 = first.get(60, TimeUnit.SECONDS);
        String r2 = second.get(60, TimeUnit.SECONDS);
        pool.shutdownNow();

        long successCount = List.of(r1, r2).stream().filter("OK"::equals).count();
        assertEquals(1, successCount, "并发刷新只能有一个成功者，实际: " + r1 + "/" + r2);

        List<Map<String, Object>> tokens = refreshTokens(login.getSessionId());
        long activeChildren = tokens.stream()
                .filter(t -> ((Number) t.get("token_status")).intValue() == TokenStatus.ACTIVE.getCode())
                .count();
        assertTrue(activeChildren <= 1, "不得存在多个有效子令牌");
        Map<String, Object> parent = tokens.get(0);
        assertEquals(TokenStatus.ROTATED.getCode(), ((Number) parent.get("token_status")).intValue());
    }

    // ---- TC-003-8 ----

    @Test
    @DisplayName("TC-003-8 会话非活跃 / 到期 / 历史 scope 为空 均拒绝刷新")
    void refresh_rejectedForInvalidSessionStates() {
        String mobile = uniqueMobile();
        String deviceId = uniqueDevice();
        LoginResultDto login = login(mobile, deviceId);

        // 1) 历史会话 scope 为 NULL：不得通过新刷新链路签发令牌
        jdbcTemplate.update("UPDATE ciam_session SET scope = NULL WHERE session_id = ?", login.getSessionId());
        BusinessException noScope = assertThrows(BusinessException.class,
                () -> authenticationAppService.refreshToken(login.getRefreshToken(), CLIENT_ID));
        assertEquals(CiamErrorCode.TOKEN_INVALID, noScope.getErrorCode());
        jdbcTemplate.update("UPDATE ciam_session SET scope = ? WHERE session_id = ?",
                SEEDED_SCOPE, login.getSessionId());

        // 2) 会话已到期（now >= expireTime）
        jdbcTemplate.update("UPDATE ciam_session SET expire_time = ? WHERE session_id = ?",
                Timestamp.from(Instant.now().minusSeconds(1)), login.getSessionId());
        BusinessException expired = assertThrows(BusinessException.class,
                () -> authenticationAppService.refreshToken(login.getRefreshToken(), CLIENT_ID));
        assertEquals(CiamErrorCode.SESSION_EXPIRED, expired.getErrorCode());

        // 3) 会话被踢下线
        jdbcTemplate.update("UPDATE ciam_session SET session_status = ?, expire_time = ? WHERE session_id = ?",
                SessionStatus.KICKED.getCode(),
                Timestamp.from(Instant.now().plusSeconds(SEEDED_REFRESH_TTL)), login.getSessionId());
        BusinessException kicked = assertThrows(BusinessException.class,
                () -> authenticationAppService.refreshToken(login.getRefreshToken(), CLIENT_ID));
        assertEquals(CiamErrorCode.TOKEN_INVALID, kicked.getErrorCode());
    }

    // ---- TC-003-9 ----

    @Test
    @DisplayName("TC-003-9 登出后会话 KICKED、刷新链撤销、重复刷新被拒绝")
    void logout_kicksSessionAndRevokesRefreshChain() {
        String mobile = uniqueMobile();
        String deviceId = uniqueDevice();
        LoginResultDto login = login(mobile, deviceId);

        authenticationAppService.logout(login.getSessionId(), login.getUserId(), CLIENT_ID);

        Map<String, Object> row = session(login.getSessionId());
        assertEquals(SessionStatus.KICKED.getCode(), ((Number) row.get("session_status")).intValue());
        assertNotNull(row.get("logout_time"));

        assertTrue(refreshTokens(login.getSessionId()).stream().noneMatch(t ->
                        ((Number) t.get("token_status")).intValue() == TokenStatus.ACTIVE.getCode()),
                "登出必须撤销会话活跃 Refresh Token");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authenticationAppService.refreshToken(login.getRefreshToken(), CLIENT_ID));
        assertEquals(CiamErrorCode.TOKEN_INVALID, ex.getErrorCode());

        // 旧 Access Token 即使 JWT 未到期，会话校验也必须拒绝
        BusinessException tokenEx = assertThrows(BusinessException.class,
                () -> tokenAppService.verifyToken(login.getAccessToken()));
        assertEquals(CiamErrorCode.TOKEN_INVALID, tokenEx.getErrorCode());
        assertFalse(sessionAppService.validateSession(login.getSessionId()).isValid());
    }

    // ---- TC-003-14 ----

    @Test
    @DisplayName("TC-003-14 V6 迁移已应用，scope 列存在，历史记录保持 NULL")
    void migrationV6_appliedWithNullableScope() {
        Integer applied = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '6' AND success = 1",
                Integer.class);
        assertEquals(1, applied, "V6 必须已成功应用");

        Integer column = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns "
                        + "WHERE table_schema = DATABASE() AND table_name = 'ciam_session' "
                        + "AND column_name = 'scope' AND is_nullable = 'YES'",
                Integer.class);
        assertEquals(1, column, "ciam_session.scope 必须存在且可空（历史记录不回填）");

        // 历史会话（V6 之前）scope 保持 NULL
        String legacySessionId = "legacy-" + UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO ciam_session "
                        + "(session_id, user_id, client_type, session_status, expire_time) "
                        + "VALUES (?, ?, 'app', 1, ?)",
                legacySessionId, "legacy-user", Timestamp.from(Instant.now().plusSeconds(3600)));
        assertNull(session(legacySessionId).get("scope"));
    }

    // ---- 令牌指纹工具校验（防止测试与实现指纹算法漂移）----

    @Test
    @DisplayName("Refresh Token 落库仅保存指纹，不保存原始值")
    void refreshTokenStoredAsFingerprintOnly() {
        String mobile = uniqueMobile();
        String deviceId = uniqueDevice();
        LoginResultDto login = login(mobile, deviceId);

        List<Map<String, Object>> tokens = refreshTokens(login.getSessionId());
        assertEquals(1, tokens.size());
        assertEquals(TokenDigest.fingerprint(login.getRefreshToken()), tokens.get(0).get("token_fingerprint"));
        assertNotEquals(login.getRefreshToken(), tokens.get(0).get("token_fingerprint"));
    }
}
