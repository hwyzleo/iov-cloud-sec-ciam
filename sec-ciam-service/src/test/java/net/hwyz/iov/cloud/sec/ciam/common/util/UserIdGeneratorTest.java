package net.hwyz.iov.cloud.sec.ciam.common.util;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class UserIdGeneratorTest {

    @Test
    void generate_returnsNonNullNonBlank() {
        String id = UserIdGenerator.generate();
        assertNotNull(id);
        assertFalse(id.isBlank());
    }

    @Test
    void generate_returns32CharHexString() {
        String id = UserIdGenerator.generate();
        assertEquals(32, id.length(), "UUID without dashes should be 32 chars");
        assertTrue(id.matches("[0-9a-f]{32}"), "Should be lowercase hex");
    }

    @Test
    void generate_containsNoHyphens() {
        String id = UserIdGenerator.generate();
        assertFalse(id.contains("-"), "Should not contain hyphens");
    }

    @Test
    void generate_producesUniqueIds() {
        Set<String> ids = new HashSet<>();
        int count = 1000;
        for (int i = 0; i < count; i++) {
            ids.add(UserIdGenerator.generate());
        }
        assertEquals(count, ids.size(), "All generated IDs should be unique");
    }

    @Test
    void generate_twoConsecutiveCalls_returnDifferentValues() {
        String id1 = UserIdGenerator.generate();
        String id2 = UserIdGenerator.generate();
        assertNotEquals(id1, id2);
    }
}
