package net.hwyz.iov.cloud.sec.ciam.service.domain.repository;

import net.hwyz.iov.cloud.sec.ciam.service.domain.model.DeactivationRequest;
import java.util.List;
import java.util.Optional;

public interface DeactivationRequestRepository {
    Optional<DeactivationRequest> findByDeactivationRequestId(String deactivationRequestId);
    List<DeactivationRequest> findByReviewStatus(int reviewStatus);
    int insert(DeactivationRequest entity);
    int updateByDeactivationRequestId(DeactivationRequest entity);
}
