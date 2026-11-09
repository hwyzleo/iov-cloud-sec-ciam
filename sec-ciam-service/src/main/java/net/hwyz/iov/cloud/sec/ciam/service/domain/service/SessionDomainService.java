package net.hwyz.iov.cloud.sec.ciam.service.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.exception.BusinessException;
import net.hwyz.iov.cloud.sec.ciam.service.common.exception.CiamErrorCode;
import net.hwyz.iov.cloud.framework.common.util.DateTimeUtil;
import net.hwyz.iov.cloud.sec.ciam.service.domain.enums.DeviceStatus;
import net.hwyz.iov.cloud.sec.ciam.service.domain.enums.SessionStatus;
import net.hwyz.iov.cloud.sec.ciam.service.domain.model.Device;
import net.hwyz.iov.cloud.sec.ciam.service.domain.model.Session;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.DeviceRepository;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.RefreshTokenRepository;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.SessionRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 会话领域服务 — 封装退出登录、会话失效等逻辑。
 * <p>
 * 对应 design.md 模块 9（注销与生命周期管理模块）中的退出登录能力。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionDomainService {

    private final SessionRepository sessionRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final DeviceRepository deviceRepository;

    /**
     * 建立手机登录会话事实（SEC-CIAM-DSN-CR-003 RD-003-1/2，§4.1）。
     * <p>
     * 会话是手机登录上下文的事实源：user/client/clientType/device/scope/expireTime 均在此冻结。
     *
     * @param userId     用户业务唯一标识
     * @param clientId   OAuth 客户端标识
     * @param clientType 终端类型（手机为 MOBILE）
     * @param deviceId   设备标识
     * @param scope      登录时冻结的授权范围（空格分隔，必须非空）
     * @param expireTime 绝对到期时间（登录时间 + 客户端 refreshTokenTtl）
     * @return 已持久化的 ACTIVE 会话
     */
    public Session openSession(String userId,
                               String clientId,
                               String clientType,
                               String deviceId,
                               String scope,
                               Instant expireTime) {
        Instant now = DateTimeUtil.getNowInstant();
        Session session = Session.builder()
                .sessionId(UUID.randomUUID().toString())
                .userId(userId)
                .deviceId(deviceId)
                .clientId(clientId)
                .clientType(clientType)
                .scope(scope)
                .riskLevel(0)
                .sessionStatus(SessionStatus.ACTIVE.getCode())
                .loginTime(now)
                .lastActiveTime(now)
                .expireTime(expireTime)
                .modifyTime(now)
                .description("Mobile code login")
                .build();
        sessionRepository.insert(session);
        log.info("建立手机会话：sessionId={}, userId={}, clientId={}, clientType={}",
                session.getSessionId(), userId, clientId, clientType);
        return session;
    }

    /**
     * 要求会话存在、处于 ACTIVE 且未到期（SEC-CIAM-DSN-CR-003 §5.4）。
     *
     * @param sessionId 会话业务唯一标识
     * @return 有效会话
     * @throws BusinessException TOKEN_INVALID（缺少会话标识或会话非活跃）、
     *                           SESSION_NOT_FOUND（不存在）、SESSION_EXPIRED（已到期）
     */
    public Session requireActiveSession(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new BusinessException(CiamErrorCode.TOKEN_INVALID, "令牌缺少会话标识");
        }

        Session session = sessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new BusinessException(CiamErrorCode.SESSION_NOT_FOUND));

        if (session.getSessionStatus() == null
                || session.getSessionStatus() != SessionStatus.ACTIVE.getCode()) {
            throw new BusinessException(CiamErrorCode.TOKEN_INVALID, "会话已非活跃状态");
        }

        if (session.getExpireTime() == null
                || !DateTimeUtil.getNowInstant().isBefore(session.getExpireTime())) {
            throw new BusinessException(CiamErrorCode.SESSION_EXPIRED);
        }

        return session;
    }

    /**
     * 校验 Token 声明与会话事实完全一致（SEC-CIAM-DSN-CR-003 §5.4，RD-003-8）。
     * <p>
     * 要求 user/client/clientType/device 四项均与持久会话一致，任一缺失或不匹配均视为令牌无效。
     *
     * @param session    持久会话
     * @param userId     Token sub
     * @param clientId   Token client_id
     * @param clientType Token client_type
     * @param deviceId   Token device_id
     * @throws BusinessException TOKEN_INVALID 绑定不一致
     */
    public void assertClaimBinding(Session session,
                                   String userId,
                                   String clientId,
                                   String clientType,
                                   String deviceId) {
        if (isBlank(userId) || isBlank(clientId) || isBlank(clientType) || isBlank(deviceId)
                || !userId.equals(session.getUserId())
                || !clientId.equals(session.getClientId())
                || !clientType.equals(session.getClientType())
                || !deviceId.equals(session.getDeviceId())) {
            throw new BusinessException(CiamErrorCode.TOKEN_INVALID, "令牌与会话绑定不一致");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /**
     * 用户主动退出登录。
     * <p>
     * 流程：
     * <ol>
     *   <li>根据 sessionId 查找会话</li>
     *   <li>校验会话归属于指定 userId</li>
     *   <li>将会话状态设为 KICKED（下线）</li>
     *   <li>设置 logout_time</li>
     *   <li>撤销该会话关联的所有有效 Refresh Token</li>
     * </ol>
     *
     * @param sessionId 会话业务唯一标识
     * @param userId    用户业务唯一标识
     * @throws BusinessException SESSION_NOT_FOUND 会话不存在；FORBIDDEN 会话不属于该用户
     */
    public void logout(String sessionId, String userId) {
        Session session = sessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new BusinessException(CiamErrorCode.SESSION_NOT_FOUND));

        if (!userId.equals(session.getUserId())) {
            throw new BusinessException(CiamErrorCode.FORBIDDEN);
        }

        if (session.getSessionStatus() != SessionStatus.ACTIVE.getCode()) {
            log.info("会话已非活跃状态，跳过退出操作：sessionId={}, status={}",
                    sessionId, session.getSessionStatus());
            return;
        }

        session.setSessionStatus(SessionStatus.KICKED.getCode());
        session.setLogoutTime(DateTimeUtil.getNowInstant());
        session.setModifyTime(DateTimeUtil.getNowInstant());
        sessionRepository.updateBySessionId(session);

        int revokedCount = refreshTokenRepository.revokeAllBySessionId(sessionId);
        log.info("退出登录完成：sessionId={}, userId={}, revokedTokens={}",
                sessionId, userId, revokedCount);
    }

    /**
     * 内部强制失效会话（密码修改、风险事件等场景）。
     * <p>
     * 与 {@link #logout} 不同，此方法不校验 userId 归属，
     * 将会话状态设为 INVALID（失效），并撤销关联的 Refresh Token。
     *
     * @param sessionId 会话业务唯一标识
     * @throws BusinessException SESSION_NOT_FOUND 会话不存在
     */
    public void invalidateSession(String sessionId) {
        Session session = sessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new BusinessException(CiamErrorCode.SESSION_NOT_FOUND));

        if (session.getSessionStatus() != SessionStatus.ACTIVE.getCode()) {
            log.info("会话已非活跃状态，跳过失效操作：sessionId={}, status={}",
                    sessionId, session.getSessionStatus());
            return;
        }

        session.setSessionStatus(SessionStatus.INVALID.getCode());
        session.setLogoutTime(DateTimeUtil.getNowInstant());
        session.setModifyTime(DateTimeUtil.getNowInstant());
        sessionRepository.updateBySessionId(session);

        int revokedCount = refreshTokenRepository.revokeAllBySessionId(sessionId);
        log.info("会话强制失效完成：sessionId={}, revokedTokens={}", sessionId, revokedCount);
    }

    /**
     * 查询用户的所有活跃会话。
     *
     * @param userId 用户业务唯一标识
     * @return 活跃会话列表
     */
    public List<Session> findUserSessions(String userId) {
        return sessionRepository.findByUserIdAndStatus(userId, SessionStatus.ACTIVE.getCode());
    }

    /**
     * 根据会话 ID 查询会话信息。
     *
     * @param sessionId 会话业务唯一标识
     * @return 会话数据对象（可能为空）
     */
    public Optional<Session> findBySessionId(String sessionId) {
        return sessionRepository.findBySessionId(sessionId);
    }


    /**
     * 查询用户的所有活跃设备。
     *
     * @param userId 用户业务唯一标识
     * @return 活跃设备列表
     */
    public List<Device> findUserDevices(String userId) {
        return deviceRepository.findByUserIdAndStatus(userId, DeviceStatus.ACTIVE.getCode());
    }

    /**
     * 強制下线指定会话。
     * <p>
     * 复用 {@link #logout} 逻辑：校验会话归属、设置 KICKED 状态、撤销关联 Refresh Token。
     *
     * @param sessionId 会话业务唯一标识
     * @param userId    用户业务唯一标识
     * @throws BusinessException SESSION_NOT_FOUND 会话不存在；FORBIDDEN 会话不属于该用户
     */
    public void kickSession(String sessionId, String userId) {
        logout(sessionId, userId);
        log.info("强制下线会话完成：sessionId={}, userId={}", sessionId, userId);
    }

    /**
     * 强制下线指定设备的所有活跃会话，并将设备状态设为失效。
     *
     * @param deviceId 设备业务唯一标识
     * @param userId   用户业务唯一标识
     * @throws BusinessException DEVICE_NOT_FOUND 设备不存在；FORBIDDEN 设备不属于该用户
     */
    public void kickDevice(String deviceId, String userId) {
        Device device = deviceRepository.findByDeviceId(deviceId)
                .orElseThrow(() -> new BusinessException(CiamErrorCode.DEVICE_NOT_FOUND));

        if (!userId.equals(device.getUserId())) {
            throw new BusinessException(CiamErrorCode.FORBIDDEN);
        }

        // 下线该设备上的所有活跃会话
        List<Session> activeSessions = sessionRepository.findByDeviceIdAndStatus(
                deviceId, SessionStatus.ACTIVE.getCode());
        for (Session session : activeSessions) {
            session.setSessionStatus(SessionStatus.KICKED.getCode());
            session.setLogoutTime(DateTimeUtil.getNowInstant());
            session.setModifyTime(DateTimeUtil.getNowInstant());
            sessionRepository.updateBySessionId(session);
            refreshTokenRepository.revokeAllBySessionId(session.getSessionId());
        }

        // 将设备状态设为失效
        device.setDeviceStatus(DeviceStatus.INVALID.getCode());
        deviceRepository.updateByDeviceId(device);

        log.info("强制下线设备完成：deviceId={}, userId={}, offlineSessions={}",
                deviceId, userId, activeSessions.size());
    }
}
