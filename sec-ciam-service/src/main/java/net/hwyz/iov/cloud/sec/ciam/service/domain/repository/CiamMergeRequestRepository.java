package net.hwyz.iov.cloud.sec.ciam.service.domain.repository;

import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.dataobject.CiamMergeRequestDo;

import java.util.List;
import java.util.Optional;

/**
 * 账号合并申请表仓储接口。
 */
public interface CiamMergeRequestRepository {

    /** 根据业务 ID 查询 */
    Optional<CiamMergeRequestDo> findByMergeRequestId(String mergeRequestId);

    /** 根据审核状态查询 */
    List<CiamMergeRequestDo> findByReviewStatus(int reviewStatus);

    /** 根据源用户 ID 查询 */
    List<CiamMergeRequestDo> findBySourceUserId(String sourceUserId);

    /** 根据目标用户 ID 查询 */
    List<CiamMergeRequestDo> findByTargetUserId(String targetUserId);

    /** 插入合并申请记录 */
    int insert(CiamMergeRequestDo entity);

    /** 根据业务 ID 更新 */
    int updateByMergeRequestId(CiamMergeRequestDo entity);
}
