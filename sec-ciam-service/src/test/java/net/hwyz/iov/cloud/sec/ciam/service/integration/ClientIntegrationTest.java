package net.hwyz.iov.cloud.sec.ciam.service.integration;

import net.hwyz.iov.cloud.framework.common.bean.ApiResponse;
import net.hwyz.iov.cloud.sec.ciam.service.adapter.web.controller.mobile.vo.MobileLoginRequest;
import net.hwyz.iov.cloud.sec.ciam.service.domain.enums.IdentityStatus;
import net.hwyz.iov.cloud.sec.ciam.service.domain.enums.UserStatus;
import net.hwyz.iov.cloud.sec.ciam.service.domain.model.OAuthClient;
import net.hwyz.iov.cloud.sec.ciam.service.domain.model.User;
import net.hwyz.iov.cloud.sec.ciam.service.domain.model.UserIdentity;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.OAuthClientRepository;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.UserIdentityRepository;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ClientIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @MockBean
    private UserRepository userRepository;
    @MockBean
    private UserIdentityRepository identityRepository;
    @MockBean
    private OAuthClientRepository clientRepository;

    private static final String CLIENT_ID = "test-client";

    @BeforeEach
    void setUp() {
        OAuthClient client = OAuthClient.builder()
                .clientId(CLIENT_ID)
                .clientStatus(1)
                .build();
        when(clientRepository.findByClientId(CLIENT_ID)).thenReturn(Optional.of(client));
    }

    @Test
    void testLoginFlow_compilation() {
        String userId = "u1";
        UserIdentity identity = UserIdentity.builder().userId(userId).identityType("MOBILE").identityStatus(IdentityStatus.BOUND.getCode()).build();
        when(identityRepository.findByTypeAndHash(any(), any())).thenReturn(Optional.of(identity));
        when(userRepository.findByUserId(userId)).thenReturn(Optional.of(User.builder().userId(userId).userStatus(UserStatus.ACTIVE.getCode()).build()));

        MobileLoginRequest req = MobileLoginRequest.builder().mobile("138").code("123456").build();
        ResponseEntity<ApiResponse> response = restTemplate.postForEntity("/mobile/auth/login/mobile", req, ApiResponse.class);
        assertNotNull(response);
    }
}
