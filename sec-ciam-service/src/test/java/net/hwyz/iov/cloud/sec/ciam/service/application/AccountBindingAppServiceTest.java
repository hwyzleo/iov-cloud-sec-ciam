package net.hwyz.iov.cloud.sec.ciam.service.application;

import net.hwyz.iov.cloud.sec.ciam.service.application.service.AccountBindingAppService;
import net.hwyz.iov.cloud.sec.ciam.service.common.audit.AuditLogger;
import net.hwyz.iov.cloud.sec.ciam.service.common.security.FieldEncryptor;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.CiamMergeRequestRepository;
import net.hwyz.iov.cloud.sec.ciam.service.domain.service.IdentityDomainService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;

class AccountBindingAppServiceTest {

    private AccountBindingAppService service;

    @BeforeEach
    void setUp() {
        service = new AccountBindingAppService(
                mock(IdentityDomainService.class), mock(CiamMergeRequestRepository.class),
                mock(FieldEncryptor.class), mock(AuditLogger.class));
    }

    @Test
    void test_compilation() {
        // 仅用于验证构造器匹配
    }
}
