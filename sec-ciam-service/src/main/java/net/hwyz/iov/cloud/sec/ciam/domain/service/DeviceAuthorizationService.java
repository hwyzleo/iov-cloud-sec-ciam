package net.hwyz.iov.cloud.sec.ciam.domain.service;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.framework.common.exception.BusinessException;
import net.hwyz.iov.cloud.sec.ciam.common.exception.CiamErrorCode;
import net.hwyz.iov.cloud.sec.ciam.domain.repository.CiamOAuthClientRepository;
import net.hwyz.iov.cloud.sec.ciam.infrastructure.repository.dao.dataobject.CiamOAuthClientDo;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * 设备授权领域服务 — 封装 Device Authorization Grant 授权流程。
 * <p>
 * 面向车机等输入受限终端，支持设备码申请、用户确认、授权轮询。
 * 设备授权状态通过 VerificationCodeStore（Redis / 内存）管理，格式为
 * {@code status|clientId|scope|userId}。
 */
@Service
@RequiredArgsConstructor
public class DeviceAuthorizationService {

    /** 设备授权码默认有效期：10 分钟 */
    static final int DEVICE_CODE_TTL_SECONDS = 600;

    /** 轮询间隔：5 秒 */
    static final int POLLING_INTERVAL_SECONDS = 5;

    /** 验证地址 */
    static final String VERIFICATION_URI = "https://account.openiov.com/device";

    /** 用户码长度 */
    static final int USER_CODE_LENGTH = 8;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int DEVICE_CODE_BYTE_LENGTH = 32;

    /** 存储键前缀 */
    static final String DEVICE_CODE_PREFIX = "device_auth:dc:";
    static final String USER_CODE_PREFIX = "device_auth:uc:";

    /** 状态常量 */
    static final String STATUS_PENDING = "pending";
    static final String STATUS_APPROVED = "approved";
    static final String STATUS_DENIED = "denied";

    private final CiamOAuthClientRepository clientRepository;
    private final VerificationCodeStore store;

    /**
     * 发起设备授权。
     *
     * @param clientId 客户端标识
     * @param scope    授权范围
     * @return 设备授权响应
     */
    public DeviceAuthorizationResponse initiateDeviceAuthorization(String clientId, String scope) {
        // 校验客户端存在且启用
        findEnabledClient(clientId);

        String deviceCode = generateDeviceCode();
        String userCode = generateUserCode();

        // 状态格式: status|clientId|scope|userId
        String state = STATUS_PENDING + "|" + clientId + "|" + (scope == null ? "" : scope) + "|";

        // 以 device_code 和 user_code 为键分别存储，便于双向查找
        store.saveCode(DEVICE_CODE_PREFIX + deviceCode, state, DEVICE_CODE_TTL_SECONDS);
        store.saveCode(USER_CODE_PREFIX + userCode, deviceCode, DEVICE_CODE_TTL_SECONDS);

        return new DeviceAuthorizationResponse(
                deviceCode, userCode, VERIFICATION_URI, DEVICE_CODE_TTL_SECONDS, POLLING_INTERVAL_SECONDS);
    }

    /**
     * 用户确认设备授权。
     *
     * @param userCode 用户码
     * @param userId   用户业务唯一标识
     */
    public void approveDeviceAuthorization(String userCode, String userId) {
        // 通过 user_code 查找 device_code
        String deviceCode = store.getCode(USER_CODE_PREFIX + userCode)
                .orElseThrow(() -> new BusinessException(CiamErrorCode.DEVICE_USER_CODE_NOT_FOUND));

        // 获取当前状态
        String state = store.getCode(DEVICE_CODE_PREFIX + deviceCode)
                .orElseThrow(() -> new BusinessException(CiamErrorCode.DEVICE_CODE_EXPIRED));

        String[] parts = parseState(state);
        // 更新状态为 approved 并设置 userId
        String newState = STATUS_APPROVED + "|" + parts[1] + "|" + parts[2] + "|" + userId;
        store.saveCode(DEVICE_CODE_PREFIX + deviceCode, newState, DEVICE_CODE_TTL_SECONDS);
    }

    /**
     * 轮询设备授权状态。
     *
     * @param deviceCode 设备码
     * @param clientId   客户端标识
     * @return 授权结果（仅在 approved 时返回）
     */
    public DeviceAuthorizationResult pollDeviceAuthorization(String deviceCode, String clientId) {
        String state = store.getCode(DEVICE_CODE_PREFIX + deviceCode)
                .orElseThrow(() -> new BusinessException(CiamErrorCode.DEVICE_CODE_EXPIRED));

        String[] parts = parseState(state);
        String status = parts[0];

        // 校验 clientId 匹配
        if (!clientId.equals(parts[1])) {
            throw new BusinessException(CiamErrorCode.AUTH_CODE_CLIENT_MISMATCH);
        }

        return switch (status) {
            case STATUS_PENDING -> throw new BusinessException(CiamErrorCode.DEVICE_CODE_PENDING);
            case STATUS_APPROVED -> {
                // 授权成功，清理状态
                store.deleteCode(DEVICE_CODE_PREFIX + deviceCode);
                yield new DeviceAuthorizationResult(parts[3], parts[2], parts[1]);
            }
            case STATUS_DENIED -> throw new BusinessException(CiamErrorCode.DEVICE_CODE_DENIED);
            default -> throw new BusinessException(CiamErrorCode.DEVICE_CODE_NOT_FOUND);
        };
    }

    // ---- 内部方法 ----

    private CiamOAuthClientDo findEnabledClient(String clientId) {
        CiamOAuthClientDo client = clientRepository.findByClientId(clientId)
                .orElseThrow(() -> new BusinessException(CiamErrorCode.CLIENT_NOT_FOUND));
        if (client.getClientStatus() == null || client.getClientStatus() != 1) {
            throw new BusinessException(CiamErrorCode.CLIENT_DISABLED);
        }
        return client;
    }

    /**
     * 解析状态字符串为数组: [status, clientId, scope, userId]
     */
    static String[] parseState(String state) {
        String[] parts = state.split("\\|", -1);
        if (parts.length < 4) {
            throw new BusinessException(CiamErrorCode.DEVICE_CODE_NOT_FOUND);
        }
        return parts;
    }

    private String generateDeviceCode() {
        byte[] bytes = new byte[DEVICE_CODE_BYTE_LENGTH];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * 生成人类可读的用户码（8 位大写字母）。
     */
    static String generateUserCode() {
        StringBuilder sb = new StringBuilder(USER_CODE_LENGTH);
        for (int i = 0; i < USER_CODE_LENGTH; i++) {
            sb.append((char) ('A' + SECURE_RANDOM.nextInt(26)));
        }
        return sb.toString();
    }
}
