package net.hwyz.iov.cloud.sec.ciam.service.common.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TokenDigestTest {

    @Test
    void fingerprint_producesConsistent64CharHex() {
        String token = "abc123-refresh-token-value";
        String fp1 = TokenDigest.fingerprint(token);
        String fp2 = TokenDigest.fingerprint(token);
        assertEquals(fp1, fp2);
        assertEquals(64, fp1.length());
        assertTrue(fp1.matches("[0-9a-f]{64}"));
    }

    @Test
    void fingerprint_differentInputs_produceDifferentResults() {
        assertNotEquals(
                TokenDigest.fingerprint("auth-code-1"),
                TokenDigest.fingerprint("auth-code-2")
        );
    }

    @Test
    void matches_correctValue_returnsTrue() {
        String raw = "verify-code-123456";
        String stored = TokenDigest.fingerprint(raw);
        assertTrue(TokenDigest.matches(raw, stored));
    }

    @Test
    void matches_wrongValue_returnsFalse() {
        String stored = TokenDigest.fingerprint("original-token");
        assertFalse(TokenDigest.matches("different-token", stored));
    }

    @Test
    void fingerprint_nullInput_throws() {
        assertThrows(NullPointerException.class, () -> TokenDigest.fingerprint(null));
    }

    @Test
    void matches_nullRawValue_throws() {
        assertThrows(NullPointerException.class, () -> TokenDigest.matches(null, "abc"));
    }

    @Test
    void matches_nullStoredHash_throws() {
        assertThrows(NullPointerException.class, () -> TokenDigest.matches("abc", null));
    }

    @Test
    void fingerprint_authCode_scenario() {
        // Simulates storing code_hash for ciam_auth_code table
        String authCode = "SplxlOBeZQQYbYS6WxSbIA";
        String codeHash = TokenDigest.fingerprint(authCode);
        assertEquals(64, codeHash.length());
        assertTrue(TokenDigest.matches(authCode, codeHash));
    }

    @Test
    void fingerprint_refreshToken_scenario() {
        // Simulates storing token_fingerprint for ciam_refresh_token table
        String refreshToken = "dGhpcyBpcyBhIHJlZnJlc2ggdG9rZW4";
        String tokenFingerprint = TokenDigest.fingerprint(refreshToken);
        assertEquals(64, tokenFingerprint.length());
        assertTrue(TokenDigest.matches(refreshToken, tokenFingerprint));
    }
}
