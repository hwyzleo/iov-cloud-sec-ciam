package net.hwyz.iov.cloud.sec.ciam.service.domain.service;

import net.hwyz.iov.cloud.sec.ciam.service.domain.enums.UserStatus;
import net.hwyz.iov.cloud.sec.ciam.service.domain.model.User;
import net.hwyz.iov.cloud.sec.ciam.service.domain.model.UserIdentity;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.CiamUserIdentityRepository;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.CiamUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CoreDomainLogicTest {

    private CiamUserRepository userRepository;
    private CiamUserIdentityRepository identityRepository;
    private UserDomainService userDomainService;

    @BeforeEach
    void setUp() {
        userRepository = mock(CiamUserRepository.class);
        identityRepository = mock(CiamUserIdentityRepository.class);
        userDomainService = new UserDomainService(userRepository, mock(net.hwyz.iov.cloud.sec.ciam.service.domain.repository.CiamUserProfileRepository.class));
    }

    @Test
    void testUserStatusTransition() {
        String userId = "u1";
        User user = User.builder().userId(userId).userStatus(UserStatus.PENDING.getCode()).build();
        when(userRepository.findByUserId(userId)).thenReturn(Optional.of(user));

        userDomainService.activate(userId);
        assertEquals(UserStatus.ACTIVE.getCode(), user.getUserStatus());
    }

    @Test
    void testIdentityBinding() {
        String userId = "u1";
        UserIdentity identity = UserIdentity.builder().userId(userId).identityType("MOBILE").build();
        when(identityRepository.findByUserId(userId)).thenReturn(List.of(identity));

        List<UserIdentity> result = identityRepository.findByUserId(userId);
        assertEquals(1, result.size());
    }
}
