package net.hwyz.iov.cloud.sec.ciam.common.security;

import net.hwyz.iov.cloud.sec.ciam.common.security.CallbackSignatureVerifier.VerifyResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CallbackSignatureVerifierTest {

    private static final String SECRET = "test-callback-secret-key-2024";
    private CallbackSignatureVerifier verifier;

    @BeforeEach
    void setUp() {
        verifier = new CallbackSignatureVerifier(SECRET);
        verifier.clearNonces();
    }

    @Test
    void validSignature_returnsOk() {
        long timestamp = System.currentTimeMillis();
        String nonce = "nonce-001";
        String body = "{\"userId\":\"user-001\",\"status\":\"verified\"}";
        String payload = timestamp + "." + nonce + "." + body;
        String signature = verifier.computeHmac(payload);

        VerifyResult result = verifier.verify(body, timestamp, nonce, signature);

        assertEquals(VerifyResult.OK, result);
    }

    @Test
    void expiredTimestamp_returnsTimestampExpired() {
        long oldTimestamp = System.currentTimeMillis() - 6 * 60 * 1000L; // 6 minutes ago
        String nonce = "nonce-002";
        String body = "test-body";
        String payload = oldTimestamp + "." + nonce + "." + body;
        String signature = verifier.computeHmac(payload);

        VerifyResult result = verifier.verify(body, oldTimestamp, nonce, signature);

        assertEquals(VerifyResult.TIMESTAMP_EXPIRED, result);
    }

    @Test
    void futureTimestamp_beyondWindow_returnsTimestampExpired() {
        long futureTimestamp = System.currentTimeMillis() + 6 * 60 * 1000L; // 6 minutes in future
        String nonce = "nonce-003";
        String body = "test-body";
        String payload = futureTimestamp + "." + nonce + "." + body;
        String signature = verifier.computeHmac(payload);

        VerifyResult result = verifier.verify(body, futureTimestamp, nonce, signature);

        assertEquals(VerifyResult.TIMESTAMP_EXPIRED, result);
    }

    @Test
    void missingNonce_returnsNonceMissing() {
        long timestamp = System.currentTimeMillis();
        String body = "test-body";

        VerifyResult result = verifier.verify(body, timestamp, null, "any-sig");

        assertEquals(VerifyResult.NONCE_MISSING, result);
    }

    @Test
    void blankNonce_returnsNonceMissing() {
        long timestamp = System.currentTimeMillis();
        String body = "test-body";

        VerifyResult result = verifier.verify(body, timestamp, "  ", "any-sig");

        assertEquals(VerifyResult.NONCE_MISSING, result);
    }

    @Test
    void duplicateNonce_returnsNonceDuplicate() {
        long timestamp = System.currentTimeMillis();
        String nonce = "nonce-004";
        String body = "test-body";
        String payload = timestamp + "." + nonce + "." + body;
        String signature = verifier.computeHmac(payload);

        // First call succeeds
        assertEquals(VerifyResult.OK, verifier.verify(body, timestamp, nonce, signature));

        // Second call with same nonce fails
        assertEquals(VerifyResult.NONCE_DUPLICATE, verifier.verify(body, timestamp, nonce, signature));
    }

    @Test
    void invalidSignature_returnsSignatureInvalid() {
        long timestamp = System.currentTimeMillis();
        String nonce = "nonce-005";
        String body = "test-body";

        VerifyResult result = verifier.verify(body, timestamp, nonce, "invalid-signature");

        assertEquals(VerifyResult.SIGNATURE_INVALID, result);
    }

    @Test
    void tamperedBody_returnsSignatureInvalid() {
        long timestamp = System.currentTimeMillis();
        String nonce = "nonce-006";
        String originalBody = "original-body";
        String payload = timestamp + "." + nonce + "." + originalBody;
        String signature = verifier.computeHmac(payload);

        // Verify with tampered body
        VerifyResult result = verifier.verify("tampered-body", timestamp, nonce, signature);

        assertEquals(VerifyResult.SIGNATURE_INVALID, result);
    }

    @Test
    void differentSecrets_produceDifferentSignatures() {
        CallbackSignatureVerifier other = new CallbackSignatureVerifier("different-secret");
        String payload = "test-payload";

        assertNotEquals(verifier.computeHmac(payload), other.computeHmac(payload));
    }

    @Test
    void computeHmac_producesConsistentResults() {
        String payload = "consistent-test";
        String sig1 = verifier.computeHmac(payload);
        String sig2 = verifier.computeHmac(payload);

        assertEquals(sig1, sig2);
        assertEquals(64, sig1.length(), "HMAC-SHA256 hex should be 64 chars");
    }
}
