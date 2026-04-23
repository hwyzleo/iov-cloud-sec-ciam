package net.hwyz.iov.cloud.sec.ciam.service.config;

import net.hwyz.iov.cloud.sec.ciam.service.common.security.CallbackSignatureVerifier;
import net.hwyz.iov.cloud.sec.ciam.service.common.security.FieldEncryptor;
import net.hwyz.iov.cloud.sec.ciam.service.common.security.PasswordEncoder;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.config.SecurityConfig;
import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class SecurityConfigTest {

    private final SecurityConfig config = new SecurityConfig();

    private String generateValidKey() {
        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);
        return Base64.getEncoder().encodeToString(key);
    }

    @Test
    void fieldEncryptor_createdWithValidKey_canEncryptDecrypt() {
        FieldEncryptor encryptor = config.fieldEncryptor(generateValidKey());
        assertNotNull(encryptor);
        String plaintext = "+8613800138000";
        String encrypted = encryptor.encrypt(plaintext);
        assertEquals(plaintext, encryptor.decrypt(encrypted));
    }

    @Test
    void ciamPasswordEncoder_createdWithStrength_canEncodeAndMatch() {
        PasswordEncoder encoder = config.ciamPasswordEncoder(4);
        assertNotNull(encoder);
        String raw = "TestP@ss1!";
        String hash = encoder.encode(raw);
        assertTrue(encoder.matches(raw, hash));
    }

    @Test
    void ciamPasswordEncoder_defaultStrength_works() {
        PasswordEncoder encoder = config.ciamPasswordEncoder(10);
        assertNotNull(encoder);
        String hash = encoder.encode("Password1!");
        assertNotNull(hash);
        assertTrue(hash.startsWith("$2a$"));
    }

    @Test
    void callbackSignatureVerifier_createdWithSecret_canVerify() {
        CallbackSignatureVerifier verifier = config.callbackSignatureVerifier("test-secret");
        assertNotNull(verifier);
        String hmac = verifier.computeHmac("test-payload");
        assertNotNull(hmac);
        assertEquals(64, hmac.length());
    }
}
