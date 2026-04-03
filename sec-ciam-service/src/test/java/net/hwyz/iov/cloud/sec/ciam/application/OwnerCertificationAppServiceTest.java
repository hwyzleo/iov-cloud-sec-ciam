package net.hwyz.iov.cloud.sec.ciam.application;

import net.hwyz.iov.cloud.framework.common.exception.BusinessException;
import net.hwyz.iov.cloud.sec.ciam.common.audit.AuditEvent;
import net.hwyz.iov.cloud.sec.ciam.common.audit.AuditLogger;
import net.hwyz.iov.cloud.sec.ciam.common.exception.CiamErrorCode;
import net.hwyz.iov.cloud.sec.ciam.domain.enums.CertStatus;
import net.hwyz.iov.cloud.sec.ciam.domain.enums.TagStatus;
import net.hwyz.iov.cloud.sec.ciam.domain.repository.CiamOwnerCertStateRepository;
import net.hwyz.iov.cloud.sec.ciam.domain.repository.CiamUserTagRepository;
import net.hwyz.iov.cloud.sec.ciam.domain.service.TagDomainService;
import net.hwyz.iov.cloud.sec.ciam.infrastructure.repository.dao.dataobject.CiamOwnerCertStateDo;
import net.hwyz.iov.cloud.sec.ciam.infrastructure.repository.dao.dataobject.CiamUserTagDo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * OwnerCertificationAppService 单元测试。
 * <p>
 * Mock 仓储接口与审计日志，使用真实 TagDomainService（注入 mock 仓储），
 * 与项目现有测试风格一致（仅 mock 接口，不 mock 具体类）。
 */
class OwnerCertificationAppServiceTest {

    private CiamOwnerCertStateRepository certStateRepository;
    private CiamUserTagRepository tagRepository;
    private AuditLogger auditLogger;
    private OwnerCertificationAppService service;

    private static final String USER_ID = "user-cert-001";
    private static final String VIN = "LSVAU2180N2000001";
    private static final String CERT_SOURCE = "external_cert_service";

    @BeforeEach
    void setUp() {
        certStateRepository = mock(CiamOwnerCertStateRepository.class);
        tagRepository = mock(CiamUserTagRepository.class);
        auditLogger = mock(AuditLogger.class);

        when(certStateRepository.insert(any())).thenReturn(1);
        when(certStateRepository.updateByOwnerCertId(any())).thenReturn(1);
        when(tagRepository.insert(any())).thenReturn(1);
        when(tagRepository.updateByTagId(any())).thenReturn(1);

        TagDomainService tagDomainService = new TagDomainService(tagRepository);
        service = new OwnerCertificationAppService(
                certStateRepository, tagDomainService, auditLogger);
    }

    private CiamOwnerCertStateDo stubCertRecord(int certStatus) {
        CiamOwnerCertStateDo record = new CiamOwnerCertStateDo();
        record.setOwnerCertId("cert-001");
        record.setUserId(USER_ID);
        record.setVin(VIN);
        record.setCertStatus(certStatus);
        record.setCertSource(CERT_SOURCE);
        record.setRowVersion(1);
        record.setRowValid(1);
        return record;
    }

    private CiamUserTagDo stubTag(int status) {
        CiamUserTagDo tag = new CiamUserTagDo();
        tag.setTagId("tag-001");
        tag.setUserId(USER_ID);
        tag.setTagCode("owner_verified");
        tag.setTagName("已车主认证");
        tag.setTagStatus(status);
        tag.setTagSource(CERT_SOURCE);
        tag.setRowValid(1);
        return tag;
    }

    // ---- handleCertificationCallback ----

    @Nested
    class HandleCertificationCallbackTests {

        @Test
        void createsNewRecordWhenNoExistingRecord() {
            when(certStateRepository.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(tagRepository.findByUserIdAndTagCode(USER_ID, "owner_verified"))
                    .thenReturn(Optional.empty());

            service.handleCertificationCallback(USER_ID, CertStatus.CERTIFIED.getCode(),
                    VIN, CERT_SOURCE);

            ArgumentCaptor<CiamOwnerCertStateDo> captor =
                    ArgumentCaptor.forClass(CiamOwnerCertStateDo.class);
            verify(certStateRepository).insert(captor.capture());
            CiamOwnerCertStateDo created = captor.getValue();
            assertEquals(USER_ID, created.getUserId());
            assertEquals(VIN, created.getVin());
            assertEquals(CertStatus.CERTIFIED.getCode(), created.getCertStatus());
            assertEquals(CERT_SOURCE, created.getCertSource());
            assertNotNull(created.getOwnerCertId());
            assertNotNull(created.getCallbackTime());
            assertNotNull(created.getEffectiveTime());
            assertEquals(1, created.getRowValid());
        }

        @Test
        void updatesExistingRecordWhenMatchingVin() {
            CiamOwnerCertStateDo existing = stubCertRecord(CertStatus.CERTIFYING.getCode());
            when(certStateRepository.findByUserId(USER_ID)).thenReturn(List.of(existing));
            when(tagRepository.findByUserIdAndTagCode(USER_ID, "owner_verified"))
                    .thenReturn(Optional.empty());

            service.handleCertificationCallback(USER_ID, CertStatus.CERTIFIED.getCode(),
                    VIN, CERT_SOURCE);

            verify(certStateRepository, never()).insert(any());
            ArgumentCaptor<CiamOwnerCertStateDo> captor =
                    ArgumentCaptor.forClass(CiamOwnerCertStateDo.class);
            verify(certStateRepository).updateByOwnerCertId(captor.capture());
            assertEquals(CertStatus.CERTIFIED.getCode(), captor.getValue().getCertStatus());
            assertNotNull(captor.getValue().getEffectiveTime());
        }

        @Test
        void addsOwnerTagWhenCertified() {
            when(certStateRepository.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(tagRepository.findByUserIdAndTagCode(USER_ID, "owner_verified"))
                    .thenReturn(Optional.empty());

            service.handleCertificationCallback(USER_ID, CertStatus.CERTIFIED.getCode(),
                    VIN, CERT_SOURCE);

            verify(tagRepository).insert(argThat(tag ->
                    "owner_verified".equals(tag.getTagCode())
                            && USER_ID.equals(tag.getUserId())));
        }

        @Test
        void removesOwnerTagWhenCertFailed() {
            when(certStateRepository.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            CiamUserTagDo existingTag = stubTag(TagStatus.VALID.getCode());
            when(tagRepository.findByUserIdAndTagCode(USER_ID, "owner_verified"))
                    .thenReturn(Optional.of(existingTag));

            service.handleCertificationCallback(USER_ID, CertStatus.CERT_FAILED.getCode(),
                    VIN, CERT_SOURCE);

            ArgumentCaptor<CiamUserTagDo> tagCaptor = ArgumentCaptor.forClass(CiamUserTagDo.class);
            verify(tagRepository).updateByTagId(tagCaptor.capture());
            assertEquals(TagStatus.INVALID.getCode(), tagCaptor.getValue().getTagStatus());
        }

        @Test
        void doesNotAddTagWhenAlreadyExists() {
            when(certStateRepository.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            CiamUserTagDo existingTag = stubTag(TagStatus.VALID.getCode());
            when(tagRepository.findByUserIdAndTagCode(USER_ID, "owner_verified"))
                    .thenReturn(Optional.of(existingTag));

            service.handleCertificationCallback(USER_ID, CertStatus.CERTIFIED.getCode(),
                    VIN, CERT_SOURCE);

            // Should not insert a new tag (already exists)
            verify(tagRepository, never()).insert(any());
        }

        @Test
        void doesNotRemoveTagWhenNotExists() {
            when(certStateRepository.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(tagRepository.findByUserIdAndTagCode(USER_ID, "owner_verified"))
                    .thenReturn(Optional.empty());

            service.handleCertificationCallback(USER_ID, CertStatus.CERT_FAILED.getCode(),
                    VIN, CERT_SOURCE);

            verify(tagRepository, never()).updateByTagId(any());
        }

        @Test
        void logsAuditEventOnCallback() {
            when(certStateRepository.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(tagRepository.findByUserIdAndTagCode(USER_ID, "owner_verified"))
                    .thenReturn(Optional.empty());

            service.handleCertificationCallback(USER_ID, CertStatus.CERTIFIED.getCode(),
                    VIN, CERT_SOURCE);

            ArgumentCaptor<AuditEvent> auditCaptor = ArgumentCaptor.forClass(AuditEvent.class);
            verify(auditLogger).log(auditCaptor.capture());
            AuditEvent event = auditCaptor.getValue();
            assertEquals("OWNER_CERT", event.getEventType());
            assertEquals("车主认证回调", event.getEventName());
            assertTrue(event.isSuccess());
            assertEquals(USER_ID, event.getUserId());
        }

        @Test
        void throwsWhenUserIdIsBlank() {
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.handleCertificationCallback("",
                            CertStatus.CERTIFIED.getCode(), VIN, CERT_SOURCE));
            assertEquals(CiamErrorCode.INVALID_PARAM, ex.getErrorCode());
        }

        @Test
        void throwsWhenUserIdIsNull() {
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.handleCertificationCallback(null,
                            CertStatus.CERTIFIED.getCode(), VIN, CERT_SOURCE));
            assertEquals(CiamErrorCode.INVALID_PARAM, ex.getErrorCode());
        }

        @Test
        void throwsWhenCertResultIsInvalid() {
            assertThrows(IllegalArgumentException.class,
                    () -> service.handleCertificationCallback(USER_ID, 99, VIN, CERT_SOURCE));
        }
    }

    // ---- queryCertificationStatus ----

    @Nested
    class QueryCertificationStatusTests {

        @Test
        void returnsAllRecordsForUser() {
            CiamOwnerCertStateDo record = stubCertRecord(CertStatus.CERTIFIED.getCode());
            when(certStateRepository.findByUserId(USER_ID)).thenReturn(List.of(record));

            List<CiamOwnerCertStateDo> result = service.queryCertificationStatus(USER_ID);

            assertEquals(1, result.size());
            assertEquals(CertStatus.CERTIFIED.getCode(), result.get(0).getCertStatus());
        }

        @Test
        void returnsEmptyListWhenNoRecords() {
            when(certStateRepository.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            List<CiamOwnerCertStateDo> result = service.queryCertificationStatus(USER_ID);

            assertTrue(result.isEmpty());
        }

        @Test
        void logsAuditEventOnQuery() {
            when(certStateRepository.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            service.queryCertificationStatus(USER_ID);

            ArgumentCaptor<AuditEvent> auditCaptor = ArgumentCaptor.forClass(AuditEvent.class);
            verify(auditLogger).log(auditCaptor.capture());
            assertEquals("OWNER_CERT", auditCaptor.getValue().getEventType());
            assertEquals("车主认证查询", auditCaptor.getValue().getEventName());
        }

        @Test
        void throwsWhenUserIdIsBlank() {
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.queryCertificationStatus(""));
            assertEquals(CiamErrorCode.INVALID_PARAM, ex.getErrorCode());
        }
    }

    // ---- compensateCertificationStatus ----

    @Nested
    class CompensateCertificationStatusTests {

        @Test
        void updatesLastQueryTimeForPendingRecords() {
            CiamOwnerCertStateDo pending = stubCertRecord(CertStatus.CERTIFYING.getCode());
            when(certStateRepository.findByUserIdAndCertStatus(USER_ID,
                    CertStatus.CERTIFYING.getCode())).thenReturn(List.of(pending));

            List<CiamOwnerCertStateDo> result =
                    service.compensateCertificationStatus(USER_ID);

            assertEquals(1, result.size());
            ArgumentCaptor<CiamOwnerCertStateDo> captor =
                    ArgumentCaptor.forClass(CiamOwnerCertStateDo.class);
            verify(certStateRepository).updateByOwnerCertId(captor.capture());
            assertNotNull(captor.getValue().getLastQueryTime());
            assertNotNull(captor.getValue().getModifyTime());
        }

        @Test
        void returnsEmptyWhenNoPendingRecords() {
            when(certStateRepository.findByUserIdAndCertStatus(USER_ID,
                    CertStatus.CERTIFYING.getCode())).thenReturn(Collections.emptyList());

            List<CiamOwnerCertStateDo> result =
                    service.compensateCertificationStatus(USER_ID);

            assertTrue(result.isEmpty());
            verify(certStateRepository, never()).updateByOwnerCertId(any());
        }

        @Test
        void logsAuditEventOnCompensate() {
            when(certStateRepository.findByUserIdAndCertStatus(USER_ID,
                    CertStatus.CERTIFYING.getCode())).thenReturn(Collections.emptyList());

            service.compensateCertificationStatus(USER_ID);

            ArgumentCaptor<AuditEvent> auditCaptor = ArgumentCaptor.forClass(AuditEvent.class);
            verify(auditLogger).log(auditCaptor.capture());
            assertEquals("OWNER_CERT", auditCaptor.getValue().getEventType());
            assertEquals("车主认证补偿", auditCaptor.getValue().getEventName());
        }

        @Test
        void throwsWhenUserIdIsNull() {
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.compensateCertificationStatus(null));
            assertEquals(CiamErrorCode.INVALID_PARAM, ex.getErrorCode());
        }
    }
}
