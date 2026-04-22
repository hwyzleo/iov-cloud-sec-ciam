package net.hwyz.iov.cloud.sec.ciam.service.domain.repository;

import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.dataobject.DeactivationRequestPo;

import java.util.List;
import java.util.Optional;

/**
 * 注销申请表仓储接口。
 */
public interface CiamDeactivationRequestRepository {

    /** 根据业务 ID 查询 */
    Optional<DeactivationRequestPo> findByDeactivationRequestId(String deactivationRequestId);

    /** 根据用户 ID 和审核状态查询 */
    List<DeactivationRequestPo> findByUserIdAndReviewStatus(String userId, int reviewStatus);

    /** 根据审核状态查询 */
    List<DeactivationRequestPo> findByReviewStatus(int reviewStatus);

    /** 根据执行状态查询 */
    List<DeactivationRequestPo> findByExecuteStatus(int executeStatus);

    /** 插入注销申请记录 */
    int insert(DeactivationRequestPo entity);

    /** 根据业务 ID 更新 */
    int updateByDeactivationRequestId(DeactivationRequestPo entity);
}
