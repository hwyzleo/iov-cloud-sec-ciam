package net.hwyz.iov.cloud.sec.ciam.service.common.security;
import net.hwyz.iov.cloud.sec.ciam.service.application.service.*;
import net.hwyz.iov.cloud.sec.ciam.service.domain.adapter.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class FieldEncryptorTest {

    private FieldEncryptor encryptor;

    @BeforeEach
    void setUp() {
        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);
        String keyBase64 = Base64.getEncoder().encodeToString(key);
        encryptor = new FieldEncryptor(keyBase64);
    }

    @Test
    void encryptDecrypt_phone_roundTrip() {
        String phone = "+8613812341234";
        String encrypted = encryptor.encrypt(phone);
        assertNotEquals(phone, encrypted);
        assertEquals(phone, encryptor.decrypt(encrypted));
    }

    @Test
    void encryptDecrypt_email_roundTrip() {
        String email = "user@example.com";
        String encrypted = encryptor.encrypt(email);
        assertEquals(email, encryptor.decrypt(encrypted));
    }

    @Test
    void encryptDecrypt_thirdPartySubject_roundTrip() {
        String subject = "apple_sub_abc123xyz";
        assertEquals(subject, encryptor.decrypt(encryptor.encrypt(subject)));
    }

    @Test
    void encrypt_sameInput_producesDifferentCiphertext() {
        String value = "13812341234";
        String c1 = encryptor.encrypt(value);
        String c2 = encryptor.encrypt(value);
        assertNotEquals(c1, c2, "Each encryption should use a unique IV");
    }

    @Test
    void hash_producesConsistent64CharHex() {
        String value = "13812341234";
        String h1 = FieldEncryptor.hash(value);
        String h2 = FieldEncryptor.hash(value);
        assertEquals(h1, h2);
        assertEquals(64, h1.length());
        assertTrue(h1.matches("[0-9a-f]{64}"));
    }

    @Test
    void hash_differentInputs_produceDifferentHashes() {
        assertNotEquals(FieldEncryptor.hash("a@b.com"), FieldEncryptor.hash("c@d.com"));
    }

    @Test
    void constructor_invalidKeyLength_throws() {
        String shortKey = Base64.getEncoder().encodeToString(new byte[16]);
        assertThrows(IllegalArgumentException.class, () -> new FieldEncryptor(shortKey));
    }

    @Test
    void encrypt_nullInput_throws() {
        assertThrows(NullPointerException.class, () -> encryptor.encrypt(null));
    }

    @Test
    void decrypt_nullInput_throws() {
        assertThrows(NullPointerException.class, () -> encryptor.decrypt(null));
    }

    @Test
    void hash_nullInput_throws() {
        assertThrows(NullPointerException.class, () -> FieldEncryptor.hash(null));
    }

    @Test
    void decrypt_tamperedCiphertext_throws() {
        String encrypted = encryptor.encrypt("test");
        byte[] decoded = Base64.getDecoder().decode(encrypted);
        decoded[decoded.length - 1] ^= 0xFF; // tamper
        String tampered = Base64.getEncoder().encodeToString(decoded);
        assertThrows(SecurityException.class, () -> encryptor.decrypt(tampered));
    }
}
