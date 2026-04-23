package net.hwyz.iov.cloud.sec.ciam.service.domain.repository;

import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.po.SessionPo;

import java.util.List;
import java.util.Optional;

/**
 * 会话表仓储接口。
 */
public interface SessionRepository {

    /** 根据业务 ID 查询 */
    Optional<SessionPo> findBySessionId(String sessionId);

    /** 根据用户 ID 和会话状态查询 */
    List<SessionPo> findByUserIdAndStatus(String userId, int sessionStatus);

    /** 根据设备 ID 查询 */
    List<SessionPo> findByDeviceId(String deviceId);

    /** 根据设备 ID 和会话状态查询 */
    List<SessionPo> findByDeviceIdAndStatus(String deviceId, int sessionStatus);

    /** 插入会话记录 */
    int insert(SessionPo entity);

    /** 根据业务 ID 更新 */
    int updateBySessionId(SessionPo entity);

    /** 批量失效用户所有有效会话 */
    int invalidateAllByUserId(String userId);
}
