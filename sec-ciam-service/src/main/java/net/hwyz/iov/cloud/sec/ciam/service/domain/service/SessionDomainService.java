package net.hwyz.iov.cloud.sec.ciam.service.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.exception.BusinessException;
import net.hwyz.iov.cloud.sec.ciam.service.common.exception.CiamErrorCode;
import net.hwyz.iov.cloud.framework.common.util.DateTimeUtil;
import net.hwyz.iov.cloud.sec.ciam.service.domain.enums.DeviceStatus;
import net.hwyz.iov.cloud.sec.ciam.service.domain.enums.SessionStatus;
import net.hwyz.iov.cloud.sec.ciam.service.domain.model.Device;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.CiamDeviceRepository;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.CiamRefreshTokenRepository;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.CiamSessionRepository;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.po.SessionPo;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 会话领域服务 — 封装退出登录、会话失效等逻辑。
 * <p>
 * 对应 design.md 模块 9（注销与生命周期管理模块）中的退出登录能力。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionDomainService {

    private final CiamSessionRepository sessionRepository;
    private final CiamRefreshTokenRepository refreshTokenRepository;
    private final CiamDeviceRepository deviceRepository;

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
        SessionPo session = sessionRepository.findBySessionId(sessionId)
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
        SessionPo session = sessionRepository.findBySessionId(sessionId)
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
    public List<SessionPo> findUserSessions(String userId) {
        return sessionRepository.findByUserIdAndStatus(userId, SessionStatus.ACTIVE.getCode());
    }

    /**
     * 根据会话 ID 查询会话信息。
     *
     * @param sessionId 会话业务唯一标识
     * @return 会话数据对象（可能为空）
     */
    public java.util.Optional<SessionPo> findBySessionId(String sessionId) {
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
        List<SessionPo> activeSessions = sessionRepository.findByDeviceIdAndStatus(
                deviceId, SessionStatus.ACTIVE.getCode());
        for (SessionPo session : activeSessions) {
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
