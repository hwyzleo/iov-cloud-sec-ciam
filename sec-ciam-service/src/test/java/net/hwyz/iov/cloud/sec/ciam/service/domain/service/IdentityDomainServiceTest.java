package net.hwyz.iov.cloud.sec.ciam.service.domain.service;

import net.hwyz.iov.cloud.sec.ciam.service.common.security.FieldEncryptor;
import net.hwyz.iov.cloud.sec.ciam.service.domain.enums.IdentityType;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.UserIdentityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;

class IdentityDomainServiceTest {

    private IdentityDomainService service;

    @BeforeEach
    void setUp() {
        service = new IdentityDomainService(mock(UserIdentityRepository.class), mock(FieldEncryptor.class));
    }

    @Test
    void test_bind_compilation() {
        // 修正为 5 参数调用
        service.bindIdentity("u1", IdentityType.MOBILE, "138", "86", "APP");
    }
}
