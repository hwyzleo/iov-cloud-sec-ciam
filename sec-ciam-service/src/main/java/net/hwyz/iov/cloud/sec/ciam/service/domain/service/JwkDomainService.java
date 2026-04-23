package net.hwyz.iov.cloud.sec.ciam.service.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.util.DateTimeUtil;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.JwkRepository;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.po.JwkPo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * JWK 密钥领域服务 — 密钥生成、加载、轮换。
 * <p>
 * 职责：
 * <ul>
 *   <li>应用启动时从数据库加载主密钥，如无则生成新密钥</li>
 *   <li>提供主密钥对用于签名 JWT</li>
 *   <li>提供所有激活密钥的公钥用于验证 JWT 和 JWKS 端点</li>
 *   <li>支持密钥轮换（手动或定时）</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JwkDomainService {

    private static final String ALGORITHM_RSA = "RSA";
    private static final int DEFAULT_KEY_SIZE = 2048;
    private static final int KEY_STATUS_ACTIVE = 1;
    private static final int KEY_STATUS_DISABLED = 2;
    private static final int KEY_STATUS_EXPIRED = 3;

    private final JwkRepository jwkRepository;

    @Value("${ciam.jwt.key-expire-days:365}")
    private int keyExpireDays;

    private RSAPublicKey cachedPublicKey;
    private RSAPrivateKey cachedPrivateKey;
    private String cachedKeyId;

    /**
     * 应用启动时初始化密钥。
     */
    public void initialize() {
        jwkRepository.findPrimary()
                .ifPresentOrElse(
                        this::loadKeyPair,
                        () -> generateAndStoreNewKey()
                );
    }

    /**
     * 获取主私钥（用于签名新 token）。
     */
    public RSAPrivateKey getPrimaryPrivateKey() {
        if (cachedPrivateKey == null) {
            log.warn("密钥缓存为空，重新从数据库加载");
            initialize();
        }
        return cachedPrivateKey;
    }

    /**
     * 获取主公钥（用于验证 token）。
     */
    public RSAPublicKey getPrimaryPublicKey() {
        if (cachedPublicKey == null) {
            log.warn("密钥缓存为空，重新从数据库加载");
            initialize();
        }
        return cachedPublicKey;
    }

    /**
     * 获取主密钥对（用于签名新 token）。
     */
    public KeyPair getPrimaryKeypair() {
        if (cachedPrivateKey == null || cachedPublicKey == null) {
            log.warn("密钥缓存为空，重新从数据库加载");
            initialize();
        }
        return new KeyPair(cachedPublicKey, cachedPrivateKey);
    }

    /**
     * 获取密钥 ID（kid）。
     */
    public String getKeyId() {
        return cachedKeyId;
    }

    /**
     * 获取所有激活状态的公钥（用于 JWKS 端点）。
     */
    public List<JwkPo> getAllActiveKeys() {
        return jwkRepository.findAllActive();
    }

    /**
     * 根据密钥 ID 获取公钥（用于验证特定 token）。
     */
    public RSAPublicKey getPublicKeyByKeyId(String keyId) {
        return jwkRepository.findByKeyId(keyId)
                .map(this::parsePublicKey)
                .orElseThrow(() -> new NoSuchElementException("密钥不存在：keyId=" + keyId));
    }

    /**
     * 生成新密钥并存储。
     */
    public JwkPo generateAndStoreNewKey() {
        return generateAndStoreNewKey(DEFAULT_KEY_SIZE, true);
    }

    /**
     * 生成新密钥并存储，支持轮换场景。
     *
     * @param keySize     密钥长度
     * @param setPrimary  是否设为主密钥
     * @return 存储的密钥记录
     */
    public JwkPo generateAndStoreNewKey(int keySize, boolean setPrimary) {
        KeyPair keyPair = generateRsaKeyPair(keySize);
        String keyId = UUID.randomUUID().toString().replace("-", "");
        Instant now = DateTimeUtil.getNowInstant();
        Instant expireTime = keyExpireDays > 0
                ? now.plusSeconds(keyExpireDays * 24L * 60 * 60)
                : null;

        if (setPrimary) {
            jwkRepository.revokePrimary();
        }

        JwkPo entity = new JwkPo();
        entity.setKeyId(keyId);
        entity.setPrivateKeyPem(encodePrivateKey((RSAPrivateKey) keyPair.getPrivate()));
        entity.setPublicKeyPem(encodePublicKey((RSAPublicKey) keyPair.getPublic()));
        entity.setAlgorithm(ALGORITHM_RSA);
        entity.setKeySize(keySize);
        entity.setStatus(KEY_STATUS_ACTIVE);
        entity.setIssueTime(now);
        entity.setExpireTime(expireTime);
        entity.setIsPrimary(setPrimary ? 1 : 0);
        entity.setCreateTime(now);
        entity.setModifyTime(now);
        entity.setRowVersion(1);

        jwkRepository.insert(entity);

        if (setPrimary) {
            cachedPublicKey = (RSAPublicKey) keyPair.getPublic();
            cachedPrivateKey = (RSAPrivateKey) keyPair.getPrivate();
            cachedKeyId = keyId;
        }

        log.info("JWK 密钥生成并存储：keyId={}, keySize={}, isPrimary={}, expireTime={}",
                keyId, keySize, setPrimary, expireTime);

        return entity;
    }

    /**
     * 密钥轮换 — 生成新主密钥，旧密钥保持激活状态用于验证旧 token。
     *
     * @return 新生成的密钥记录
     */
    public JwkPo rotateKey() {
        log.info("开始 JWK 密钥轮换");
        return generateAndStoreNewKey(DEFAULT_KEY_SIZE, true);
    }

    /**
     * 撤销指定密钥。
     *
     * @param keyId 密钥 ID
     */
    public void revokeKey(String keyId) {
        JwkPo entity = jwkRepository.findByKeyId(keyId)
                .orElseThrow(() -> new NoSuchElementException("密钥不存在：keyId=" + keyId));

        if (entity.getIsPrimary() == 1) {
            log.warn("撤销主密钥，将触发新密钥生成：keyId={}", keyId);
            generateAndStoreNewKey(DEFAULT_KEY_SIZE, true);
        }

        entity.setStatus(KEY_STATUS_DISABLED);
        entity.setModifyTime(DateTimeUtil.getNowInstant());
        jwkRepository.update(entity);

        log.info("JWK 密钥已撤销：keyId={}", keyId);
    }

    // ---- 内部方法 ----

    private KeyPair loadKeyPair(JwkPo entity) {
        try {
            java.security.KeyFactory keyFactory = java.security.KeyFactory.getInstance(ALGORITHM_RSA);

            byte[] privateKeyBytes = Base64.getMimeDecoder().decode(entity.getPrivateKeyPem());
            java.security.spec.PKCS8EncodedKeySpec privateKeySpec =
                    new java.security.spec.PKCS8EncodedKeySpec(privateKeyBytes);
            RSAPrivateKey privateKey = (RSAPrivateKey) keyFactory.generatePrivate(privateKeySpec);

            byte[] publicKeyBytes = Base64.getMimeDecoder().decode(entity.getPublicKeyPem());
            java.security.spec.X509EncodedKeySpec publicKeySpec =
                    new java.security.spec.X509EncodedKeySpec(publicKeyBytes);
            RSAPublicKey publicKey = (RSAPublicKey) keyFactory.generatePublic(publicKeySpec);

            cachedPublicKey = publicKey;
            cachedPrivateKey = privateKey;
            cachedKeyId = entity.getKeyId();

            log.info("JWK 密钥加载成功：keyId={}, algorithm={}, keySize={}",
                    entity.getKeyId(), entity.getAlgorithm(), entity.getKeySize());

            return new KeyPair(publicKey, privateKey);
        } catch (Exception e) {
            throw new IllegalStateException("JWK 密钥加载失败：keyId=" + entity.getKeyId(), e);
        }
    }

    private KeyPair generateRsaKeyPair(int keySize) {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance(ALGORITHM_RSA);
            generator.initialize(keySize);
            return generator.generateKeyPair();
        } catch (Exception e) {
            throw new IllegalStateException("RSA 密钥对生成失败", e);
        }
    }

    private String encodePrivateKey(RSAPrivateKey privateKey) {
        return Base64.getMimeEncoder(64, System.lineSeparator().getBytes())
                .encodeToString(privateKey.getEncoded());
    }

    private String encodePublicKey(RSAPublicKey publicKey) {
        return Base64.getMimeEncoder(64, System.lineSeparator().getBytes())
                .encodeToString(publicKey.getEncoded());
    }

    private RSAPublicKey parsePublicKey(JwkPo entity) {
        try {
            java.security.KeyFactory keyFactory = java.security.KeyFactory.getInstance(ALGORITHM_RSA);
            byte[] publicKeyBytes = Base64.getMimeDecoder().decode(entity.getPublicKeyPem());
            java.security.spec.X509EncodedKeySpec publicKeySpec =
                    new java.security.spec.X509EncodedKeySpec(publicKeyBytes);
            return (RSAPublicKey) keyFactory.generatePublic(publicKeySpec);
        } catch (Exception e) {
            throw new IllegalStateException("公钥解析失败：keyId=" + entity.getKeyId(), e);
        }
    }
}
