package net.hwyz.iov.cloud.sec.ciam.domain.service;

import net.hwyz.iov.cloud.sec.ciam.common.security.FieldEncryptor;
import net.hwyz.iov.cloud.sec.ciam.domain.enums.IdentityStatus;
import net.hwyz.iov.cloud.sec.ciam.domain.enums.IdentityType;
import net.hwyz.iov.cloud.sec.ciam.domain.repository.CiamUserIdentityRepository;
import net.hwyz.iov.cloud.sec.ciam.domain.repository.CiamUserProfileRepository;
import net.hwyz.iov.cloud.sec.ciam.infrastructure.repository.dao.dataobject.CiamUserIdentityDo;
import net.hwyz.iov.cloud.sec.ciam.infrastructure.repository.dao.dataobject.CiamUserProfileDo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OidcServiceTest {

    private static final String AES_KEY_BASE64 = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=";
    private static final String USER_ID = "user-oidc-001";
    private static final String CLIENT_ID = "client-app-001";

    private CiamUserProfileRepository profileRepository;
    private CiamUserIdentityRepository identityRepository;
    private FieldEncryptor fieldEncryptor;
    private IdentityDomainService identityDomainService;
    private OidcService service;

    @BeforeEach
    void setUp() {
        profileRepository = mock(CiamUserProfileRepository.class);
        identityRepository = mock(CiamUserIdentityRepository.class);
        fieldEncryptor = new FieldEncryptor(AES_KEY_BASE64);
        identityDomainService = new IdentityDomainService(identityRepository, fieldEncryptor);
        service = new OidcService(profileRepository, identityDomainService, fieldEncryptor);
    }

    private CiamUserProfileDo stubProfile() {
        CiamUserProfileDo profile = new CiamUserProfileDo();
        profile.setUserId(USER_ID);
        profile.setNickname("张三");
        profile.setAvatarUrl("https://cdn.openiov.com/avatar/001.jpg");
        profile.setGender(1);
        profile.setBirthday(LocalDate.of(1990, 6, 15));
        return profile;
    }

    private CiamUserIdentityDo stubEmailIdentity() {
        CiamUserIdentityDo identity = new CiamUserIdentityDo();
        identity.setUserId(USER_ID);
        identity.setIdentityType(IdentityType.EMAIL.getCode());
        identity.setIdentityValue(fieldEncryptor.encrypt("zhangsan@example.com"));
        identity.setIdentityStatus(IdentityStatus.BOUND.getCode());
        return identity;
    }

    private CiamUserIdentityDo stubMobileIdentity() {
        CiamUserIdentityDo identity = new CiamUserIdentityDo();
        identity.setUserId(USER_ID);
        identity.setIdentityType(IdentityType.MOBILE.getCode());
        identity.setIdentityValue(fieldEncryptor.encrypt("+8613800138000"));
        identity.setIdentityStatus(IdentityStatus.BOUND.getCode());
        return identity;
    }

    // ---- getUserInfo ----

    @Nested
    class GetUserInfoTests {

        @Test
        void getUserInfo_withFullProfile() {
            when(profileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(stubProfile()));
            when(identityRepository.findByUserId(USER_ID))
                    .thenReturn(List.of(stubEmailIdentity(), stubMobileIdentity()));

            OidcUserInfo info = service.getUserInfo(USER_ID);

            assertEquals(USER_ID, info.getSub());
            assertEquals("张三", info.getName());
            assertEquals("https://cdn.openiov.com/avatar/001.jpg", info.getPicture());
            assertEquals("male", info.getGender());
            assertEquals("1990-06-15", info.getBirthdate());
            assertEquals("zhangsan@example.com", info.getEmail());
            assertEquals("+8613800138000", info.getPhoneNumber());
        }

        @Test
        void getUserInfo_withNoProfile() {
            when(profileRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
            when(identityRepository.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            OidcUserInfo info = service.getUserInfo(USER_ID);

            assertEquals(USER_ID, info.getSub());
            assertNull(info.getName());
            assertNull(info.getPicture());
            assertNull(info.getGender());
            assertNull(info.getBirthdate());
            assertNull(info.getEmail());
            assertNull(info.getPhoneNumber());
        }

        @Test
        void getUserInfo_femaleGender() {
            CiamUserProfileDo profile = stubProfile();
            profile.setGender(2);
            when(profileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(profile));
            when(identityRepository.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            OidcUserInfo info = service.getUserInfo(USER_ID);

            assertEquals("female", info.getGender());
        }

        @Test
        void getUserInfo_unknownGender() {
            CiamUserProfileDo profile = stubProfile();
            profile.setGender(0);
            when(profileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(profile));
            when(identityRepository.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            OidcUserInfo info = service.getUserInfo(USER_ID);

            assertEquals("unknown", info.getGender());
        }
    }

    // ---- getDiscoveryDocument ----

    @Nested
    class GetDiscoveryDocumentTests {

        @Test
        void discoveryDocument_hasCorrectIssuer() {
            OidcDiscoveryDocument doc = service.getDiscoveryDocument();

            assertEquals("https://account.openiov.com", doc.getIssuer());
        }

        @Test
        void discoveryDocument_hasStandardEndpoints() {
            OidcDiscoveryDocument doc = service.getDiscoveryDocument();

            assertTrue(doc.getAuthorizationEndpoint().startsWith(doc.getIssuer()));
            assertTrue(doc.getTokenEndpoint().startsWith(doc.getIssuer()));
            assertTrue(doc.getUserinfoEndpoint().startsWith(doc.getIssuer()));
            assertTrue(doc.getJwksUri().startsWith(doc.getIssuer()));
            assertTrue(doc.getDeviceAuthorizationEndpoint().startsWith(doc.getIssuer()));
        }

        @Test
        void discoveryDocument_supportsExpectedGrantTypes() {
            OidcDiscoveryDocument doc = service.getDiscoveryDocument();

            assertTrue(doc.getGrantTypesSupported().contains("authorization_code"));
            assertTrue(doc.getGrantTypesSupported().contains("client_credentials"));
            assertTrue(doc.getGrantTypesSupported().contains("refresh_token"));
        }

        @Test
        void discoveryDocument_supportsOpenidScope() {
            OidcDiscoveryDocument doc = service.getDiscoveryDocument();

            assertTrue(doc.getScopesSupported().contains("openid"));
            assertTrue(doc.getScopesSupported().contains("profile"));
            assertTrue(doc.getScopesSupported().contains("email"));
            assertTrue(doc.getScopesSupported().contains("phone"));
        }
    }

    // ---- getIdTokenClaims ----

    @Nested
    class GetIdTokenClaimsTests {

        @Test
        void idTokenClaims_containsStandardClaims() {
            when(profileRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
            when(identityRepository.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            long beforeEpoch = Instant.now().getEpochSecond();
            Map<String, Object> claims = service.getIdTokenClaims(USER_ID, CLIENT_ID, "openid");
            long afterEpoch = Instant.now().getEpochSecond();

            assertEquals("https://account.openiov.com", claims.get("iss"));
            assertEquals(USER_ID, claims.get("sub"));
            assertEquals(CLIENT_ID, claims.get("aud"));

            long iat = (long) claims.get("iat");
            long exp = (long) claims.get("exp");
            assertTrue(iat >= beforeEpoch && iat <= afterEpoch);
            assertEquals(iat + OidcService.ID_TOKEN_TTL_SECONDS, exp);
        }

        @Test
        void idTokenClaims_withProfileScope_includesProfileClaims() {
            when(profileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(stubProfile()));
            when(identityRepository.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            Map<String, Object> claims = service.getIdTokenClaims(USER_ID, CLIENT_ID, "openid,profile");

            assertEquals("张三", claims.get("name"));
            assertEquals("https://cdn.openiov.com/avatar/001.jpg", claims.get("picture"));
            assertEquals("male", claims.get("gender"));
            assertEquals("1990-06-15", claims.get("birthdate"));
        }

        @Test
        void idTokenClaims_withEmailScope_includesEmail() {
            when(profileRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
            when(identityRepository.findByUserId(USER_ID))
                    .thenReturn(List.of(stubEmailIdentity()));

            Map<String, Object> claims = service.getIdTokenClaims(USER_ID, CLIENT_ID, "openid,email");

            assertEquals("zhangsan@example.com", claims.get("email"));
            assertFalse(claims.containsKey("phone_number"));
        }

        @Test
        void idTokenClaims_withPhoneScope_includesPhone() {
            when(profileRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
            when(identityRepository.findByUserId(USER_ID))
                    .thenReturn(List.of(stubMobileIdentity()));

            Map<String, Object> claims = service.getIdTokenClaims(USER_ID, CLIENT_ID, "openid,phone");

            assertEquals("+8613800138000", claims.get("phone_number"));
            assertFalse(claims.containsKey("email"));
        }

        @Test
        void idTokenClaims_openidOnly_noUserClaims() {
            when(profileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(stubProfile()));
            when(identityRepository.findByUserId(USER_ID))
                    .thenReturn(List.of(stubEmailIdentity(), stubMobileIdentity()));

            Map<String, Object> claims = service.getIdTokenClaims(USER_ID, CLIENT_ID, "openid");

            assertFalse(claims.containsKey("name"));
            assertFalse(claims.containsKey("email"));
            assertFalse(claims.containsKey("phone_number"));
        }

        @Test
        void idTokenClaims_nullScope_noUserClaims() {
            Map<String, Object> claims = service.getIdTokenClaims(USER_ID, CLIENT_ID, null);

            assertEquals(USER_ID, claims.get("sub"));
            assertFalse(claims.containsKey("name"));
            assertFalse(claims.containsKey("email"));
        }
    }

    // ---- mapGender ----

    @Nested
    class MapGenderTests {

        @Test
        void mapGender_male() {
            assertEquals("male", OidcService.mapGender(1));
        }

        @Test
        void mapGender_female() {
            assertEquals("female", OidcService.mapGender(2));
        }

        @Test
        void mapGender_unknown() {
            assertEquals("unknown", OidcService.mapGender(0));
        }

        @Test
        void mapGender_null() {
            assertEquals("unknown", OidcService.mapGender(null));
        }
    }
}
