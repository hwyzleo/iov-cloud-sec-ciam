package net.hwyz.iov.cloud.sec.ciam.service.domain.service;
import net.hwyz.iov.cloud.sec.ciam.service.application.service.*;
import net.hwyz.iov.cloud.sec.ciam.service.domain.adapter.*;

import net.hwyz.iov.cloud.framework.common.exception.BusinessException;
import net.hwyz.iov.cloud.sec.ciam.service.common.exception.CiamErrorCode;
import net.hwyz.iov.cloud.sec.ciam.service.common.security.PasswordEncoder;
import net.hwyz.iov.cloud.sec.ciam.service.domain.enums.ClientStatus;
import net.hwyz.iov.cloud.sec.ciam.service.domain.enums.OAuthClientType;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.CiamOAuthClientRepository;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.po.OAuthClientPo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OAuthClientDomainServiceTest {

    private CiamOAuthClientRepository clientRepository;
    private PasswordEncoder passwordEncoder;
    private OAuthClientDomainService service;

    @BeforeEach
    void setUp() {
        clientRepository = mock(CiamOAuthClientRepository.class);
        passwordEncoder = new PasswordEncoder(4); // low strength for fast tests
        when(clientRepository.insert(any())).thenReturn(1);
        when(clientRepository.updateByClientId(any())).thenReturn(1);
        service = new OAuthClientDomainService(clientRepository, passwordEncoder);
    }

    private OAuthClientPo stubClient(String clientId, OAuthClientType type,
                                         String rawSecret, int status) {
        OAuthClientPo client = new OAuthClientPo();
        client.setClientId(clientId);
        client.setClientName("Test App");
        client.setClientType(type.getCode());
        client.setClientSecretHash(rawSecret != null ? passwordEncoder.encode(rawSecret) : null);
        client.setRedirectUris("https://example.com/callback,https://example.com/cb2");
        client.setGrantTypes("authorization_code,refresh_token");
        client.setScopes("openid,profile");
        client.setPkceRequired(1);
        client.setAccessTokenTtl(1800);
        client.setRefreshTokenTtl(2592000);
        client.setClientStatus(status);
        client.setRowVersion(1);
        client.setRowValid(1);
        return client;
    }

    // ---- registerClient ----

    @Nested
    class RegisterClientTests {

        @Test
        void registerPublicClient_noSecret() {
            ClientRegistrationResult result = service.registerClient(
                    "Mobile App", OAuthClientType.PUBLIC,
                    "https://app.example.com/callback", "authorization_code",
                    "openid,profile", true, 1800, 2592000);

            assertNotNull(result.getClientId());
            assertNull(result.getClientSecret());
            assertEquals("Mobile App", result.getClientName());
            verify(clientRepository).insert(argThat(entity -> {
                assertEquals(OAuthClientType.PUBLIC.getCode(), entity.getClientType());
                assertNull(entity.getClientSecretHash());
                assertEquals(ClientStatus.ENABLED.getCode(), entity.getClientStatus());
                return true;
            }));
        }

        @Test
        void registerConfidentialClient_withSecret() {
            ClientRegistrationResult result = service.registerClient(
                    "Backend Service", OAuthClientType.CONFIDENTIAL,
                    "https://api.example.com/callback", "authorization_code,client_credentials",
                    "openid,profile,email", false, 3600, 86400);

            assertNotNull(result.getClientId());
            assertNotNull(result.getClientSecret());
            assertTrue(result.getClientSecret().length() > 0);
            assertEquals("Backend Service", result.getClientName());
            verify(clientRepository).insert(argThat(entity -> {
                assertEquals(OAuthClientType.CONFIDENTIAL.getCode(), entity.getClientType());
                assertNotNull(entity.getClientSecretHash());
                // hash should be verifiable with the raw secret
                assertTrue(passwordEncoder.matches(result.getClientSecret(), entity.getClientSecretHash()));
                return true;
            }));
        }

        @Test
        void registerInternalClient_withSecret() {
            ClientRegistrationResult result = service.registerClient(
                    "Internal System", OAuthClientType.INTERNAL,
                    null, "client_credentials",
                    "internal", false, 1800, 0);

            assertNotNull(result.getClientId());
            assertNotNull(result.getClientSecret());
            assertEquals("Internal System", result.getClientName());
            verify(clientRepository).insert(argThat(entity -> {
                assertEquals(OAuthClientType.INTERNAL.getCode(), entity.getClientType());
                assertNotNull(entity.getClientSecretHash());
                return true;
            }));
        }
    }

    // ---- findByClientId ----

    @Nested
    class FindByClientIdTests {

        @Test
        void findByClientId_returnsClient() {
            OAuthClientPo client = stubClient("cid-001", OAuthClientType.CONFIDENTIAL,
                    "secret123", ClientStatus.ENABLED.getCode());
            when(clientRepository.findByClientId("cid-001")).thenReturn(Optional.of(client));

            Optional<OAuthClientPo> result = service.findByClientId("cid-001");

            assertTrue(result.isPresent());
            assertEquals("cid-001", result.get().getClientId());
        }

        @Test
        void findByClientId_returnsEmptyWhenNotFound() {
            when(clientRepository.findByClientId("nonexistent")).thenReturn(Optional.empty());

            Optional<OAuthClientPo> result = service.findByClientId("nonexistent");

            assertFalse(result.isPresent());
        }
    }

    // ---- validateClient ----

    @Nested
    class ValidateClientTests {

        @Test
        void validateClient_successForConfidentialClient() {
            String rawSecret = "my-secret";
            OAuthClientPo client = stubClient("cid-001", OAuthClientType.CONFIDENTIAL,
                    rawSecret, ClientStatus.ENABLED.getCode());
            when(clientRepository.findByClientId("cid-001")).thenReturn(Optional.of(client));

            assertTrue(service.validateClient("cid-001", rawSecret));
        }

        @Test
        void validateClient_failsWithWrongSecret() {
            OAuthClientPo client = stubClient("cid-001", OAuthClientType.CONFIDENTIAL,
                    "correct-secret", ClientStatus.ENABLED.getCode());
            when(clientRepository.findByClientId("cid-001")).thenReturn(Optional.of(client));

            assertFalse(service.validateClient("cid-001", "wrong-secret"));
        }

        @Test
        void validateClient_throwsWhenClientNotFound() {
            when(clientRepository.findByClientId("nonexistent")).thenReturn(Optional.empty());

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.validateClient("nonexistent", "secret"));
            assertEquals(CiamErrorCode.CLIENT_NOT_FOUND, ex.getErrorCode());
        }

        @Test
        void validateClient_throwsWhenClientDisabled() {
            OAuthClientPo client = stubClient("cid-001", OAuthClientType.CONFIDENTIAL,
                    "secret", ClientStatus.DISABLED.getCode());
            when(clientRepository.findByClientId("cid-001")).thenReturn(Optional.of(client));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.validateClient("cid-001", "secret"));
            assertEquals(CiamErrorCode.CLIENT_DISABLED, ex.getErrorCode());
        }

        @Test
        void validateClient_returnsFalseForPublicClient() {
            OAuthClientPo client = stubClient("cid-pub", OAuthClientType.PUBLIC,
                    null, ClientStatus.ENABLED.getCode());
            when(clientRepository.findByClientId("cid-pub")).thenReturn(Optional.of(client));

            assertFalse(service.validateClient("cid-pub", "any-secret"));
        }
    }

    // ---- validateRedirectUri ----

    @Nested
    class ValidateRedirectUriTests {

        @Test
        void validateRedirectUri_matchesRegisteredUri() {
            OAuthClientPo client = stubClient("cid-001", OAuthClientType.CONFIDENTIAL,
                    "secret", ClientStatus.ENABLED.getCode());
            when(clientRepository.findByClientId("cid-001")).thenReturn(Optional.of(client));

            assertTrue(service.validateRedirectUri("cid-001", "https://example.com/callback"));
            assertTrue(service.validateRedirectUri("cid-001", "https://example.com/cb2"));
        }

        @Test
        void validateRedirectUri_rejectsUnregisteredUri() {
            OAuthClientPo client = stubClient("cid-001", OAuthClientType.CONFIDENTIAL,
                    "secret", ClientStatus.ENABLED.getCode());
            when(clientRepository.findByClientId("cid-001")).thenReturn(Optional.of(client));

            assertFalse(service.validateRedirectUri("cid-001", "https://evil.com/callback"));
        }

        @Test
        void validateRedirectUri_throwsWhenClientNotFound() {
            when(clientRepository.findByClientId("nonexistent")).thenReturn(Optional.empty());

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.validateRedirectUri("nonexistent", "https://example.com"));
            assertEquals(CiamErrorCode.CLIENT_NOT_FOUND, ex.getErrorCode());
        }

        @Test
        void validateRedirectUri_returnsFalseWhenNoUrisConfigured() {
            OAuthClientPo client = stubClient("cid-001", OAuthClientType.INTERNAL,
                    "secret", ClientStatus.ENABLED.getCode());
            client.setRedirectUris(null);
            when(clientRepository.findByClientId("cid-001")).thenReturn(Optional.of(client));

            assertFalse(service.validateRedirectUri("cid-001", "https://example.com"));
        }
    }

    // ---- disableClient / enableClient ----

    @Nested
    class ClientStatusToggleTests {

        @Test
        void disableClient_setsStatusToDisabled() {
            OAuthClientPo client = stubClient("cid-001", OAuthClientType.CONFIDENTIAL,
                    "secret", ClientStatus.ENABLED.getCode());
            when(clientRepository.findByClientId("cid-001")).thenReturn(Optional.of(client));

            service.disableClient("cid-001");

            verify(clientRepository).updateByClientId(argThat(entity -> {
                assertEquals(ClientStatus.DISABLED.getCode(), entity.getClientStatus());
                return true;
            }));
        }

        @Test
        void enableClient_setsStatusToEnabled() {
            OAuthClientPo client = stubClient("cid-001", OAuthClientType.CONFIDENTIAL,
                    "secret", ClientStatus.DISABLED.getCode());
            when(clientRepository.findByClientId("cid-001")).thenReturn(Optional.of(client));

            service.enableClient("cid-001");

            verify(clientRepository).updateByClientId(argThat(entity -> {
                assertEquals(ClientStatus.ENABLED.getCode(), entity.getClientStatus());
                return true;
            }));
        }

        @Test
        void disableClient_throwsWhenNotFound() {
            when(clientRepository.findByClientId("nonexistent")).thenReturn(Optional.empty());

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.disableClient("nonexistent"));
            assertEquals(CiamErrorCode.CLIENT_NOT_FOUND, ex.getErrorCode());
        }

        @Test
        void enableClient_throwsWhenNotFound() {
            when(clientRepository.findByClientId("nonexistent")).thenReturn(Optional.empty());

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.enableClient("nonexistent"));
            assertEquals(CiamErrorCode.CLIENT_NOT_FOUND, ex.getErrorCode());
        }
    }

    // ---- updateClient ----

    @Nested
    class UpdateClientTests {

        @Test
        void updateClient_updatesSpecifiedFields() {
            OAuthClientPo client = stubClient("cid-001", OAuthClientType.CONFIDENTIAL,
                    "secret", ClientStatus.ENABLED.getCode());
            when(clientRepository.findByClientId("cid-001")).thenReturn(Optional.of(client));

            service.updateClient("cid-001", "New Name", "https://new.example.com/cb",
                    null, "openid,email", null, 7200, null);

            verify(clientRepository).updateByClientId(argThat(entity -> {
                assertEquals("New Name", entity.getClientName());
                assertEquals("https://new.example.com/cb", entity.getRedirectUris());
                assertEquals("authorization_code,refresh_token", entity.getGrantTypes()); // unchanged
                assertEquals("openid,email", entity.getScopes());
                assertEquals(7200, entity.getAccessTokenTtl()); // updated
                assertEquals(2592000, entity.getRefreshTokenTtl()); // unchanged
                return true;
            }));
        }

        @Test
        void updateClient_throwsWhenNotFound() {
            when(clientRepository.findByClientId("nonexistent")).thenReturn(Optional.empty());

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.updateClient("nonexistent", "Name", null, null, null, null, null, null));
            assertEquals(CiamErrorCode.CLIENT_NOT_FOUND, ex.getErrorCode());
        }
    }
}
