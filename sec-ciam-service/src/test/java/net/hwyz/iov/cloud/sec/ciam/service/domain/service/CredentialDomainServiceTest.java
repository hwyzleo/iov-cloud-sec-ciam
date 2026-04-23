package net.hwyz.iov.cloud.sec.ciam.service.domain.service;

import net.hwyz.iov.cloud.framework.common.exception.BusinessException;
import net.hwyz.iov.cloud.sec.ciam.service.common.exception.CiamErrorCode;
import net.hwyz.iov.cloud.sec.ciam.service.common.security.PasswordEncoder;
import net.hwyz.iov.cloud.sec.ciam.service.domain.enums.CredentialStatus;
import net.hwyz.iov.cloud.sec.ciam.service.domain.enums.CredentialType;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.UserCredentialRepository;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.po.UserCredentialPo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class CredentialDomainServiceTest {

    private UserCredentialRepository credentialRepository;
    private PasswordEncoder passwordEncoder;
    private PasswordPolicyService passwordPolicyService;
    private CredentialDomainService service;

    /** A password that satisfies the complexity policy */
    private static final String VALID_PASSWORD = "P@ssw0rd!";

    @BeforeEach
    void setUp() {
        credentialRepository = mock(UserCredentialRepository.class);
        passwordEncoder = new PasswordEncoder(4); // low strength for fast tests
        passwordPolicyService = new PasswordPolicyService();
        when(credentialRepository.insert(any())).thenReturn(1);
        when(credentialRepository.updateByCredentialId(any())).thenReturn(1);
        service = new CredentialDomainService(credentialRepository, passwordEncoder, passwordPolicyService);
    }

    private UserCredentialPo stubCredential(String userId, String rawPassword, int status) {
        UserCredentialPo cred = new UserCredentialPo();
        cred.setCredentialId("cred-001");
        cred.setUserId(userId);
        cred.setCredentialType(CredentialType.EMAIL_PASSWORD.getCode());
        cred.setCredentialHash(passwordEncoder.encode(rawPassword));
        cred.setHashAlgorithm(PasswordEncoder.ALGORITHM);
        cred.setFailCount(0);
        cred.setCredentialStatus(status);
        cred.setRowValid(1);
        return cred;
    }

    // ---- setPassword ----

    @Nested
    class SetPasswordTests {

        @Test
        void setPassword_createsNewCredential() {
            when(credentialRepository.findByUserIdAndType(eq("user-001"), eq("email_password")))
                    .thenReturn(Optional.empty());

            UserCredentialPo result = service.setPassword("user-001", VALID_PASSWORD);

            assertNotNull(result.getCredentialId());
            assertEquals(32, result.getCredentialId().length());
            assertEquals("user-001", result.getUserId());
            assertEquals("email_password", result.getCredentialType());
            assertEquals(PasswordEncoder.ALGORITHM, result.getHashAlgorithm());
            assertEquals(CredentialStatus.VALID.getCode(), result.getCredentialStatus());
            assertEquals(0, result.getFailCount());
            assertNotNull(result.getPasswordSetTime());
            assertNotNull(result.getCreateTime());
            assertNotNull(result.getModifyTime());
            // hash should not be plaintext
            assertNotEquals(VALID_PASSWORD, result.getCredentialHash());
            // hash should be verifiable
            assertTrue(passwordEncoder.matches(VALID_PASSWORD, result.getCredentialHash()));
            verify(credentialRepository).insert(any(UserCredentialPo.class));
        }

        @Test
        void setPassword_throwsWhenActiveCredentialExists() {
            UserCredentialPo existing = stubCredential("user-001", "OldPass1!", CredentialStatus.VALID.getCode());
            when(credentialRepository.findByUserIdAndType(eq("user-001"), eq("email_password")))
                    .thenReturn(Optional.of(existing));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.setPassword("user-001", VALID_PASSWORD));
            assertEquals(CiamErrorCode.CREDENTIAL_ALREADY_EXISTS, ex.getErrorCode());
            verify(credentialRepository, never()).insert(any());
        }

        @Test
        void setPassword_allowsWhenExistingCredentialIsInvalid() {
            UserCredentialPo existing = stubCredential("user-001", "OldPass1!", CredentialStatus.INVALID.getCode());
            when(credentialRepository.findByUserIdAndType(eq("user-001"), eq("email_password")))
                    .thenReturn(Optional.of(existing));

            UserCredentialPo result = service.setPassword("user-001", VALID_PASSWORD);

            assertNotNull(result);
            verify(credentialRepository).insert(any(UserCredentialPo.class));
        }
    }

    // ---- verifyPassword ----

    @Nested
    class VerifyPasswordTests {

        @Test
        void verifyPassword_returnsSuccessOnMatch() {
            UserCredentialPo cred = stubCredential("user-001", "Correct1!", CredentialStatus.VALID.getCode());
            when(credentialRepository.findByUserIdAndType(eq("user-001"), eq("email_password")))
                    .thenReturn(Optional.of(cred));

            PasswordVerifyResult result = service.verifyPassword("user-001", "Correct1!");
            assertTrue(result.isMatched());
            assertFalse(result.isChallengeRequired());
            assertFalse(result.isLocked());
            verify(credentialRepository).updateByCredentialId(any());
        }

        @Test
        void verifyPassword_returnsFailOnMismatch() {
            UserCredentialPo cred = stubCredential("user-001", "Correct1!", CredentialStatus.VALID.getCode());
            when(credentialRepository.findByUserIdAndType(eq("user-001"), eq("email_password")))
                    .thenReturn(Optional.of(cred));

            PasswordVerifyResult result = service.verifyPassword("user-001", "Wrong1!");
            assertFalse(result.isMatched());
            assertEquals(1, result.getFailCount());
            verify(credentialRepository).updateByCredentialId(any());
        }

        @Test
        void verifyPassword_throwsWhenNoCredential() {
            when(credentialRepository.findByUserIdAndType(eq("user-001"), eq("email_password")))
                    .thenReturn(Optional.empty());

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.verifyPassword("user-001", "Any1!"));
            assertEquals(CiamErrorCode.CREDENTIAL_INVALID, ex.getErrorCode());
        }

        @Test
        void verifyPassword_throwsWhenCredentialInvalid() {
            UserCredentialPo cred = stubCredential("user-001", "Pass1!", CredentialStatus.INVALID.getCode());
            when(credentialRepository.findByUserIdAndType(eq("user-001"), eq("email_password")))
                    .thenReturn(Optional.of(cred));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.verifyPassword("user-001", "Pass1!"));
            assertEquals(CiamErrorCode.CREDENTIAL_INVALID, ex.getErrorCode());
        }

        @Test
        void verifyPassword_resetsFailCountOnSuccess() {
            UserCredentialPo cred = stubCredential("user-001", "Correct1!", CredentialStatus.VALID.getCode());
            cred.setFailCount(3);
            when(credentialRepository.findByUserIdAndType(eq("user-001"), eq("email_password")))
                    .thenReturn(Optional.of(cred));

            PasswordVerifyResult result = service.verifyPassword("user-001", "Correct1!");
            assertTrue(result.isMatched());
            assertEquals(0, cred.getFailCount());
            assertNull(cred.getLockedUntil());
            assertNotNull(cred.getLastVerifyTime());
        }
    }

    // ---- changePassword ----

    @Nested
    class ChangePasswordTests {

        @Test
        void changePassword_updatesHashWhenOldPasswordMatches() {
            UserCredentialPo cred = stubCredential("user-001", "OldPass1!", CredentialStatus.VALID.getCode());
            String oldHash = cred.getCredentialHash();
            when(credentialRepository.findByUserIdAndType(eq("user-001"), eq("email_password")))
                    .thenReturn(Optional.of(cred));

            service.changePassword("user-001", "OldPass1!", VALID_PASSWORD);

            assertNotEquals(oldHash, cred.getCredentialHash());
            assertTrue(passwordEncoder.matches(VALID_PASSWORD, cred.getCredentialHash()));
            assertEquals(0, cred.getFailCount());
            assertNotNull(cred.getPasswordSetTime());
            verify(credentialRepository).updateByCredentialId(any());
        }

        @Test
        void changePassword_throwsWhenOldPasswordWrong() {
            UserCredentialPo cred = stubCredential("user-001", "OldPass1!", CredentialStatus.VALID.getCode());
            when(credentialRepository.findByUserIdAndType(eq("user-001"), eq("email_password")))
                    .thenReturn(Optional.of(cred));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.changePassword("user-001", "WrongOld!", VALID_PASSWORD));
            assertEquals(CiamErrorCode.CREDENTIAL_INVALID, ex.getErrorCode());
            verify(credentialRepository, never()).updateByCredentialId(any());
        }

        @Test
        void changePassword_throwsWhenNoCredential() {
            when(credentialRepository.findByUserIdAndType(eq("user-001"), eq("email_password")))
                    .thenReturn(Optional.empty());

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.changePassword("user-001", "Old1!", VALID_PASSWORD));
            assertEquals(CiamErrorCode.CREDENTIAL_INVALID, ex.getErrorCode());
        }
    }

    // ---- resetPassword ----

    @Nested
    class ResetPasswordTests {

        @Test
        void resetPassword_updatesHashWithoutOldPasswordCheck() {
            UserCredentialPo cred = stubCredential("user-001", "OldPass1!", CredentialStatus.VALID.getCode());
            cred.setFailCount(5);
            when(credentialRepository.findByUserIdAndType(eq("user-001"), eq("email_password")))
                    .thenReturn(Optional.of(cred));

            service.resetPassword("user-001", VALID_PASSWORD);

            assertTrue(passwordEncoder.matches(VALID_PASSWORD, cred.getCredentialHash()));
            assertEquals(0, cred.getFailCount());
            assertNull(cred.getLockedUntil());
            assertNotNull(cred.getPasswordSetTime());
            verify(credentialRepository).updateByCredentialId(any());
        }

        @Test
        void resetPassword_throwsWhenNoCredential() {
            when(credentialRepository.findByUserIdAndType(eq("user-001"), eq("email_password")))
                    .thenReturn(Optional.empty());

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.resetPassword("user-001", VALID_PASSWORD));
            assertEquals(CiamErrorCode.CREDENTIAL_INVALID, ex.getErrorCode());
        }

        @Test
        void resetPassword_clearsLockedUntil() {
            UserCredentialPo cred = stubCredential("user-001", "OldPass1!", CredentialStatus.VALID.getCode());
            cred.setLockedUntil(java.time.Instant.now().plusSeconds(30 * 60));
            when(credentialRepository.findByUserIdAndType(eq("user-001"), eq("email_password")))
                    .thenReturn(Optional.of(cred));

            service.resetPassword("user-001", VALID_PASSWORD);

            assertNull(cred.getLockedUntil());
        }
    }

    // ---- findActiveCredential ----

    @Nested
    class FindActiveCredentialTests {

        @Test
        void findActiveCredential_returnsCredentialWhenValid() {
            UserCredentialPo cred = stubCredential("user-001", "Pass1!", CredentialStatus.VALID.getCode());
            when(credentialRepository.findByUserIdAndType(eq("user-001"), eq("email_password")))
                    .thenReturn(Optional.of(cred));

            Optional<UserCredentialPo> result = service.findActiveCredential("user-001", CredentialType.EMAIL_PASSWORD);

            assertTrue(result.isPresent());
            assertEquals("cred-001", result.get().getCredentialId());
        }

        @Test
        void findActiveCredential_returnsEmptyWhenInvalid() {
            UserCredentialPo cred = stubCredential("user-001", "Pass1!", CredentialStatus.INVALID.getCode());
            when(credentialRepository.findByUserIdAndType(eq("user-001"), eq("email_password")))
                    .thenReturn(Optional.of(cred));

            Optional<UserCredentialPo> result = service.findActiveCredential("user-001", CredentialType.EMAIL_PASSWORD);

            assertTrue(result.isEmpty());
        }

        @Test
        void findActiveCredential_returnsEmptyWhenNotFound() {
            when(credentialRepository.findByUserIdAndType(eq("user-001"), eq("email_password")))
                    .thenReturn(Optional.empty());

            Optional<UserCredentialPo> result = service.findActiveCredential("user-001", CredentialType.EMAIL_PASSWORD);

            assertTrue(result.isEmpty());
        }
    }

    // ---- credential type enforcement ----

    @Nested
    class CredentialTypeTests {

        @Test
        void setPassword_alwaysUsesEmailPasswordType() {
            when(credentialRepository.findByUserIdAndType(anyString(), anyString()))
                    .thenReturn(Optional.empty());

            UserCredentialPo result = service.setPassword("user-001", VALID_PASSWORD);

            assertEquals(CredentialType.EMAIL_PASSWORD.getCode(), result.getCredentialType());
        }

        @Test
        void setPassword_alwaysUsesBcryptAlgorithm() {
            when(credentialRepository.findByUserIdAndType(anyString(), anyString()))
                    .thenReturn(Optional.empty());

            UserCredentialPo result = service.setPassword("user-001", VALID_PASSWORD);

            assertEquals("BCRYPT", result.getHashAlgorithm());
            // BCrypt hashes start with $2a$ or $2b$
            assertTrue(result.getCredentialHash().startsWith("$2"));
        }
    }

    // ---- password policy enforcement ----

    @Nested
    class PasswordPolicyEnforcementTests {

        @Test
        void setPassword_rejectsWeakPassword() {
            when(credentialRepository.findByUserIdAndType(anyString(), anyString()))
                    .thenReturn(Optional.empty());

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.setPassword("user-001", "weak"));
            assertEquals(CiamErrorCode.PASSWORD_COMPLEXITY_INSUFFICIENT, ex.getErrorCode());
            verify(credentialRepository, never()).insert(any());
        }

        @Test
        void changePassword_rejectsWeakNewPassword() {
            UserCredentialPo cred = stubCredential("user-001", "OldPass1!", CredentialStatus.VALID.getCode());
            when(credentialRepository.findByUserIdAndType(eq("user-001"), eq("email_password")))
                    .thenReturn(Optional.of(cred));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.changePassword("user-001", "OldPass1!", "nodigit!A"));
            assertEquals(CiamErrorCode.PASSWORD_COMPLEXITY_INSUFFICIENT, ex.getErrorCode());
        }

        @Test
        void resetPassword_rejectsWeakNewPassword() {
            UserCredentialPo cred = stubCredential("user-001", "OldPass1!", CredentialStatus.VALID.getCode());
            when(credentialRepository.findByUserIdAndType(eq("user-001"), eq("email_password")))
                    .thenReturn(Optional.of(cred));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.resetPassword("user-001", "12345678"));
            assertEquals(CiamErrorCode.PASSWORD_COMPLEXITY_INSUFFICIENT, ex.getErrorCode());
        }
    }

    // ---- failure lockout ----

    @Nested
    class FailureLockoutTests {

        @Test
        void verifyPassword_incrementsFailCountOnFailure() {
            UserCredentialPo cred = stubCredential("user-001", "Correct1!", CredentialStatus.VALID.getCode());
            cred.setFailCount(0);
            when(credentialRepository.findByUserIdAndType(eq("user-001"), eq("email_password")))
                    .thenReturn(Optional.of(cred));

            PasswordVerifyResult result = service.verifyPassword("user-001", "Wrong1!");

            assertFalse(result.isMatched());
            assertFalse(result.isChallengeRequired());
            assertFalse(result.isLocked());
            assertEquals(1, result.getFailCount());
            assertEquals(1, cred.getFailCount());
        }

        @Test
        void verifyPassword_triggersChallengeAt3Failures() {
            UserCredentialPo cred = stubCredential("user-001", "Correct1!", CredentialStatus.VALID.getCode());
            cred.setFailCount(2); // next failure will be 3rd
            when(credentialRepository.findByUserIdAndType(eq("user-001"), eq("email_password")))
                    .thenReturn(Optional.of(cred));

            PasswordVerifyResult result = service.verifyPassword("user-001", "Wrong1!");

            assertFalse(result.isMatched());
            assertTrue(result.isChallengeRequired());
            assertFalse(result.isLocked());
            assertEquals(3, result.getFailCount());
        }

        @Test
        void verifyPassword_triggersChallengeAt4Failures() {
            UserCredentialPo cred = stubCredential("user-001", "Correct1!", CredentialStatus.VALID.getCode());
            cred.setFailCount(3);
            when(credentialRepository.findByUserIdAndType(eq("user-001"), eq("email_password")))
                    .thenReturn(Optional.of(cred));

            PasswordVerifyResult result = service.verifyPassword("user-001", "Wrong1!");

            assertTrue(result.isChallengeRequired());
            assertFalse(result.isLocked());
            assertEquals(4, result.getFailCount());
        }

        @Test
        void verifyPassword_locksAccountAt5Failures() {
            UserCredentialPo cred = stubCredential("user-001", "Correct1!", CredentialStatus.VALID.getCode());
            cred.setFailCount(4); // next failure will be 5th
            when(credentialRepository.findByUserIdAndType(eq("user-001"), eq("email_password")))
                    .thenReturn(Optional.of(cred));

            PasswordVerifyResult result = service.verifyPassword("user-001", "Wrong1!");

            assertFalse(result.isMatched());
            assertTrue(result.isChallengeRequired());
            assertTrue(result.isLocked());
            assertEquals(5, result.getFailCount());
            assertNotNull(cred.getLockedUntil());
        }

        @Test
        void verifyPassword_throwsAccountLockedWhenStillLocked() {
            UserCredentialPo cred = stubCredential("user-001", "Correct1!", CredentialStatus.VALID.getCode());
            cred.setFailCount(5);
            cred.setLockedUntil(java.time.Instant.now().plusSeconds(29 * 60));
            when(credentialRepository.findByUserIdAndType(eq("user-001"), eq("email_password")))
                    .thenReturn(Optional.of(cred));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.verifyPassword("user-001", "Correct1!"));
            assertEquals(CiamErrorCode.ACCOUNT_LOCKED, ex.getErrorCode());
        }

        @Test
        void verifyPassword_allowsLoginAfterLockExpires() {
            UserCredentialPo cred = stubCredential("user-001", "Correct1!", CredentialStatus.VALID.getCode());
            cred.setFailCount(5);
            cred.setLockedUntil(java.time.Instant.now().minusSeconds(1 * 60)); // lock expired
            when(credentialRepository.findByUserIdAndType(eq("user-001"), eq("email_password")))
                    .thenReturn(Optional.of(cred));

            PasswordVerifyResult result = service.verifyPassword("user-001", "Correct1!");

            assertTrue(result.isMatched());
            assertEquals(0, cred.getFailCount());
            assertNull(cred.getLockedUntil());
        }

        @Test
        void verifyPassword_successResetsFailCountAndLockedUntil() {
            UserCredentialPo cred = stubCredential("user-001", "Correct1!", CredentialStatus.VALID.getCode());
            cred.setFailCount(4);
            cred.setLockedUntil(null);
            when(credentialRepository.findByUserIdAndType(eq("user-001"), eq("email_password")))
                    .thenReturn(Optional.of(cred));

            PasswordVerifyResult result = service.verifyPassword("user-001", "Correct1!");

            assertTrue(result.isMatched());
            assertEquals(0, cred.getFailCount());
            assertNull(cred.getLockedUntil());
        }
    }
}
