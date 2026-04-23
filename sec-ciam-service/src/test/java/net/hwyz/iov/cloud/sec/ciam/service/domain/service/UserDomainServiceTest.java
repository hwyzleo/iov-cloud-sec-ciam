package net.hwyz.iov.cloud.sec.ciam.service.domain.service;

import net.hwyz.iov.cloud.sec.ciam.service.domain.enums.IdentityType;
import net.hwyz.iov.cloud.sec.ciam.service.domain.enums.RegisterSource;
import net.hwyz.iov.cloud.sec.ciam.service.domain.model.User;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.UserProfileRepository;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserDomainServiceTest {

    private UserRepository userRepository;
    private UserProfileRepository profileRepository;
    private UserDomainService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        profileRepository = mock(UserProfileRepository.class);
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
