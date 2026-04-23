package net.hwyz.iov.cloud.sec.ciam.service.application;

import net.hwyz.iov.cloud.sec.ciam.service.application.service.AccountLifecycleAppService;
import net.hwyz.iov.cloud.sec.ciam.service.application.service.PasswordChangeAppService;
import net.hwyz.iov.cloud.sec.ciam.service.common.audit.AuditLogger;
import net.hwyz.iov.cloud.sec.ciam.service.common.audit.SecurityEventLogger;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.*;
import net.hwyz.iov.cloud.sec.ciam.service.domain.service.IdentityDomainService;
import net.hwyz.iov.cloud.sec.ciam.service.domain.service.UserDomainService;
import net.hwyz.iov.cloud.sec.ciam.service.domain.service.VerificationCodeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;

class AccountLifecycleAppServiceTest {

    private AccountLifecycleAppService service;

    @BeforeEach
    void setUp() {
        service = new AccountLifecycleAppService(
                mock(VerificationCodeService.class), mock(IdentityDomainService.class),
                mock(UserDomainService.class), mock(PasswordChangeAppService.class),
                mock(DeactivationRequestRepository.class), mock(UserRepository.class),
                mock(UserIdentityRepository.class), mock(UserCredentialRepository.class),
                mock(UserProfileRepository.class), mock(SessionRepository.class),
                mock(RefreshTokenRepository.class), mock(AuditLogger.class),
                mock(SecurityEventLogger.class));
    }

    @Test
    void test_compilation() {
        // 此处仅用于验证编译
    }
}
