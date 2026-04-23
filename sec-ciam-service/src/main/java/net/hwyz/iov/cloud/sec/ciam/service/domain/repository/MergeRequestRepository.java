package net.hwyz.iov.cloud.sec.ciam.service.domain.repository;

import net.hwyz.iov.cloud.sec.ciam.service.domain.model.MergeRequest;
import java.util.List;
import java.util.Optional;

public interface MergeRequestRepository {
    Optional<MergeRequest> findByMergeRequestId(String mergeRequestId);
    List<MergeRequest> findByReviewStatus(int reviewStatus);
    int insert(MergeRequest entity);
    int updateByMergeRequestId(MergeRequest entity);
}
