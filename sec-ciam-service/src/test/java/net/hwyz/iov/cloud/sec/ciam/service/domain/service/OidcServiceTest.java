package net.hwyz.iov.cloud.sec.ciam.service.domain.service;

import net.hwyz.iov.cloud.sec.ciam.service.common.security.FieldEncryptor;
import net.hwyz.iov.cloud.sec.ciam.service.domain.model.UserProfile;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.UserProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OidcServiceTest {

    private UserProfileRepository profileRepository;
    private IdentityDomainService identityDomainService;
    private FieldEncryptor fieldEncryptor;
    private OidcService service;

    private static final String USER_ID = "u-001";

    @BeforeEach
    void setUp() {
        profileRepository = mock(UserProfileRepository.class);
        identityDomainService = mock(IdentityDomainService.class);
        fieldEncryptor = mock(FieldEncryptor.class);
        // 严格对齐构造器：profileRepository, identityDomainService, fieldEncryptor
        service = new OidcService(profileRepository, identityDomainService, fieldEncryptor);
    }

    @Test
    void testGetUserInfo_compilation() {
        UserProfile profile = UserProfile.builder().userId(USER_ID).build();
        when(profileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(profile));
        
        // 修正为测试 getUserInfo， getUserClaims 已在主代码中被重构或删除
        assertNotNull(service.getUserInfo(USER_ID));
    }
}
