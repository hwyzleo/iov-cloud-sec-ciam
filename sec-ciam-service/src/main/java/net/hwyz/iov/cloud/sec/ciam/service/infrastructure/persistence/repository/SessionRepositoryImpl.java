package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.framework.common.util.DateTimeUtil;
import net.hwyz.iov.cloud.sec.ciam.service.domain.enums.SessionStatus;
import net.hwyz.iov.cloud.sec.ciam.service.domain.model.Session;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.SessionRepository;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.converter.SessionPoConverter;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.mapper.CiamSessionMapper;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.po.SessionPo;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class SessionRepositoryImpl implements SessionRepository {

    private final CiamSessionMapper mapper;
    private final SessionPoConverter converter;

    @Override
    public Optional<Session> findBySessionId(String sessionId) {
        return Optional.ofNullable(mapper.selectOne(
                new LambdaQueryWrapper<SessionPo>()
                        .eq(SessionPo::getSessionId, sessionId))).map(converter::toDomain);
    }

    @Override
    public List<Session> findByUserIdAndStatus(String userId, int sessionStatus) {
        return mapper.selectList(
                new LambdaQueryWrapper<SessionPo>()
                        .eq(SessionPo::getUserId, userId)
                        .eq(SessionPo::getSessionStatus, sessionStatus)
                        .eq(SessionPo::getRowValid, 1))
                .stream().map(converter::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<Session> findByDeviceId(String deviceId) {
        return mapper.selectList(
                new LambdaQueryWrapper<SessionPo>()
                        .eq(SessionPo::getDeviceId, deviceId)
                        .eq(SessionPo::getRowValid, 1))
                .stream().map(converter::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<Session> findByDeviceIdAndStatus(String deviceId, int sessionStatus) {
        return mapper.selectList(
                new LambdaQueryWrapper<SessionPo>()
                        .eq(SessionPo::getDeviceId, deviceId)
                        .eq(SessionPo::getSessionStatus, sessionStatus)
                        .eq(SessionPo::getRowValid, 1))
                .stream().map(converter::toDomain).collect(Collectors.toList());
    }

    @Override
    public int insert(Session entity) {
        return mapper.insert(converter.toPo(entity));
    }

    @Override
    public int updateBySessionId(Session entity) {
        return mapper.update(converter.toPo(entity),
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
