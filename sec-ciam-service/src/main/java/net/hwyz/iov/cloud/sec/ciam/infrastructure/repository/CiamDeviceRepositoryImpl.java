package net.hwyz.iov.cloud.sec.ciam.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.sec.ciam.domain.repository.CiamDeviceRepository;
import net.hwyz.iov.cloud.sec.ciam.infrastructure.repository.dao.CiamDeviceMapper;
import net.hwyz.iov.cloud.sec.ciam.infrastructure.repository.dao.dataobject.CiamDeviceDo;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CiamDeviceRepositoryImpl implements CiamDeviceRepository {

    private final CiamDeviceMapper mapper;

    @Override
    public Optional<CiamDeviceDo> findByDeviceId(String deviceId) {
        return Optional.ofNullable(mapper.selectOne(
                new LambdaQueryWrapper<CiamDeviceDo>()
                        .eq(CiamDeviceDo::getDeviceId, deviceId)
                        .eq(CiamDeviceDo::getRowValid, 1)));
    }

    @Override
    public List<CiamDeviceDo> findByUserIdAndStatus(String userId, int deviceStatus) {
        return mapper.selectList(
                new LambdaQueryWrapper<CiamDeviceDo>()
                        .eq(CiamDeviceDo::getUserId, userId)
                        .eq(CiamDeviceDo::getDeviceStatus, deviceStatus)
                        .eq(CiamDeviceDo::getRowValid, 1));
    }

    @Override
    public List<CiamDeviceDo> findByUserId(String userId) {
        return mapper.selectList(
                new LambdaQueryWrapper<CiamDeviceDo>()
                        .eq(CiamDeviceDo::getUserId, userId)
                        .eq(CiamDeviceDo::getRowValid, 1));
    }

    @Override
    public List<CiamDeviceDo> findAll() {
        return mapper.selectList(
                new LambdaQueryWrapper<CiamDeviceDo>()
                        .eq(CiamDeviceDo::getRowValid, 1));
    }

    @Override
    public Optional<CiamDeviceDo> findByDeviceFingerprint(String deviceFingerprint) {
        return Optional.ofNullable(mapper.selectOne(
                new LambdaQueryWrapper<CiamDeviceDo>()
                        .eq(CiamDeviceDo::getDeviceFingerprint, deviceFingerprint)
                        .eq(CiamDeviceDo::getRowValid, 1)));
    }

    @Override
    public int insert(CiamDeviceDo entity) {
        return mapper.insert(entity);
    }

    @Override
    public int updateByDeviceId(CiamDeviceDo entity) {
        return mapper.update(entity,
                new LambdaUpdateWrapper<CiamDeviceDo>()
                        .eq(CiamDeviceDo::getDeviceId, entity.getDeviceId()));
    }
}
