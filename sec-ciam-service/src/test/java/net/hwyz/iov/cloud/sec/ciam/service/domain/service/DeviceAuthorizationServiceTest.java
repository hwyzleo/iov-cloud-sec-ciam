package net.hwyz.iov.cloud.sec.ciam.service.domain.service;

import net.hwyz.iov.cloud.framework.common.exception.BusinessException;
import net.hwyz.iov.cloud.sec.ciam.service.common.exception.CiamErrorCode;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.CiamOAuthClientRepository;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.dataobject.OAuthClientPo;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.store.InMemoryVerificationCodeStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DeviceAuthorizationServiceTest {

    private CiamOAuthClientRepository clientRepository;
    private InMemoryVerificationCodeStore store;
    private DeviceAuthorizationService service;

    private static final String CLIENT_ID = "vehicle-client-001";
    private static final String USER_ID = "user-001";
    private static final String SCOPE = "openid,vehicle";

    @BeforeEach
    void setUp() {
        clientRepository = mock(CiamOAuthClientRepository.class);
        store = new InMemoryVerificationCodeStore();
        service = new DeviceAuthorizationService(clientRepository, store);
    }

    private OAuthClientPo stubEnabledClient() {
        OAuthClientPo client = new OAuthClientPo();
        client.setClientId(CLIENT_ID);
        client.setClientName("Vehicle App");
        client.setClientType("public");
        client.setRedirectUris(null);
        client.setGrantTypes("urn:ietf:params:oauth:grant-type:device_code");
        client.setScopes("openid,vehicle");
        client.setPkceRequired(0);
        client.setAccessTokenTtl(1800);
        client.setRefreshTokenTtl(2592000);
        client.setClientStatus(1);
        client.setRowVersion(1);
        client.setRowValid(1);
        return client;
    }

    // ---- initiateDeviceAuthorization ----

    @Nested
    class InitiateTests {

        @Test
        void initiate_successfully() {
            when(clientRepository.findByClientId(CLIENT_ID))
                    .thenReturn(Optional.of(stubEnabledClient()));

            DeviceAuthorizationResponse response = service.initiateDeviceAuthorization(CLIENT_ID, SCOPE);

            assertNotNull(response.getDeviceCode());
            assertFalse(response.getDeviceCode().isBlank());
            assertNotNull(response.getUserCode());
            assertEquals(DeviceAuthorizationService.USER_CODE_LENGTH, response.getUserCode().length());
            assertTrue(response.getUserCode().matches("[A-Z]+"));
            assertEquals(DeviceAuthorizationService.VERIFICATION_URI, response.getVerificationUri());
            assertEquals(DeviceAuthorizationService.DEVICE_CODE_TTL_SECONDS, response.getExpiresIn());
            assertEquals(DeviceAuthorizationService.POLLING_INTERVAL_SECONDS, response.getInterval());
        }

        @Test
        void initiate_failsWhenClientNotFound() {
            when(clientRepository.findByClientId("nonexistent")).thenReturn(Optional.empty());

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.initiateDeviceAuthorization("nonexistent", SCOPE));
            assertEquals(CiamErrorCode.CLIENT_NOT_FOUND, ex.getErrorCode());
        }

        @Test
        void initiate_failsWhenClientDisabled() {
            OAuthClientPo client = stubEnabledClient();
            client.setClientStatus(0);
            when(clientRepository.findByClientId(CLIENT_ID)).thenReturn(Optional.of(client));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.initiateDeviceAuthorization(CLIENT_ID, SCOPE));
            assertEquals(CiamErrorCode.CLIENT_DISABLED, ex.getErrorCode());
        }
    }

    // ---- approveDeviceAuthorization ----

    @Nested
    class ApproveTests {

        @Test
        void approve_successfully() {
            when(clientRepository.findByClientId(CLIENT_ID))
                    .thenReturn(Optional.of(stubEnabledClient()));

            DeviceAuthorizationResponse response = service.initiateDeviceAuthorization(CLIENT_ID, SCOPE);

            // Approve should not throw
            assertDoesNotThrow(() -> service.approveDeviceAuthorization(response.getUserCode(), USER_ID));

            // After approval, polling should return result
            DeviceAuthorizationResult result = service.pollDeviceAuthorization(
                    response.getDeviceCode(), CLIENT_ID);
            assertEquals(USER_ID, result.getUserId());
            assertEquals(SCOPE, result.getScope());
            assertEquals(CLIENT_ID, result.getClientId());
        }

        @Test
        void approve_failsWhenUserCodeNotFound() {
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.approveDeviceAuthorization("INVALID1", USER_ID));
            assertEquals(CiamErrorCode.DEVICE_USER_CODE_NOT_FOUND, ex.getErrorCode());
        }
    }

    // ---- pollDeviceAuthorization ----

    @Nested
    class PollTests {

        @Test
        void poll_pendingAuthorization() {
            when(clientRepository.findByClientId(CLIENT_ID))
                    .thenReturn(Optional.of(stubEnabledClient()));

            DeviceAuthorizationResponse response = service.initiateDeviceAuthorization(CLIENT_ID, SCOPE);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.pollDeviceAuthorization(response.getDeviceCode(), CLIENT_ID));
            assertEquals(CiamErrorCode.DEVICE_CODE_PENDING, ex.getErrorCode());
        }

        @Test
        void poll_approvedAuthorization() {
            when(clientRepository.findByClientId(CLIENT_ID))
                    .thenReturn(Optional.of(stubEnabledClient()));

            DeviceAuthorizationResponse response = service.initiateDeviceAuthorization(CLIENT_ID, SCOPE);
            service.approveDeviceAuthorization(response.getUserCode(), USER_ID);

            DeviceAuthorizationResult result = service.pollDeviceAuthorization(
                    response.getDeviceCode(), CLIENT_ID);

            assertEquals(USER_ID, result.getUserId());
            assertEquals(SCOPE, result.getScope());
            assertEquals(CLIENT_ID, result.getClientId());
        }

        @Test
        void poll_expiredAuthorization() {
            // Device code not in store at all → expired
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.pollDeviceAuthorization("nonexistent-device-code", CLIENT_ID));
            assertEquals(CiamErrorCode.DEVICE_CODE_EXPIRED, ex.getErrorCode());
        }

        @Test
        void poll_failsWhenClientIdMismatch() {
            when(clientRepository.findByClientId(CLIENT_ID))
                    .thenReturn(Optional.of(stubEnabledClient()));

            DeviceAuthorizationResponse response = service.initiateDeviceAuthorization(CLIENT_ID, SCOPE);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.pollDeviceAuthorization(response.getDeviceCode(), "wrong-client"));
            assertEquals(CiamErrorCode.AUTH_CODE_CLIENT_MISMATCH, ex.getErrorCode());
        }

        @Test
        void poll_approvedAuthorization_cleansUpState() {
            when(clientRepository.findByClientId(CLIENT_ID))
                    .thenReturn(Optional.of(stubEnabledClient()));

            DeviceAuthorizationResponse response = service.initiateDeviceAuthorization(CLIENT_ID, SCOPE);
            service.approveDeviceAuthorization(response.getUserCode(), USER_ID);

            // First poll succeeds
            DeviceAuthorizationResult result = service.pollDeviceAuthorization(
                    response.getDeviceCode(), CLIENT_ID);
            assertNotNull(result);

            // Second poll should fail (state cleaned up)
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.pollDeviceAuthorization(response.getDeviceCode(), CLIENT_ID));
            assertEquals(CiamErrorCode.DEVICE_CODE_EXPIRED, ex.getErrorCode());
        }
    }
}
