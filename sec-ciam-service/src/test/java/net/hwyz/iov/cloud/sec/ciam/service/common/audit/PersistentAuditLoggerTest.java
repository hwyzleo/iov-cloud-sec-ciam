package net.hwyz.iov.cloud.sec.ciam.service.common.audit;

import net.hwyz.iov.cloud.sec.ciam.service.domain.model.AuditLog;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.CiamAuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PersistentAuditLoggerTest {

    private CiamAuditLogRepository auditLogRepository;
    private Slf4jAuditLogger delegateLogger;
    private PersistentAuditLogger persistentAuditLogger;

    @BeforeEach
    void setUp() {
        auditLogRepository = mock(CiamAuditLogRepository.class);
        delegateLogger = mock(Slf4jAuditLogger.class);
        persistentAuditLogger = new PersistentAuditLogger(auditLogRepository, delegateLogger);
    }

    @Test
    void log_persistsToRepository() {
        AuditEvent event = AuditEvent.builder()
                .userId("u1")
                .eventType("LOGIN")
                .success(true)
                .build();

        persistentAuditLogger.log(event);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).insert(captor.capture());
        assertEquals("u1", captor.getValue().getUserId());
    }
}
