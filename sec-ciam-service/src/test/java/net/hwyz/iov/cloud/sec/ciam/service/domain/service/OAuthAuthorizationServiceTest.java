package net.hwyz.iov.cloud.sec.ciam.service.domain.service;

import net.hwyz.iov.cloud.sec.ciam.service.domain.model.AuthCode;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.CiamAuthCodeRepository;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.CiamOAuthClientRepository;
import net.hwyz.iov.cloud.sec.ciam.service.common.security.PasswordEncoder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OAuthAuthorizationServiceTest {

    private CiamAuthCodeRepository authCodeRepository;
    private OAuthAuthorizationService service;

    @BeforeEach
    void setUp() {
        authCodeRepository = mock(CiamAuthCodeRepository.class);
        service = new OAuthAuthorizationService(
                authCodeRepository, mock(CiamOAuthClientRepository.class), mock(PasswordEncoder.class));
    }

    @Test
    void test_compilation() {
        AuthCode code = AuthCode.builder().authCodeId("c1").build();
        when(authCodeRepository.findByCodeHash(anyString())).thenReturn(Optional.of(code));
        assertNotNull(code);
    }
}
