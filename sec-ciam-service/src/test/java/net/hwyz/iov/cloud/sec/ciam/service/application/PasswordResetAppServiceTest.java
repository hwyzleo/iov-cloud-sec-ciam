package net.hwyz.iov.cloud.sec.ciam.service.application;

import net.hwyz.iov.cloud.sec.ciam.service.application.service.PasswordResetAppService;
import net.hwyz.iov.cloud.sec.ciam.service.common.audit.AuditLogger;
import net.hwyz.iov.cloud.sec.ciam.service.common.audit.SecurityEventLogger;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.RefreshTokenRepository;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.SessionRepository;
import net.hwyz.iov.cloud.sec.ciam.service.domain.service.CredentialDomainService;
import net.hwyz.iov.cloud.sec.ciam.service.domain.service.IdentityDomainService;
import net.hwyz.iov.cloud.sec.ciam.service.domain.service.VerificationCodeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;

class PasswordResetAppServiceTest {

    private PasswordResetAppService service;

    @BeforeEach
    void setUp() {
        service = new PasswordResetAppService(
                mock(IdentityDomainService.class), mock(VerificationCodeService.class),
                mock(CredentialDomainService.class), mock(SessionRepository.class),
                mock(RefreshTokenRepository.class), mock(AuditLogger.class),
                mock(SecurityEventLogger.class));
    }

    @Test
    void test_compilation() {
        // 构造器对齐验证
    }
}
