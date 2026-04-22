package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.framework.common.util.DateTimeUtil;
import net.hwyz.iov.cloud.sec.ciam.service.domain.enums.SessionStatus;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.CiamSessionRepository;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.CiamSessionMapper;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.dataobject.SessionPo;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CiamSessionRepositoryImpl implements CiamSessionRepository {

    private final CiamSessionMapper mapper;

    @Override
    public Optional<SessionPo> findBySessionId(String sessionId) {
        return Optional.ofNullable(mapper.selectOne(
                new LambdaQueryWrapper<SessionPo>()
                        .eq(SessionPo::getSessionId, sessionId)));
    }

    @Override
    public List<SessionPo> findByUserIdAndStatus(String userId, int sessionStatus) {
        return mapper.selectList(
                new LambdaQueryWrapper<SessionPo>()
                        .eq(SessionPo::getUserId, userId)
                        .eq(SessionPo::getSessionStatus, sessionStatus)
                        .eq(SessionPo::getRowValid, 1));
    }

    @Override
    public List<SessionPo> findByDeviceId(String deviceId) {
        return mapper.selectList(
                new LambdaQueryWrapper<SessionPo>()
                        .eq(SessionPo::getDeviceId, deviceId)
                        .eq(SessionPo::getRowValid, 1));
    }

    @Override
    public List<SessionPo> findByDeviceIdAndStatus(String deviceId, int sessionStatus) {
        return mapper.selectList(
                new LambdaQueryWrapper<SessionPo>()
                        .eq(SessionPo::getDeviceId, deviceId)
                        .eq(SessionPo::getSessionStatus, sessionStatus)
                        .eq(SessionPo::getRowValid, 1));
    }

    @Override
    public int insert(SessionPo entity) {
        return mapper.insert(entity);
    }

    @Override
    public int updateBySessionId(SessionPo entity) {
        return mapper.update(entity,
                new LambdaUpdateWrapper<SessionPo>()
                        .eq(SessionPo::getSessionId, entity.getSessionId()));
    }

    @Override
    public int invalidateAllByUserId(String userId) {
        SessionPo update = new SessionPo();
        update.setSessionStatus(SessionStatus.INVALID.getCode());
        update.setLogoutTime(DateTimeUtil.getNowInstant());
        return mapper.update(update,
                new LambdaUpdateWrapper<SessionPo>()
                        .eq(SessionPo::getUserId, userId)
                        .eq(SessionPo::getSessionStatus, SessionStatus.ACTIVE.getCode()));
    }
}
