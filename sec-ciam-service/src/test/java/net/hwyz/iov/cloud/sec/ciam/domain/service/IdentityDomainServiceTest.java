package net.hwyz.iov.cloud.sec.ciam.domain.service;

import net.hwyz.iov.cloud.framework.common.exception.BusinessException;
import net.hwyz.iov.cloud.sec.ciam.common.exception.CiamErrorCode;
import net.hwyz.iov.cloud.sec.ciam.common.security.FieldEncryptor;
import net.hwyz.iov.cloud.sec.ciam.domain.enums.IdentityStatus;
import net.hwyz.iov.cloud.sec.ciam.domain.enums.IdentityType;
import net.hwyz.iov.cloud.sec.ciam.domain.repository.CiamUserIdentityRepository;
import net.hwyz.iov.cloud.sec.ciam.infrastructure.repository.dao.dataobject.CiamUserIdentityDo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class IdentityDomainServiceTest {

    private CiamUserIdentityRepository identityRepository;
    private FieldEncryptor fieldEncryptor;
    private IdentityDomainService service;

    private static final String AES_KEY_BASE64 = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=";

    @BeforeEach
    void setUp() {
        identityRepository = mock(CiamUserIdentityRepository.class);
        fieldEncryptor = new FieldEncryptor(AES_KEY_BASE64);
        when(identityRepository.insert(any())).thenReturn(1);
        when(identityRepository.updateByIdentityId(any())).thenReturn(1);
        service = new IdentityDomainService(identityRepository, fieldEncryptor);
    }

    private CiamUserIdentityDo stubIdentity(String userId, IdentityType type, String value, int status) {
        CiamUserIdentityDo identity = new CiamUserIdentityDo();
        identity.setIdentityId("id-001");
        identity.setUserId(userId);
        identity.setIdentityType(type.getCode());
        identity.setIdentityValue(fieldEncryptor.encrypt(value));
        identity.setIdentityHash(FieldEncryptor.hash(value));
        identity.setIdentityStatus(status);
        identity.setRowValid(1);
        return identity;
    }

    // ---- bindIdentity ----

    @Nested
    class BindIdentityTests {

        @Test
        void bindIdentity_createsNewIdentity() {
            when(identityRepository.findByTypeAndHash(eq("mobile"), anyString()))
                    .thenReturn(Optional.empty());

            CiamUserIdentityDo result = service.bindIdentity(
                    "user-001", IdentityType.MOBILE, "13800138000", "+86", "app");

            assertNotNull(result.getIdentityId());
            assertEquals(32, result.getIdentityId().length());
            assertEquals("user-001", result.getUserId());
            assertEquals("mobile", result.getIdentityType());
            assertEquals(FieldEncryptor.hash("13800138000"), result.getIdentityHash());
            assertEquals("+86", result.getCountryCode());
            assertEquals("app", result.getBindSource());
            assertEquals(IdentityStatus.BOUND.getCode(), result.getIdentityStatus());
            assertEquals(0, result.getVerifiedFlag());
            assertEquals(0, result.getPrimaryFlag());
            assertEquals(1, result.getRowValid());
            assertEquals(1, result.getRowVersion());
            assertNotNull(result.getBindTime());
            assertNotNull(result.getCreateTime());
            assertNotNull(result.getModifyTime());
            // identity_value should be encrypted (not plaintext)
            assertNotEquals("13800138000", result.getIdentityValue());
            verify(identityRepository).insert(any(CiamUserIdentityDo.class));
        }

        @Test
        void bindIdentity_throwsConflictWhenBoundToOtherUser() {
            CiamUserIdentityDo existing = stubIdentity("other-user", IdentityType.EMAIL,
                    "test@example.com", IdentityStatus.BOUND.getCode());
            when(identityRepository.findByTypeAndHash(eq("email"), anyString()))
                    .thenReturn(Optional.of(existing));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.bindIdentity("user-001", IdentityType.EMAIL,
                            "test@example.com", null, "web"));
            assertEquals(CiamErrorCode.IDENTITY_CONFLICT, ex.getErrorCode());
            verify(identityRepository, never()).insert(any());
        }

        @Test
        void bindIdentity_returnsExistingWhenAlreadyBoundToSameUser() {
            CiamUserIdentityDo existing = stubIdentity("user-001", IdentityType.MOBILE,
                    "13800138000", IdentityStatus.BOUND.getCode());
            when(identityRepository.findByTypeAndHash(eq("mobile"), anyString()))
                    .thenReturn(Optional.of(existing));

            CiamUserIdentityDo result = service.bindIdentity(
                    "user-001", IdentityType.MOBILE, "13800138000", "+86", "app");

            assertEquals("id-001", result.getIdentityId());
            verify(identityRepository, never()).insert(any());
        }

        @Test
        void bindIdentity_allowsRebindWhenPreviouslyUnbound() {
            CiamUserIdentityDo existing = stubIdentity("other-user", IdentityType.WECHAT,
                    "wx-openid-123", IdentityStatus.UNBOUND.getCode());
            when(identityRepository.findByTypeAndHash(eq("wechat"), anyString()))
                    .thenReturn(Optional.of(existing));

            CiamUserIdentityDo result = service.bindIdentity(
                    "user-002", IdentityType.WECHAT, "wx-openid-123", null, "wechat");

            assertNotNull(result);
            verify(identityRepository).insert(any(CiamUserIdentityDo.class));
        }

        @Test
        void bindIdentity_supportsAllIdentityTypes() {
            when(identityRepository.findByTypeAndHash(anyString(), anyString()))
                    .thenReturn(Optional.empty());

            for (IdentityType type : IdentityType.values()) {
                CiamUserIdentityDo result = service.bindIdentity(
                        "user-001", type, "value-" + type.getCode(), null, "test");
                assertEquals(type.getCode(), result.getIdentityType());
            }
        }
    }

    // ---- unbindIdentity ----

    @Nested
    class UnbindIdentityTests {

        @Test
        void unbindIdentity_setsStatusToUnbound() {
            String hash = FieldEncryptor.hash("13800138000");
            CiamUserIdentityDo existing = stubIdentity("user-001", IdentityType.MOBILE,
                    "13800138000", IdentityStatus.BOUND.getCode());
            when(identityRepository.findByTypeAndHash("mobile", hash))
                    .thenReturn(Optional.of(existing));

            service.unbindIdentity("user-001", IdentityType.MOBILE, hash);

            ArgumentCaptor<CiamUserIdentityDo> captor = ArgumentCaptor.forClass(CiamUserIdentityDo.class);
            verify(identityRepository).updateByIdentityId(captor.capture());
            assertEquals(IdentityStatus.UNBOUND.getCode(), captor.getValue().getIdentityStatus());
            assertNotNull(captor.getValue().getUnbindTime());
            assertNotNull(captor.getValue().getModifyTime());
        }

        @Test
        void unbindIdentity_throwsWhenIdentityNotFound() {
            when(identityRepository.findByTypeAndHash(anyString(), anyString()))
                    .thenReturn(Optional.empty());

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.unbindIdentity("user-001", IdentityType.EMAIL, "nonexistent-hash"));
            assertEquals(CiamErrorCode.USER_NOT_FOUND, ex.getErrorCode());
            verify(identityRepository, never()).updateByIdentityId(any());
        }

        @Test
        void unbindIdentity_throwsWhenUserIdMismatch() {
            String hash = FieldEncryptor.hash("test@example.com");
            CiamUserIdentityDo existing = stubIdentity("other-user", IdentityType.EMAIL,
                    "test@example.com", IdentityStatus.BOUND.getCode());
            when(identityRepository.findByTypeAndHash("email", hash))
                    .thenReturn(Optional.of(existing));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.unbindIdentity("user-001", IdentityType.EMAIL, hash));
            assertEquals(CiamErrorCode.USER_NOT_FOUND, ex.getErrorCode());
            verify(identityRepository, never()).updateByIdentityId(any());
        }
    }

    // ---- findByTypeAndHash ----

    @Nested
    class FindByTypeAndHashTests {

        @Test
        void findByTypeAndHash_returnsIdentityWhenExists() {
            CiamUserIdentityDo existing = stubIdentity("user-001", IdentityType.APPLE,
                    "apple-sub-123", IdentityStatus.BOUND.getCode());
            when(identityRepository.findByTypeAndHash("apple", existing.getIdentityHash()))
                    .thenReturn(Optional.of(existing));

            Optional<CiamUserIdentityDo> result = service.findByTypeAndHash(
                    IdentityType.APPLE, existing.getIdentityHash());

            assertTrue(result.isPresent());
            assertEquals("user-001", result.get().getUserId());
        }

        @Test
        void findByTypeAndHash_returnsEmptyWhenNotFound() {
            when(identityRepository.findByTypeAndHash(anyString(), anyString()))
                    .thenReturn(Optional.empty());

            Optional<CiamUserIdentityDo> result = service.findByTypeAndHash(
                    IdentityType.GOOGLE, "nonexistent-hash");

            assertTrue(result.isEmpty());
        }
    }

    // ---- findByUserId ----

    @Nested
    class FindByUserIdTests {

        @Test
        void findByUserId_returnsOnlyBoundIdentities() {
            CiamUserIdentityDo bound = stubIdentity("user-001", IdentityType.MOBILE,
                    "13800138000", IdentityStatus.BOUND.getCode());
            CiamUserIdentityDo unbound = stubIdentity("user-001", IdentityType.EMAIL,
                    "test@example.com", IdentityStatus.UNBOUND.getCode());
            when(identityRepository.findByUserId("user-001")).thenReturn(List.of(bound, unbound));

            List<CiamUserIdentityDo> result = service.findByUserId("user-001");

            assertEquals(1, result.size());
            assertEquals("mobile", result.get(0).getIdentityType());
        }

        @Test
        void findByUserId_returnsEmptyWhenNoIdentities() {
            when(identityRepository.findByUserId("user-001")).thenReturn(Collections.emptyList());

            List<CiamUserIdentityDo> result = service.findByUserId("user-001");

            assertTrue(result.isEmpty());
        }
    }

    // ---- checkConflict ----

    @Nested
    class CheckConflictTests {

        @Test
        void checkConflict_returnsUserIdWhenConflictExists() {
            CiamUserIdentityDo existing = stubIdentity("user-001", IdentityType.MOBILE,
                    "13800138000", IdentityStatus.BOUND.getCode());
            when(identityRepository.findByTypeAndHash(eq("mobile"), anyString()))
                    .thenReturn(Optional.of(existing));

            Optional<String> result = service.checkConflict(IdentityType.MOBILE, "13800138000");

            assertTrue(result.isPresent());
            assertEquals("user-001", result.get());
        }

        @Test
        void checkConflict_returnsEmptyWhenNoConflict() {
            when(identityRepository.findByTypeAndHash(anyString(), anyString()))
                    .thenReturn(Optional.empty());

            Optional<String> result = service.checkConflict(IdentityType.EMAIL, "new@example.com");

            assertTrue(result.isEmpty());
        }

        @Test
        void checkConflict_returnsEmptyWhenExistingIsUnbound() {
            CiamUserIdentityDo existing = stubIdentity("user-001", IdentityType.EMAIL,
                    "test@example.com", IdentityStatus.UNBOUND.getCode());
            when(identityRepository.findByTypeAndHash(eq("email"), anyString()))
                    .thenReturn(Optional.of(existing));

            Optional<String> result = service.checkConflict(IdentityType.EMAIL, "test@example.com");

            assertTrue(result.isEmpty());
        }
    }

    // ---- findByTypeAndValue ----

    @Nested
    class FindByTypeAndValueTests {

        @Test
        void findByTypeAndValue_returnsIdentityWhenExists() {
            CiamUserIdentityDo existing = stubIdentity("user-001", IdentityType.MOBILE,
                    "13800138000", IdentityStatus.BOUND.getCode());
            when(identityRepository.findByTypeAndHash(eq("mobile"), eq(FieldEncryptor.hash("13800138000"))))
                    .thenReturn(Optional.of(existing));

            Optional<CiamUserIdentityDo> result = service.findByTypeAndValue(
                    IdentityType.MOBILE, "13800138000");

            assertTrue(result.isPresent());
            assertEquals("user-001", result.get().getUserId());
        }

        @Test
        void findByTypeAndValue_returnsEmptyWhenNotFound() {
            when(identityRepository.findByTypeAndHash(anyString(), anyString()))
                    .thenReturn(Optional.empty());

            Optional<CiamUserIdentityDo> result = service.findByTypeAndValue(
                    IdentityType.EMAIL, "nonexistent@example.com");

            assertTrue(result.isEmpty());
        }
    }

    // ---- markVerified ----

    @Nested
    class MarkVerifiedTests {

        @Test
        void markVerified_setsVerifiedFlagToOne() {
            String hash = FieldEncryptor.hash("13800138000");
            CiamUserIdentityDo existing = stubIdentity("user-001", IdentityType.MOBILE,
                    "13800138000", IdentityStatus.BOUND.getCode());
            when(identityRepository.findByTypeAndHash("mobile", hash))
                    .thenReturn(Optional.of(existing));

            service.markVerified("user-001", IdentityType.MOBILE, hash);

            ArgumentCaptor<CiamUserIdentityDo> captor = ArgumentCaptor.forClass(CiamUserIdentityDo.class);
            verify(identityRepository).updateByIdentityId(captor.capture());
            assertEquals(1, captor.getValue().getVerifiedFlag());
            assertNotNull(captor.getValue().getModifyTime());
        }

        @Test
        void markVerified_throwsWhenIdentityNotFound() {
            when(identityRepository.findByTypeAndHash(anyString(), anyString()))
                    .thenReturn(Optional.empty());

            assertThrows(BusinessException.class,
                    () -> service.markVerified("user-001", IdentityType.EMAIL, "nonexistent-hash"));
        }

        @Test
        void markVerified_throwsWhenUserIdMismatch() {
            String hash = FieldEncryptor.hash("test@example.com");
            CiamUserIdentityDo existing = stubIdentity("other-user", IdentityType.EMAIL,
                    "test@example.com", IdentityStatus.BOUND.getCode());
            when(identityRepository.findByTypeAndHash("email", hash))
                    .thenReturn(Optional.of(existing));

            assertThrows(BusinessException.class,
                    () -> service.markVerified("user-001", IdentityType.EMAIL, hash));
        }
    }

    // ---- countBoundIdentities ----

    @Nested
    class CountBoundIdentitiesTests {

        @Test
        void countBoundIdentities_countsOnlyBound() {
            CiamUserIdentityDo bound = stubIdentity("user-001", IdentityType.MOBILE,
                    "13800138000", IdentityStatus.BOUND.getCode());
            CiamUserIdentityDo unbound = stubIdentity("user-001", IdentityType.EMAIL,
                    "test@example.com", IdentityStatus.UNBOUND.getCode());
            when(identityRepository.findByUserId("user-001")).thenReturn(List.of(bound, unbound));

            long count = service.countBoundIdentities("user-001");

            assertEquals(1, count);
        }

        @Test
        void countBoundIdentities_returnsZeroWhenNone() {
            when(identityRepository.findByUserId("user-001")).thenReturn(Collections.emptyList());

            long count = service.countBoundIdentities("user-001");

            assertEquals(0, count);
        }
    }

    // ---- checkConflictDetail ----

    @Nested
    class CheckConflictDetailTests {

        @Test
        void checkConflictDetail_returnsConflictingIdentity() {
            CiamUserIdentityDo existing = stubIdentity("other-user", IdentityType.MOBILE,
                    "13800138000", IdentityStatus.BOUND.getCode());
            when(identityRepository.findByTypeAndHash(eq("mobile"), anyString()))
                    .thenReturn(Optional.of(existing));

            Optional<CiamUserIdentityDo> result = service.checkConflictDetail(
                    IdentityType.MOBILE, "13800138000", "user-001");

            assertTrue(result.isPresent());
            assertEquals("other-user", result.get().getUserId());
        }

        @Test
        void checkConflictDetail_returnsEmptyWhenSameUser() {
            CiamUserIdentityDo existing = stubIdentity("user-001", IdentityType.MOBILE,
                    "13800138000", IdentityStatus.BOUND.getCode());
            when(identityRepository.findByTypeAndHash(eq("mobile"), anyString()))
                    .thenReturn(Optional.of(existing));

            Optional<CiamUserIdentityDo> result = service.checkConflictDetail(
                    IdentityType.MOBILE, "13800138000", "user-001");

            assertTrue(result.isEmpty());
        }

        @Test
        void checkConflictDetail_returnsEmptyWhenUnbound() {
            CiamUserIdentityDo existing = stubIdentity("other-user", IdentityType.EMAIL,
                    "test@example.com", IdentityStatus.UNBOUND.getCode());
            when(identityRepository.findByTypeAndHash(eq("email"), anyString()))
                    .thenReturn(Optional.of(existing));

            Optional<CiamUserIdentityDo> result = service.checkConflictDetail(
                    IdentityType.EMAIL, "test@example.com", "user-001");

            assertTrue(result.isEmpty());
        }

        @Test
        void checkConflictDetail_returnsEmptyWhenNoRecord() {
            when(identityRepository.findByTypeAndHash(anyString(), anyString()))
                    .thenReturn(Optional.empty());

            Optional<CiamUserIdentityDo> result = service.checkConflictDetail(
                    IdentityType.GOOGLE, "google-sub-123", "user-001");

            assertTrue(result.isEmpty());
        }
    }
}
