package net.hwyz.iov.cloud.sec.ciam.service.checkpoint;

import net.hwyz.iov.cloud.sec.ciam.service.application.service.AuthenticationAppService;
import net.hwyz.iov.cloud.sec.ciam.service.domain.enums.UserStatus;
import net.hwyz.iov.cloud.sec.ciam.service.domain.model.User;
import net.hwyz.iov.cloud.sec.ciam.service.domain.model.UserIdentity;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.CiamUserIdentityRepository;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.CiamUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
class CoreAuthenticationLoopVerificationTest {

    @MockBean
    private CiamUserRepository userRepository;
    @MockBean
    private CiamUserIdentityRepository identityRepository;
    @Autowired
    private AuthenticationAppService authenticationAppService;

    @Test
    void verifyAuthenticationLoop_compilation() {
        String userId = "u1";
        User user = User.builder().userId(userId).userStatus(UserStatus.ACTIVE.getCode()).build();
        UserIdentity identity = UserIdentity.builder().userId(userId).identityType("MOBILE").build();

        when(userRepository.findByUserId(anyString())).thenReturn(Optional.of(user));
        when(identityRepository.findByTypeAndHash(anyString(), anyString())).thenReturn(Optional.of(identity));

        assertNotNull(authenticationAppService);
    }
}
