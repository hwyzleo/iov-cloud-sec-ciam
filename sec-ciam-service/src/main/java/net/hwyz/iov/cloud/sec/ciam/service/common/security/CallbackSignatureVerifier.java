package net.hwyz.iov.cloud.sec.ciam.service.common.security;

import lombok.extern.slf4j.Slf4j;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 第三方回调签名校验工具。
 * <p>
 * 提供 HMAC-SHA256 签名校验、时间戳重放防护（5 分钟窗口）和 Nonce 去重检测。
 * 生产环境 Nonce 存储应替换为 Redis。
 */
@Slf4j
public class CallbackSignatureVerifier {

    static final long MAX_TIMESTAMP_DRIFT_MILLIS = 5 * 60 * 1000L;
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final byte[] secretKey;
    private final Set<String> usedNonces = ConcurrentHashMap.newKeySet();

    public CallbackSignatureVerifier(String secretKey) {
        this.secretKey = secretKey.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * 校验回调请求的签名、时间戳和 Nonce。
     *
     * @param body      请求体
     * @param timestamp 请求时间戳（毫秒）
     * @param nonce     一次性随机数
     * @param signature 请求签名（hex 编码）
     * @return 校验结果
     */
    public VerifyResult verify(String body, long timestamp, String nonce, String signature) {
        // 1. 时间戳校验
        long now = System.currentTimeMillis();
        if (Math.abs(now - timestamp) > MAX_TIMESTAMP_DRIFT_MILLIS) {
            log.warn("回调签名校验失败: 时间戳过期, drift={}ms", Math.abs(now - timestamp));
            return VerifyResult.TIMESTAMP_EXPIRED;
        }

        // 2. Nonce 去重
        if (nonce == null || nonce.isBlank()) {
            return VerifyResult.NONCE_MISSING;
        }
        if (!usedNonces.add(nonce)) {
            log.warn("回调签名校验失败: Nonce 重复, nonce={}", nonce);
            return VerifyResult.NONCE_DUPLICATE;
        }

        // 3. 签名校验
        String payload = timestamp + "." + nonce + "." + body;
        String expected = computeHmac(payload);
        if (!MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                signature.getBytes(StandardCharsets.UTF_8))) {
            log.warn("回调签名校验失败: 签名不匹配");
            return VerifyResult.SIGNATURE_INVALID;
        }

        return VerifyResult.OK;
    }

    /**
     * 为给定载荷生成 HMAC-SHA256 签名。
     */
    public String computeHmac(String payload) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secretKey, HMAC_ALGORITHM));
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("HMAC 计算失败", e);
        }
    }

    // 仅用于测试
    void clearNonces() {
        usedNonces.clear();
    }

    public enum VerifyResult {
        OK,
        TIMESTAMP_EXPIRED,
        NONCE_MISSING,
        NONCE_DUPLICATE,
        SIGNATURE_INVALID
    }
}
