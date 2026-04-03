package net.hwyz.iov.cloud.sec.ciam.common.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordEncoderTest {

    private PasswordEncoder encoder;

    @BeforeEach
    void setUp() {
        encoder = new PasswordEncoder();
    }

    @Test
    void encode_producesNonNullBCryptHash() {
        String hash = encoder.encode("P@ssw0rd!");
        assertNotNull(hash);
        assertTrue(hash.startsWith("$2a$"), "Should be a BCrypt hash");
    }

    @Test
    void encode_neverReturnsPlaintext() {
        String raw = "MySecret123!";
        assertNotEquals(raw, encoder.encode(raw));
    }

    @Test
    void matches_correctPassword_returnsTrue() {
        String raw = "Str0ng!Pass";
        String hash = encoder.encode(raw);
        assertTrue(encoder.matches(raw, hash));
    }

    @Test
    void matches_wrongPassword_returnsFalse() {
        String hash = encoder.encode("CorrectPassword1!");
        assertFalse(encoder.matches("WrongPassword1!", hash));
    }

    @Test
    void encode_samePassword_producesDifferentHashes() {
        String raw = "SamePass1!";
        String h1 = encoder.encode(raw);
        String h2 = encoder.encode(raw);
        assertNotEquals(h1, h2, "BCrypt should use random salt each time");
        assertTrue(encoder.matches(raw, h1));
        assertTrue(encoder.matches(raw, h2));
    }

    @Test
    void encode_nullPassword_throws() {
        assertThrows(NullPointerException.class, () -> encoder.encode(null));
    }

    @Test
    void matches_nullRawPassword_throws() {
        assertThrows(NullPointerException.class, () -> encoder.matches(null, "$2a$10$abc"));
    }

    @Test
    void algorithmConstant_isBCRYPT() {
        assertEquals("BCRYPT", PasswordEncoder.ALGORITHM);
    }
}
