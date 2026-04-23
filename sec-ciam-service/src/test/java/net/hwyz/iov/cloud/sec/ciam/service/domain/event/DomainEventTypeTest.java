package net.hwyz.iov.cloud.sec.ciam.service.domain.event;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DomainEventTypeTest {

    @Test
    void allEventTypes_haveNonBlankDescription() {
        for (DomainEventType type : DomainEventType.values()) {
            assertNotNull(type.getDescription(), type.name() + " description should not be null");
            assertFalse(type.getDescription().isBlank(), type.name() + " description should not be blank");
        }
    }

    @Test
    void enumValues_containExpectedCount() {
        assertEquals(10, DomainEventType.values().length);
    }

    @Test
    void valueOf_resolvesKnownTypes() {
        assertEquals(DomainEventType.USER_REGISTERED, DomainEventType.valueOf("USER_REGISTERED"));
        assertEquals(DomainEventType.LOGIN_SUCCESS, DomainEventType.valueOf("LOGIN_SUCCESS"));
        assertEquals(DomainEventType.DEACTIVATION_COMPLETED, DomainEventType.valueOf("DEACTIVATION_COMPLETED"));
        assertEquals(DomainEventType.OWNER_CERT_CHANGED, DomainEventType.valueOf("OWNER_CERT_CHANGED"));
    }
}
