package net.hwyz.iov.cloud.sec.ciam.service.domain.service;

import net.hwyz.iov.cloud.framework.common.exception.BusinessException;
import net.hwyz.iov.cloud.sec.ciam.service.common.exception.CiamErrorCode;
import net.hwyz.iov.cloud.sec.ciam.service.common.security.PasswordEncoder;
import net.hwyz.iov.cloud.sec.ciam.service.domain.model.OAuthClient;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.OAuthClientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 手机客户端准入策略测试（SEC-CIAM-DSN-CR-003 RD-003-4，§5.1/§6.5）。
 */
class MobileClientPolicyTest {

    private static final String CLIENT_ID = "ciam-mobile-app";

    private OAuthClientRepository clientRepository;
    private OAuthClientDomainService service;

    @BeforeEach
    void setUp() {
        clientRepository = mock(OAuthClientRepository.class);
        service = new OAuthClientDomainService(clientRepository, new PasswordEncoder(4));
    }

    private OAuthClient client(String type, int status) {
        return OAuthClient.builder()
                .clientId(CLIENT_ID)
                .clientType(type)
                .clientStatus(status)
                .scopes("openid profile")
                .accessTokenTtl(1800)
                .refreshTokenTtl(2592000)
                .build();
    }

    @Test
    void allowlistedEnabledPublicClient_isAccepted() {
        when(clientRepository.findByClientId(CLIENT_ID)).thenReturn(Optional.of(client("public", 1)));

        OAuthClient result = service.requireMobileClient(CLIENT_ID, List.of(CLIENT_ID));

        assertEquals(CLIENT_ID, result.getClientId());
    }

    @Test
    void clientOutsideAllowlist_isRejected() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.requireMobileClient(CLIENT_ID, List.of("other-client")));
        assertEquals(CiamErrorCode.CLIENT_TYPE_NOT_SUPPORTED, ex.getErrorCode());
    }

    @Test
    void emptyAllowlist_rejectsAllClients() {
        assertEquals(CiamErrorCode.CLIENT_TYPE_NOT_SUPPORTED,
                assertThrows(BusinessException.class,
                        () -> service.requireMobileClient(CLIENT_ID, List.of())).getErrorCode());
        assertEquals(CiamErrorCode.CLIENT_TYPE_NOT_SUPPORTED,
                assertThrows(BusinessException.class,
                        () -> service.requireMobileClient(CLIENT_ID, null)).getErrorCode());
    }

    @Test
    void unknownClient_isClientNotFound() {
        when(clientRepository.findByClientId(CLIENT_ID)).thenReturn(Optional.empty());

        assertEquals(CiamErrorCode.CLIENT_NOT_FOUND,
                assertThrows(BusinessException.class,
                        () -> service.requireMobileClient(CLIENT_ID, List.of(CLIENT_ID))).getErrorCode());
    }

    @Test
    void disabledClient_isClientDisabled() {
        when(clientRepository.findByClientId(CLIENT_ID)).thenReturn(Optional.of(client("public", 0)));

        assertEquals(CiamErrorCode.CLIENT_DISABLED,
                assertThrows(BusinessException.class,
                        () -> service.requireMobileClient(CLIENT_ID, List.of(CLIENT_ID))).getErrorCode());
    }

    @Test
    void confidentialClient_isRejectedForMobileLogin() {
        when(clientRepository.findByClientId(CLIENT_ID)).thenReturn(Optional.of(client("confidential", 1)));

        assertEquals(CiamErrorCode.CLIENT_TYPE_NOT_SUPPORTED,
                assertThrows(BusinessException.class,
                        () -> service.requireMobileClient(CLIENT_ID, List.of(CLIENT_ID))).getErrorCode());
    }
}
