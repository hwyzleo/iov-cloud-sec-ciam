package net.hwyz.iov.cloud.sec.ciam.domain.repository;

import net.hwyz.iov.cloud.sec.ciam.infrastructure.repository.dao.dataobject.CiamSessionDo;

import java.util.List;
import java.util.Optional;

/**
 * 会话表仓储接口。
 */
public interface CiamSessionRepository {

    /** 根据业务 ID 查询 */
    Optional<CiamSessionDo> findBySessionId(String sessionId);

    /** 根据用户 ID 和会话状态查询 */
    List<CiamSessionDo> findByUserIdAndStatus(String userId, int sessionStatus);

    /** 根据设备 ID 查询 */
    List<CiamSessionDo> findByDeviceId(String deviceId);

    /** 根据设备 ID 和会话状态查询 */
    List<CiamSessionDo> findByDeviceIdAndStatus(String deviceId, int sessionStatus);

    /** 插入会话记录 */
    int insert(CiamSessionDo entity);

    /** 根据业务 ID 更新 */
    int updateBySessionId(CiamSessionDo entity);

    /** 批量失效用户所有有效会话 */
    int invalidateAllByUserId(String userId);
}
