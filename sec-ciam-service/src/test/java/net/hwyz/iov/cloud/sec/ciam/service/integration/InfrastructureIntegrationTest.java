package net.hwyz.iov.cloud.sec.ciam.service.integration;

import net.hwyz.iov.cloud.sec.ciam.service.domain.model.User;
import net.hwyz.iov.cloud.sec.ciam.service.domain.model.UserIdentity;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.converter.UserIdentityPoConverter;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.converter.UserPoConverter;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.mapper.CiamUserIdentityMapper;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.mapper.CiamUserMapper;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.repository.UserIdentityRepositoryImpl;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.repository.UserRepositoryImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class InfrastructureIntegrationTest {

    @Autowired
    private CiamUserMapper userMapper;
    @Autowired
    private CiamUserIdentityMapper identityMapper;

    @Test
    void testUserRepository() {
        UserRepositoryImpl repo = new UserRepositoryImpl(userMapper, UserPoConverter.INSTANCE);
        String userId = UUID.randomUUID().toString();
        User user = User.builder().userId(userId).build();
        repo.insert(user);
        Optional<User> found = repo.findByUserId(userId);
        assertTrue(found.isPresent());
    }

    @Test
    void testIdentityRepository() {
        UserIdentityRepositoryImpl repo = new UserIdentityRepositoryImpl(identityMapper, UserIdentityPoConverter.INSTANCE);
        String userId = UUID.randomUUID().toString();
        UserIdentity identity = UserIdentity.builder().userId(userId).identityType("MOBILE").identityValue("138").build();
        repo.insert(identity);
        assertTrue(repo.findByUserId(userId).size() > 0);
    }
}
