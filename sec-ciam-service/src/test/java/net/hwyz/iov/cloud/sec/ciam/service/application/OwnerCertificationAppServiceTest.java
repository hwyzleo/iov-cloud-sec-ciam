package net.hwyz.iov.cloud.sec.ciam.service.application;
import net.hwyz.iov.cloud.sec.ciam.service.application.service.*;
import net.hwyz.iov.cloud.sec.ciam.service.domain.adapter.*;

import net.hwyz.iov.cloud.framework.common.exception.BusinessException;
import net.hwyz.iov.cloud.sec.ciam.service.application.dto.OwnerCertificationDto;
import net.hwyz.iov.cloud.sec.ciam.service.common.audit.AuditEvent;
import net.hwyz.iov.cloud.sec.ciam.service.common.audit.AuditLogger;
import net.hwyz.iov.cloud.sec.ciam.service.common.exception.CiamErrorCode;
import net.hwyz.iov.cloud.sec.ciam.service.domain.enums.CertStatus;
import net.hwyz.iov.cloud.sec.ciam.service.domain.enums.TagStatus;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.CiamOwnerCertStateRepository;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.CiamUserTagRepository;
import net.hwyz.iov.cloud.sec.ciam.service.domain.service.TagDomainService;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.po.OwnerCertStatePo;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.po.UserTagPo;
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

    private OwnerCertStatePo stubCertRecord(int certStatus) {
        OwnerCertStatePo record = new OwnerCertStatePo();
        record.setOwnerCertId("cert-001");
        record.setUserId(USER_ID);
        record.setVin(VIN);
        record.setCertStatus(certStatus);
        record.setCertSource(CERT_SOURCE);
        record.setRowVersion(1);
        record.setRowValid(1);
        return record;
    }

    private UserTagPo stubTag(int status) {
        UserTagPo tag = new UserTagPo();
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

            ArgumentCaptor<OwnerCertStatePo> captor =
                    ArgumentCaptor.forClass(OwnerCertStatePo.class);
            verify(certStateRepository).insert(captor.capture());
            OwnerCertStatePo created = captor.getValue();
            assertEquals(USER_ID, created.getUserId());
            assertEquals(CertStatus.CERTIFIED.getCode(), created.getCertStatus());
        }
    }

    // ---- queryCertificationStatus ----

    @Nested
    class QueryCertificationStatusTests {

        @Test
        void returnsAllRecordsForUser() {
            OwnerCertStatePo record = stubCertRecord(CertStatus.CERTIFIED.getCode());
            when(certStateRepository.findByUserId(USER_ID)).thenReturn(List.of(record));

            List<OwnerCertificationDto> result = service.queryCertificationStatus(USER_ID);

            assertEquals(1, result.size());
            assertEquals(CertStatus.CERTIFIED.getCode(), result.get(0).getCertStatus());
        }

        @Test
        void returnsEmptyListWhenNoRecords() {
            when(certStateRepository.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            List<OwnerCertificationDto> result = service.queryCertificationStatus(USER_ID);

            assertTrue(result.isEmpty());
        }
    }

    // ---- compensateCertificationStatus ----

    @Nested
    class CompensateCertificationStatusTests {

        @Test
        void updatesLastQueryTimeForPendingRecords() {
            OwnerCertStatePo pending = stubCertRecord(CertStatus.CERTIFYING.getCode());
            when(certStateRepository.findByUserIdAndCertStatus(USER_ID,
                    CertStatus.CERTIFYING.getCode())).thenReturn(List.of(pending));

            List<OwnerCertificationDto> result =
                    service.compensateCertificationStatus(USER_ID);

            assertEquals(1, result.size());
        }

        @Test
        void returnsEmptyWhenNoPendingRecords() {
            when(certStateRepository.findByUserIdAndCertStatus(USER_ID,
                    CertStatus.CERTIFYING.getCode())).thenReturn(Collections.emptyList());

            List<OwnerCertificationDto> result =
                    service.compensateCertificationStatus(USER_ID);

            assertTrue(result.isEmpty());
        }
    }
}
