package net.hwyz.iov.cloud.sec.ciam.service.application;
import net.hwyz.iov.cloud.sec.ciam.service.application.service.*;

import net.hwyz.iov.cloud.sec.ciam.service.application.dto.UserConsentDto;
import net.hwyz.iov.cloud.sec.ciam.service.common.audit.AuditEvent;
import net.hwyz.iov.cloud.sec.ciam.service.common.audit.AuditLogger;
import net.hwyz.iov.cloud.sec.ciam.service.domain.model.UserConsent;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.UserConsentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * ConsentAppService 单元测试。
 */
class ConsentAppServiceTest {

    private UserConsentRepository consentRepository;
    private AuditLogger auditLogger;
    private ConsentAppService service;

    private static final String USER_ID = "user-consent-001";
    private static final String OPERATE_IP = "192.168.1.100";

    @BeforeEach
    void setUp() {
        consentRepository = mock(UserConsentRepository.class);
        auditLogger = mock(AuditLogger.class);
        when(consentRepository.insert(any())).thenReturn(1);
        when(consentRepository.updateByConsentId(any())).thenReturn(1);

        service = new ConsentAppService(consentRepository, auditLogger);
    }

    // ========== grantConsent ==========

    @Nested
    class GrantConsentTests {

        @Test
        void grantsUserAgreementConsentSuccessfully() {
            UserConsentDto result = service.grantConsent(
                    USER_ID, "user_agreement", "v2.0", "app", "mobile", OPERATE_IP);

            assertNotNull(result);
            assertEquals(USER_ID, result.getUserId());
            assertEquals("user_agreement", result.getConsentType());
            assertEquals(1, result.getConsentStatus());

            verify(consentRepository).insert(any(UserConsent.class));

            ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
            verify(auditLogger).log(captor.capture());
            assertEquals("CONSENT", captor.getValue().getEventType());
            assertEquals("同意授予", captor.getValue().getEventName());
        }

        @Test
        void grantsPrivacyPolicyConsentSuccessfully() {
            UserConsentDto result = service.grantConsent(
                    USER_ID, "privacy_policy", "v1.5", "web", "browser", OPERATE_IP);

            assertNotNull(result);
            assertEquals("privacy_policy", result.getConsentType());
            verify(consentRepository).insert(any(UserConsent.class));
        }

        @Test
        void grantsMarketingConsentSuccessfully() {
            UserConsentDto result = service.grantConsent(
                    USER_ID, "marketing", "v1.0", "app", "mobile", OPERATE_IP);

            assertNotNull(result);
            assertEquals("marketing", result.getConsentType());
            verify(consentRepository).insert(any(UserConsent.class));
        }
    }

    // ========== withdrawMarketingConsent ==========

    @Nested
    class WithdrawMarketingConsentTests {

        @Test
        void withdrawsMarketingConsentSuccessfully() {
            UserConsent activeConsent = stubConsent("marketing", 1);
            when(consentRepository.findByUserIdAndConsentType(USER_ID, "marketing"))
                    .thenReturn(List.of(activeConsent));

            service.withdrawMarketingConsent(USER_ID, OPERATE_IP);

            ArgumentCaptor<UserConsent> captor = ArgumentCaptor.forClass(UserConsent.class);
            verify(consentRepository).updateByConsentId(captor.capture());
            assertEquals(0, captor.getValue().getConsentStatus());
        }
    }

    // ========== getConsentRecords ==========

    @Nested
    class GetConsentRecordsTests {

        @Test
        void returnsAllConsentRecords() {
            List<UserConsent> records = List.of(
                    stubConsent("user_agreement", 1),
                    stubConsent("privacy_policy", 1),
                    stubConsent("marketing", 1));
            when(consentRepository.findByUserId(USER_ID)).thenReturn(records);

            List<UserConsentDto> result = service.getConsentRecords(USER_ID);

            assertEquals(3, result.size());
            verify(consentRepository).findByUserId(USER_ID);
        }

        @Test
        void returnsEmptyListWhenNoRecords() {
            when(consentRepository.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            List<UserConsentDto> result = service.getConsentRecords(USER_ID);

            assertTrue(result.isEmpty());
        }
    }

    // ========== getConsentByType ==========

    @Nested
    class GetConsentByTypeTests {

        @Test
        void returnsConsentByType() {
            UserConsent consent = stubConsent("marketing", 1);
            when(consentRepository.findByUserIdAndConsentType(USER_ID, "marketing"))
                    .thenReturn(List.of(consent));

            List<UserConsentDto> result = service.getConsentByType(USER_ID, "marketing");

            assertEquals(1, result.size());
            assertEquals("marketing", result.get(0).getConsentType());
        }
    }

    // ========== requestDataExport ==========

    @Nested
    class RequestDataExportTests {

        @Test
        void logsAuditEventForDataExport() {
            service.requestDataExport(USER_ID);

            ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
            verify(auditLogger).log(captor.capture());
            assertEquals("CONSENT", captor.getValue().getEventType());
            assertEquals("数据导出请求", captor.getValue().getEventName());
        }
    }

    // ========== requestDataDeletion ==========

    @Nested
    class RequestDataDeletionTests {

        @Test
        void logsAuditEventForDataDeletion() {
            service.requestDataDeletion(USER_ID);

            ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
            verify(auditLogger).log(captor.capture());
            assertEquals("CONSENT", captor.getValue().getEventType());
            assertEquals("数据删除请求", captor.getValue().getEventName());
        }
    }

    // ========== helpers ==========

    private UserConsent stubConsent(String consentType, int status) {
        UserConsent consent = new UserConsent();
        consent.setConsentId("consent-" + consentType + "-" + status);
        consent.setUserId(USER_ID);
        consent.setConsentType(consentType);
        consent.setConsentStatus(status);
        consent.setPolicyVersion("v1.0");
        consent.setSourceChannel("app");
        consent.setClientType("mobile");
        return consent;
    }
}
