package net.hwyz.iov.cloud.sec.ciam.service.application;

import net.hwyz.iov.cloud.sec.ciam.service.application.service.AuthenticationAppService;
import net.hwyz.iov.cloud.sec.ciam.service.common.audit.AuditLogger;
import net.hwyz.iov.cloud.sec.ciam.service.common.security.PasswordEncoder;
import net.hwyz.iov.cloud.sec.ciam.service.domain.adapter.*;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.CiamUserRepository;
import net.hwyz.iov.cloud.sec.ciam.service.domain.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;

class AuthenticationAppServiceTest {

    private AuthenticationAppService service;

    @BeforeEach
    void setUp() {
        service = new AuthenticationAppService(
                mock(VerificationCodeService.class), mock(IdentityDomainService.class),
                mock(UserDomainService.class), mock(CiamUserRepository.class),
                mock(AuditLogger.class), mock(CredentialDomainService.class),
                mock(CaptchaDomainService.class), mock(SessionDomainService.class),
                mock(WechatLoginAdapter.class), mock(AppleLoginAdapter.class),
                mock(GoogleLoginAdapter.class), mock(LocalMobileAuthAdapter.class),
                mock(JwtTokenService.class), mock(RefreshTokenDomainService.class),
                mock(DeviceDomainService.class));
    }

    @Test
    void test_compilation() {
        // 构造器对齐验证
    }
}
