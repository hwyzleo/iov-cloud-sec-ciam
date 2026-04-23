package net.hwyz.iov.cloud.sec.ciam.service.application;
import net.hwyz.iov.cloud.sec.ciam.service.application.service.*;

import net.hwyz.iov.cloud.sec.ciam.service.application.dto.OwnerCertStateDto;
import net.hwyz.iov.cloud.sec.ciam.service.common.audit.AuditLogger;
import net.hwyz.iov.cloud.sec.ciam.service.domain.enums.CertStatus;
import net.hwyz.iov.cloud.sec.ciam.service.domain.model.OwnerCertState;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.OwnerCertStateRepository;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.UserTagRepository;
import net.hwyz.iov.cloud.sec.ciam.service.domain.service.TagDomainService;
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

    private OwnerCertStateRepository certStateRepository;
    private UserTagRepository tagRepository;
    private AuditLogger auditLogger;
    private OwnerCertificationAppService service;

    private static final String USER_ID = "user-cert-001";
    private static final String VIN = "LSVAU2180N2000001";
    private static final String CERT_SOURCE = "external_cert_service";

    @BeforeEach
    void setUp() {
        certStateRepository = mock(OwnerCertStateRepository.class);
        tagRepository = mock(UserTagRepository.class);
        auditLogger = mock(AuditLogger.class);

        when(certStateRepository.insert(any())).thenReturn(1);
        when(certStateRepository.updateByOwnerCertId(any())).thenReturn(1);
        when(tagRepository.insert(any())).thenReturn(1);
        when(tagRepository.updateByTagId(any())).thenReturn(1);

        TagDomainService tagDomainService = new TagDomainService(tagRepository);
        service = new OwnerCertificationAppService(
                certStateRepository, tagDomainService, auditLogger);
    }

    private OwnerCertState stubCertRecord(int certStatus) {
        return OwnerCertState.builder()
                .ownerCertId("cert-001")
                .userId(USER_ID)
                .vin(VIN)
                .certStatus(certStatus)
                .certSource(CERT_SOURCE)
                .build();
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

            ArgumentCaptor<OwnerCertState> captor =
                    ArgumentCaptor.forClass(OwnerCertState.class);
            verify(certStateRepository).insert(captor.capture());
            OwnerCertState created = captor.getValue();
            assertEquals(USER_ID, created.getUserId());
            assertEquals(CertStatus.CERTIFIED.getCode(), created.getCertStatus());
        }
    }

    // ---- queryCertificationStatus ----

    @Nested
    class QueryCertificationStatusTests {

        @Test
        void returnsAllRecordsForUser() {
            OwnerCertState record = stubCertRecord(CertStatus.CERTIFIED.getCode());
            when(certStateRepository.findByUserId(USER_ID)).thenReturn(List.of(record));

            List<OwnerCertStateDto> result = service.queryCertificationStatus(USER_ID);

            assertEquals(1, result.size());
            assertEquals(CertStatus.CERTIFIED.getCode(), result.get(0).getCertStatus());
        }

        @Test
        void returnsEmptyListWhenNoRecords() {
            when(certStateRepository.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            List<OwnerCertStateDto> result = service.queryCertificationStatus(USER_ID);

            assertTrue(result.isEmpty());
        }
    }

    // ---- compensateCertificationStatus ----

    @Nested
    class CompensateCertificationStatusTests {

        @Test
        void updatesLastQueryTimeForPendingRecords() {
            OwnerCertState pending = stubCertRecord(CertStatus.CERTIFYING.getCode());
            when(certStateRepository.findByUserIdAndCertStatus(USER_ID,
                    CertStatus.CERTIFYING.getCode())).thenReturn(List.of(pending));

            List<OwnerCertStateDto> result =
                    service.compensateCertificationStatus(USER_ID);

            assertEquals(1, result.size());
            assertNotNull(pending.getLastQueryTime());
            verify(certStateRepository).updateByOwnerCertId(pending);
        }

        @Test
        void returnsEmptyWhenNoPendingRecords() {
            when(certStateRepository.findByUserIdAndCertStatus(USER_ID,
                    CertStatus.CERTIFYING.getCode())).thenReturn(Collections.emptyList());

            List<OwnerCertStateDto> result =
                    service.compensateCertificationStatus(USER_ID);

            assertTrue(result.isEmpty());
        }
    }
}
