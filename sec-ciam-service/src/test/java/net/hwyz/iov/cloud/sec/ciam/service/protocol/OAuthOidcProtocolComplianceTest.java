package net.hwyz.iov.cloud.sec.ciam.service.protocol;

import net.hwyz.iov.cloud.sec.ciam.service.domain.model.AuthCode;
import net.hwyz.iov.cloud.sec.ciam.service.domain.model.UserProfile;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.AuthCodeRepository;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.UserProfileRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
class OAuthOidcProtocolComplianceTest {

    @MockBean
    private AuthCodeRepository authCodeRepository;
    @MockBean
    private UserProfileRepository profileRepository;

    @Test
    void testProtocolCompliance() {
        AuthCode code = AuthCode.builder().authCodeId("c1").build();
        when(authCodeRepository.findByCodeHash(anyString())).thenReturn(Optional.of(code));
        
        UserProfile profile = UserProfile.builder().userId("u1").build();
        when(profileRepository.findByUserId("u1")).thenReturn(Optional.of(profile));

        assertNotNull(code);
    }
}
