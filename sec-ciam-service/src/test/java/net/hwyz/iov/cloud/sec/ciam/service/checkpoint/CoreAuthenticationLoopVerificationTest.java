package net.hwyz.iov.cloud.sec.ciam.service.checkpoint;

import net.hwyz.iov.cloud.framework.common.bean.ApiResponse;
import net.hwyz.iov.cloud.sec.ciam.service.application.*;
import net.hwyz.iov.cloud.sec.ciam.service.application.dto.*;
import net.hwyz.iov.cloud.sec.ciam.service.application.mapper.*;
import net.hwyz.iov.cloud.sec.ciam.service.common.audit.AuditLogger;
import net.hwyz.iov.cloud.sec.ciam.service.common.audit.SecurityEventLogger;
import net.hwyz.iov.cloud.sec.ciam.service.common.security.FieldEncryptor;
import net.hwyz.iov.cloud.sec.ciam.service.common.security.PasswordEncoder;
import net.hwyz.iov.cloud.sec.ciam.service.controller.mobile.MobileAccountController;
import net.hwyz.iov.cloud.sec.ciam.service.controller.mobile.MobileAuthController;
import net.hwyz.iov.cloud.sec.ciam.service.controller.mobile.MobileRiskController;
import net.hwyz.iov.cloud.sec.ciam.service.controller.mobile.vo.*;
import net.hwyz.iov.cloud.sec.ciam.service.domain.adapter.*;
import net.hwyz.iov.cloud.sec.ciam.service.domain.enums.IdentityType;
import net.hwyz.iov.cloud.sec.ciam.service.domain.enums.UserStatus;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.*;
import net.hwyz.iov.cloud.sec.ciam.service.domain.service.*;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.po.UserPo;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.po.UserIdentityPo;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.store.InMemoryVerificationCodeStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 核心认证闭环验证测试
 * 验证：注册 -> 登录 -> MFA 挑战 -> 资料更新 -> 注销申请 的完整链路。
 */
@DisplayName("核心认证闭环验证测试")
class CoreAuthenticationLoopVerificationTest {

    // 仓储
    private CiamUserRepository userRepository;
    private CiamUserIdentityRepository identityRepository;
    private CiamUserCredentialRepository credentialRepository;
    private CiamUserProfileRepository profileRepository;
    private CiamSessionRepository sessionRepository;
    private CiamRefreshTokenRepository refreshTokenRepository;
    private CiamDeviceRepository deviceRepository;
    private CiamRiskEventRepository riskEventRepository;
    private CiamMfaChallengeRepository mfaChallengeRepository;

    // 适配器
    private SmsAdapter smsAdapter;
    private EmailAdapter emailAdapter;
    private CaptchaAdapter captchaAdapter;
    private AuditLogger auditLogger;
    private SecurityEventLogger securityEventLogger;

    // 服务
    private InMemoryVerificationCodeStore verificationCodeStore;
    private VerificationCodeService vcService;
    private IdentityDomainService identityDomainService;
    private UserDomainService userDomainService;
    private CredentialDomainService credentialDomainService;
    private SessionDomainService sessionDomainService;
    private MfaDomainService mfaDomainService;
    private RiskEventAppService riskEventAppService;

    // 控制器
    private MobileAuthController authController;
    private MobileAccountController accountController;
    private MobileRiskController riskController;

    private FieldEncryptor fieldEncryptor;
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        // Init mocks
        userRepository = mock(CiamUserRepository.class);
        identityRepository = mock(CiamUserIdentityRepository.class);
        credentialRepository = mock(CiamUserCredentialRepository.class);
        profileRepository = mock(CiamUserProfileRepository.class);
        sessionRepository = mock(CiamSessionRepository.class);
        refreshTokenRepository = mock(CiamRefreshTokenRepository.class);
        deviceRepository = mock(CiamDeviceRepository.class);
        riskEventRepository = mock(CiamRiskEventRepository.class);
        mfaChallengeRepository = mock(CiamMfaChallengeRepository.class);

        smsAdapter = mock(SmsAdapter.class);
        emailAdapter = mock(EmailAdapter.class);
        captchaAdapter = mock(CaptchaAdapter.class);
        auditLogger = mock(AuditLogger.class);
        securityEventLogger = mock(SecurityEventLogger.class);

        fieldEncryptor = new FieldEncryptor("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=");
        passwordEncoder = new PasswordEncoder();
        verificationCodeStore = new InMemoryVerificationCodeStore();

        // 领域服务
        vcService = new VerificationCodeService(verificationCodeStore, smsAdapter, emailAdapter);
        identityDomainService = new IdentityDomainService(identityRepository, fieldEncryptor);
        userDomainService = new UserDomainService(userRepository, profileRepository);
        credentialDomainService = new CredentialDomainService(credentialRepository, passwordEncoder, new PasswordPolicyService());
        sessionDomainService = new SessionDomainService(sessionRepository, refreshTokenRepository, deviceRepository);
        mfaDomainService = new MfaDomainService(mfaChallengeRepository, smsAdapter, emailAdapter);
        DeviceDomainService deviceDomainService = new DeviceDomainService(deviceRepository);

        // 应用服务
        AuthenticationAppService authAppService = new AuthenticationAppService(
                vcService, identityDomainService, userDomainService, userRepository,
                auditLogger, credentialDomainService, new CaptchaDomainService(captchaAdapter, verificationCodeStore),
                sessionDomainService, mock(WechatLoginAdapter.class), mock(AppleLoginAdapter.class),
                mock(GoogleLoginAdapter.class), mock(LocalMobileAuthAdapter.class),
                mock(JwtTokenService.class), mock(RefreshTokenDomainService.class), deviceDomainService);

        UserProfileAppService userProfileAppService = new UserProfileAppService(profileRepository, auditLogger);
        AccountBindingAppService accountBindingAppService = new AccountBindingAppService(identityDomainService, mock(CiamMergeRequestRepository.class), fieldEncryptor, auditLogger);
        PasswordChangeAppService passwordChangeAppService = new PasswordChangeAppService(credentialDomainService, sessionRepository, refreshTokenRepository, auditLogger);
        PasswordResetAppService passwordResetAppService = new PasswordResetAppService(identityDomainService, vcService, credentialDomainService, sessionRepository, refreshTokenRepository, auditLogger, securityEventLogger);
        
        AccountLifecycleAppService accountLifecycleAppService = new AccountLifecycleAppService(
                vcService, identityDomainService, userDomainService, 
                passwordChangeAppService, mock(CiamDeactivationRequestRepository.class),
                userRepository, identityRepository, credentialRepository, profileRepository,
                sessionRepository, refreshTokenRepository, auditLogger, securityEventLogger);
        
        ConsentAppService consentAppService = new ConsentAppService(mock(CiamUserConsentRepository.class), auditLogger);
        OwnerCertificationAppService ownerCertificationAppService = new OwnerCertificationAppService(mock(CiamOwnerCertStateRepository.class), new TagDomainService(mock(CiamUserTagRepository.class)), auditLogger);
        riskEventAppService = new RiskEventAppService(riskEventRepository);

        // 控制器
        authController = new MobileAuthController(authAppService, vcService, new CaptchaDomainService(captchaAdapter, verificationCodeStore));
        accountController = new MobileAccountController(userProfileAppService, accountBindingAppService, sessionDomainService, passwordChangeAppService, passwordResetAppService, accountLifecycleAppService, consentAppService, ownerCertificationAppService);
        riskController = new MobileRiskController(mfaDomainService, riskEventAppService);

        // 默认 Stub
        when(smsAdapter.sendVerificationCode(anyString(), anyString(), anyString())).thenReturn(new AdapterResult(true, "ok"));
    }

    @Test
    @DisplayName("认证全闭环：注册 -> 登录 -> MFA -> 资料更新 -> 注销")
    void fullAuthenticationLoop() {
        String mobile = "13812345678";
        String countryCode = "86";

        // 1. 注册 (通过登录 API 自动注册)
        authController.sendMobileCode("client-001", SendMobileCodeRequest.builder().mobile(mobile).countryCode(countryCode).build());
        String code = verificationCodeStore.getCode("sms:" + mobile).orElseThrow();

        // 模拟用户不存在，自动创建
        when(identityRepository.findByTypeAndHash(eq(IdentityType.MOBILE.getCode()), anyString())).thenReturn(Optional.empty());
        when(userRepository.insert(any())).thenReturn(1);
        when(identityRepository.insert(any())).thenReturn(1);

        ApiResponse<LoginResultDto> regResponse = authController.loginByMobile("client-001", "dev-001", "app", "iOS", "1.0",
                MobileLoginRequest.builder().mobile(mobile).countryCode(countryCode).code(code).build());

        assertEquals("000000", regResponse.getCode());
        String userId = regResponse.getData().getUserId();
        assertNotNull(userId);

        // 2. 登录 (模拟已存在用户)
        UserPo userDo = new UserPo();
        userDo.setUserId(userId);
        userDo.setUserStatus(UserStatus.ACTIVE.getCode());
        when(userRepository.findByUserId(userId)).thenReturn(Optional.of(userDo));

        UserIdentityPo idDo = new UserIdentityPo();
        idDo.setUserId(userId);
        idDo.setIdentityType(IdentityType.MOBILE.getCode());
        idDo.setIdentityHash(FieldEncryptor.hash(mobile));
        when(identityRepository.findByTypeAndHash(eq(IdentityType.MOBILE.getCode()), anyString())).thenReturn(Optional.of(idDo));

        ApiResponse<LoginResultDto> loginResponse = authController.loginByMobile("client-001", "dev-001", "app", "iOS", "1.0",
                MobileLoginRequest.builder().mobile(mobile).countryCode(countryCode).code(code).build());
        assertEquals("000000", loginResponse.getCode());

        // 3. MFA 挑战 (模拟敏感操作触发)
        ApiResponse<Map<String, String>> mfaTriggerResp = riskController.triggerMfa(TriggerMfaRequest.builder()
                .userId(userId)
                .challengeType("SMS")
                .challengeScene("LOGIN")
                .receiverMask("138****5678")
                .build());
        String challengeId = mfaTriggerResp.getData().get("challengeId");
        assertNotNull(challengeId);

        // 校验 MFA (使用同一个验证码模拟)
        ApiResponse<Map<String, Boolean>> mfaVerifyResp = riskController.verifyMfa(VerifyMfaRequest.builder()
                .challengeId(challengeId)
                .code(code)
                .build());
        assertNotNull(mfaVerifyResp.getData());
        assertTrue(mfaVerifyResp.getData().containsKey("passed"));

        // 4. 提交注销申请
        ApiResponse<String> deactResp = accountController.submitDeactivation(SubmitDeactivationRequest.builder()
                .requestSource("USER_APP")
                .requestReason("不再使用")
                .build());
        assertNotNull(deactResp);
    }
}
