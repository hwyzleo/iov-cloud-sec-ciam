package net.hwyz.iov.cloud.sec.ciam.common.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SensitiveFieldStrategyTest {

    @Test
    void strategyConstants_areDefined() {
        assertEquals("ENCRYPT_AND_HASH", SensitiveFieldStrategy.STRATEGY_ENCRYPT_AND_HASH);
        assertEquals("BCRYPT", SensitiveFieldStrategy.STRATEGY_BCRYPT);
        assertEquals("SHA256_FINGERPRINT", SensitiveFieldStrategy.STRATEGY_SHA256_FINGERPRINT);
        assertEquals("PLAINTEXT", SensitiveFieldStrategy.STRATEGY_PLAINTEXT);
    }

    @Test
    void formatConstants_areDefined() {
        assertEquals("JSON", SensitiveFieldStrategy.FORMAT_JSON);
        assertEquals("COMMA_SEPARATED", SensitiveFieldStrategy.FORMAT_COMMA_SEPARATED);
        assertEquals("JSON_ARRAY", SensitiveFieldStrategy.FORMAT_JSON_ARRAY);
        assertEquals("SPACE_SEPARATED", SensitiveFieldStrategy.FORMAT_SPACE_SEPARATED);
    }

    @Test
    void algorithmConstants_areDefined() {
        assertEquals("BCRYPT", SensitiveFieldStrategy.HASH_ALG_BCRYPT);
        assertEquals("SHA-256", SensitiveFieldStrategy.HASH_ALG_SHA256);
        assertEquals("AES-256-GCM", SensitiveFieldStrategy.ENCRYPT_ALG_AES256_GCM);
    }
}
