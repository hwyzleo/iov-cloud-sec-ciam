package net.hwyz.iov.cloud.sec.ciam.service.domain.service;

import net.hwyz.iov.cloud.sec.ciam.service.domain.enums.IdentityType;
import net.hwyz.iov.cloud.sec.ciam.service.domain.enums.RegisterSource;
import net.hwyz.iov.cloud.sec.ciam.service.domain.model.User;
import net.hwyz.iov.cloud.sec.ciam.service.domain.model.UserProfile;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.CiamUserProfileRepository;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.CiamUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserDomainServiceTest {

    private CiamUserRepository userRepository;
    private CiamUserProfileRepository profileRepository;
    private UserDomainService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(CiamUserRepository.class);
        profileRepository = mock(CiamUserProfileRepository.class);
        service = new UserDomainService(userRepository, profileRepository);
    }

    @Test
    void testCreateUser() {
        // 修正：对齐主代码 RegisterSource.EMAIL 及参数顺序
        User user = service.createUser(RegisterSource.EMAIL, "CHANNEL", "B1", IdentityType.MOBILE);
        assertNotNull(user);
        verify(userRepository).insert(any(User.class));
    }
}
