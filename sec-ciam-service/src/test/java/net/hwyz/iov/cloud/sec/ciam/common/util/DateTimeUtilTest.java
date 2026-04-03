package net.hwyz.iov.cloud.sec.ciam.common.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class DateTimeUtilTest {

    @Test
    void now_returnsNonNull() {
        assertNotNull(DateTimeUtil.now());
    }

    @Test
    void format_validDateTime_returnsExpectedString() {
        LocalDateTime dt = LocalDateTime.of(2024, 1, 15, 10, 30, 0);
        assertEquals("2024-01-15 10:30:00", DateTimeUtil.format(dt));
    }

    @Test
    void format_null_returnsNull() {
        assertNull(DateTimeUtil.format(null));
    }

    @Test
    void parse_validString_returnsDateTime() {
        LocalDateTime dt = DateTimeUtil.parse("2024-01-15 10:30:00");
        assertEquals(LocalDateTime.of(2024, 1, 15, 10, 30, 0), dt);
    }

    @Test
    void parse_null_returnsNull() {
        assertNull(DateTimeUtil.parse(null));
    }

    @Test
    void epochMilliRoundTrip_preservesValue() {
        LocalDateTime original = LocalDateTime.of(2024, 6, 1, 12, 0, 0);
        long millis = DateTimeUtil.toEpochMilli(original);
        LocalDateTime restored = DateTimeUtil.fromEpochMilli(millis);
        assertEquals(original, restored);
    }
}
