package net.hwyz.iov.cloud.sec.ciam.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.framework.common.util.DateTimeUtil;
import net.hwyz.iov.cloud.sec.ciam.domain.enums.SessionStatus;
import net.hwyz.iov.cloud.sec.ciam.domain.repository.CiamSessionRepository;
import net.hwyz.iov.cloud.sec.ciam.infrastructure.repository.dao.CiamSessionMapper;
import net.hwyz.iov.cloud.sec.ciam.infrastructure.repository.dao.dataobject.CiamSessionDo;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CiamSessionRepositoryImpl implements CiamSessionRepository {

    private final CiamSessionMapper mapper;

    @Override
    public Optional<CiamSessionDo> findBySessionId(String sessionId) {
        return Optional.ofNullable(mapper.selectOne(
                new LambdaQueryWrapper<CiamSessionDo>()
                        .eq(CiamSessionDo::getSessionId, sessionId)));
    }

    @Override
    public List<CiamSessionDo> findByUserIdAndStatus(String userId, int sessionStatus) {
        return mapper.selectList(
                new LambdaQueryWrapper<CiamSessionDo>()
                        .eq(CiamSessionDo::getUserId, userId)
                        .eq(CiamSessionDo::getSessionStatus, sessionStatus)
                        .eq(CiamSessionDo::getRowValid, 1));
    }

    @Override
    public List<CiamSessionDo> findByDeviceId(String deviceId) {
        return mapper.selectList(
                new LambdaQueryWrapper<CiamSessionDo>()
                        .eq(CiamSessionDo::getDeviceId, deviceId)
                        .eq(CiamSessionDo::getRowValid, 1));
    }

    @Override
    public List<CiamSessionDo> findByDeviceIdAndStatus(String deviceId, int sessionStatus) {
        return mapper.selectList(
                new LambdaQueryWrapper<CiamSessionDo>()
                        .eq(CiamSessionDo::getDeviceId, deviceId)
                        .eq(CiamSessionDo::getSessionStatus, sessionStatus)
                        .eq(CiamSessionDo::getRowValid, 1));
    }

    @Override
    public int insert(CiamSessionDo entity) {
        return mapper.insert(entity);
    }

    @Override
    public int updateBySessionId(CiamSessionDo entity) {
        return mapper.update(entity,
                new LambdaUpdateWrapper<CiamSessionDo>()
                        .eq(CiamSessionDo::getSessionId, entity.getSessionId()));
    }

    @Override
    public int invalidateAllByUserId(String userId) {
        CiamSessionDo update = new CiamSessionDo();
        update.setSessionStatus(SessionStatus.INVALID.getCode());
        update.setLogoutTime(DateTimeUtil.getNowInstant());
        return mapper.update(update,
                new LambdaUpdateWrapper<CiamSessionDo>()
                        .eq(CiamSessionDo::getUserId, userId)
                        .eq(CiamSessionDo::getSessionStatus, SessionStatus.ACTIVE.getCode()));
    }
}
