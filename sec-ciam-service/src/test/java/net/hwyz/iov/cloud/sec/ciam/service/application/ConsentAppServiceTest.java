package net.hwyz.iov.cloud.sec.ciam.service.application;

import net.hwyz.iov.cloud.framework.common.exception.BusinessException;
import net.hwyz.iov.cloud.sec.ciam.service.common.audit.AuditEvent;
import net.hwyz.iov.cloud.sec.ciam.service.common.audit.AuditLogger;
import net.hwyz.iov.cloud.sec.ciam.service.common.exception.CiamErrorCode;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.CiamUserConsentRepository;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.dataobject.CiamUserConsentDo;
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
 * <p>
 * 仅 mock 底层仓储接口与审计日志，与项目现有测试风格保持一致。
 */
class ConsentAppServiceTest {

    private CiamUserConsentRepository consentRepository;
    private AuditLogger auditLogger;
    private ConsentAppService service;

    private static final String USER_ID = "user-consent-001";
    private static final String OPERATE_IP = "192.168.1.100";

    @BeforeEach
    void setUp() {
        consentRepository = mock(CiamUserConsentRepository.class);
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
            CiamUserConsentDo result = service.grantConsent(
                    USER_ID, "user_agreement", "v2.0", "app", "mobile", OPERATE_IP);

            assertNotNull(result);
            assertNotNull(result.getConsentId());
            assertEquals(USER_ID, result.getUserId());
            assertEquals("user_agreement", result.getConsentType());
            assertEquals(1, result.getConsentStatus());
            assertEquals("v2.0", result.getPolicyVersion());
            assertEquals("app", result.getSourceChannel());
            assertEquals("mobile", result.getClientType());
            assertEquals(OPERATE_IP, result.getOperateIp());
            assertNotNull(result.getOperateTime());
            assertEquals(1, result.getRowValid());

            verify(consentRepository).insert(any(CiamUserConsentDo.class));

            ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
            verify(auditLogger).log(captor.capture());
            assertEquals("CONSENT", captor.getValue().getEventType());
            assertEquals("同意授予", captor.getValue().getEventName());
            assertTrue(captor.getValue().isSuccess());
        }

        @Test
        void grantsPrivacyPolicyConsentSuccessfully() {
            CiamUserConsentDo result = service.grantConsent(
                    USER_ID, "privacy_policy", "v1.5", "web", "browser", OPERATE_IP);

            assertNotNull(result);
            assertEquals("privacy_policy", result.getConsentType());
            assertEquals("v1.5", result.getPolicyVersion());
            verify(consentRepository).insert(any(CiamUserConsentDo.class));
        }

        @Test
        void grantsMarketingConsentSuccessfully() {
            CiamUserConsentDo result = service.grantConsent(
                    USER_ID, "marketing", "v1.0", "app", "mobile", OPERATE_IP);

            assertNotNull(result);
            assertEquals("marketing", result.getConsentType());
            verify(consentRepository).insert(any(CiamUserConsentDo.class));
        }

        @Test
        void throwsWhenUserIdIsNull() {
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.grantConsent(null, "user_agreement", "v1.0", "app", "mobile", OPERATE_IP));
            assertEquals(CiamErrorCode.INVALID_PARAM, ex.getErrorCode());
        }

        @Test
        void throwsWhenUserIdIsBlank() {
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.grantConsent("  ", "user_agreement", "v1.0", "app", "mobile", OPERATE_IP));
            assertEquals(CiamErrorCode.INVALID_PARAM, ex.getErrorCode());
        }

        @Test
        void throwsWhenConsentTypeIsInvalid() {
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.grantConsent(USER_ID, "unknown_type", "v1.0", "app", "mobile", OPERATE_IP));
            assertEquals(CiamErrorCode.INVALID_PARAM, ex.getErrorCode());
        }

        @Test
        void throwsWhenConsentTypeIsNull() {
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.grantConsent(USER_ID, null, "v1.0", "app", "mobile", OPERATE_IP));
            assertEquals(CiamErrorCode.INVALID_PARAM, ex.getErrorCode());
        }
    }

    // ========== withdrawMarketingConsent ==========

    @Nested
    class WithdrawMarketingConsentTests {

        @Test
        void withdrawsMarketingConsentSuccessfully() {
            CiamUserConsentDo activeConsent = stubConsent("marketing", 1);
            when(consentRepository.findByUserIdAndConsentType(USER_ID, "marketing"))
                    .thenReturn(List.of(activeConsent));

            service.withdrawMarketingConsent(USER_ID, OPERATE_IP);

            ArgumentCaptor<CiamUserConsentDo> captor = ArgumentCaptor.forClass(CiamUserConsentDo.class);
            verify(consentRepository).updateByConsentId(captor.capture());
            assertEquals(0, captor.getValue().getConsentStatus());
            assertEquals(OPERATE_IP, captor.getValue().getOperateIp());
            assertNotNull(captor.getValue().getOperateTime());

            ArgumentCaptor<AuditEvent> auditCaptor = ArgumentCaptor.forClass(AuditEvent.class);
            verify(auditLogger).log(auditCaptor.capture());
            assertEquals("CONSENT", auditCaptor.getValue().getEventType());
            assertEquals("同意撤回", auditCaptor.getValue().getEventName());
        }

        @Test
        void throwsWhenNoActiveMarketingConsent() {
            when(consentRepository.findByUserIdAndConsentType(USER_ID, "marketing"))
                    .thenReturn(Collections.emptyList());

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.withdrawMarketingConsent(USER_ID, OPERATE_IP));
            assertEquals(CiamErrorCode.INVALID_PARAM, ex.getErrorCode());
            verify(consentRepository, never()).updateByConsentId(any());
        }

        @Test
        void throwsWhenMarketingConsentAlreadyWithdrawn() {
            CiamUserConsentDo withdrawnConsent = stubConsent("marketing", 0);
            when(consentRepository.findByUserIdAndConsentType(USER_ID, "marketing"))
                    .thenReturn(List.of(withdrawnConsent));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.withdrawMarketingConsent(USER_ID, OPERATE_IP));
            assertEquals(CiamErrorCode.INVALID_PARAM, ex.getErrorCode());
        }

        @Test
        void throwsWhenUserIdIsNull() {
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.withdrawMarketingConsent(null, OPERATE_IP));
            assertEquals(CiamErrorCode.INVALID_PARAM, ex.getErrorCode());
        }
    }

    // ========== getConsentRecords ==========

    @Nested
    class GetConsentRecordsTests {

        @Test
        void returnsAllConsentRecords() {
            List<CiamUserConsentDo> records = List.of(
                    stubConsent("user_agreement", 1),
                    stubConsent("privacy_policy", 1),
                    stubConsent("marketing", 1));
            when(consentRepository.findByUserId(USER_ID)).thenReturn(records);

            List<CiamUserConsentDo> result = service.getConsentRecords(USER_ID);

            assertEquals(3, result.size());
            verify(consentRepository).findByUserId(USER_ID);
        }

        @Test
        void returnsEmptyListWhenNoRecords() {
            when(consentRepository.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            List<CiamUserConsentDo> result = service.getConsentRecords(USER_ID);

            assertTrue(result.isEmpty());
        }

        @Test
        void throwsWhenUserIdIsBlank() {
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.getConsentRecords(""));
            assertEquals(CiamErrorCode.INVALID_PARAM, ex.getErrorCode());
        }
    }

    // ========== getConsentByType ==========

    @Nested
    class GetConsentByTypeTests {

        @Test
        void returnsConsentByType() {
            CiamUserConsentDo consent = stubConsent("marketing", 1);
            when(consentRepository.findByUserIdAndConsentType(USER_ID, "marketing"))
                    .thenReturn(List.of(consent));

            List<CiamUserConsentDo> result = service.getConsentByType(USER_ID, "marketing");

            assertEquals(1, result.size());
            assertEquals("marketing", result.get(0).getConsentType());
        }

        @Test
        void throwsWhenConsentTypeIsInvalid() {
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.getConsentByType(USER_ID, "invalid"));
            assertEquals(CiamErrorCode.INVALID_PARAM, ex.getErrorCode());
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
            assertTrue(captor.getValue().isSuccess());
        }

        @Test
        void throwsWhenUserIdIsNull() {
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.requestDataExport(null));
            assertEquals(CiamErrorCode.INVALID_PARAM, ex.getErrorCode());
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
            assertTrue(captor.getValue().isSuccess());
        }

        @Test
        void throwsWhenUserIdIsNull() {
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.requestDataDeletion(null));
            assertEquals(CiamErrorCode.INVALID_PARAM, ex.getErrorCode());
        }
    }

    // ========== helpers ==========

    private CiamUserConsentDo stubConsent(String consentType, int status) {
        CiamUserConsentDo consent = new CiamUserConsentDo();
        consent.setConsentId("consent-" + consentType + "-" + status);
        consent.setUserId(USER_ID);
        consent.setConsentType(consentType);
        consent.setConsentStatus(status);
        consent.setPolicyVersion("v1.0");
        consent.setSourceChannel("app");
        consent.setClientType("mobile");
        consent.setRowValid(1);
        return consent;
    }
}
