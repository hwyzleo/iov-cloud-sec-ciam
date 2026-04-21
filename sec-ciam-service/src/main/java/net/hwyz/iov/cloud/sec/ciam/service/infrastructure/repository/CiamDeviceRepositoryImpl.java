package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.sec.ciam.service.application.mapper.DeviceMapper;
import net.hwyz.iov.cloud.sec.ciam.service.domain.model.Device;
import net.hwyz.iov.cloud.sec.ciam.service.domain.query.DeviceQuery;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.CiamDeviceRepository;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.CiamDeviceMapper;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.dataobject.CiamDeviceDo;
import net.hwyz.iov.cloud.framework.web.util.PageUtil;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CiamDeviceRepositoryImpl implements CiamDeviceRepository {

    private final CiamDeviceMapper mapper;
    private final DeviceMapper deviceMapper = DeviceMapper.INSTANCE;

    @Override
    public Optional<Device> findByDeviceId(String deviceId) {
        CiamDeviceDo entity = mapper.selectOne(
                new LambdaQueryWrapper<CiamDeviceDo>()
                        .eq(CiamDeviceDo::getDeviceId, deviceId)
                        .eq(CiamDeviceDo::getRowValid, 1));
        return Optional.ofNullable(deviceMapper.toDomain(entity));
    }

    @Override
    public List<Device> findByUserIdAndStatus(String userId, int deviceStatus) {
        List<CiamDeviceDo> entities = mapper.selectList(
                new LambdaQueryWrapper<CiamDeviceDo>()
                        .eq(CiamDeviceDo::getUserId, userId)
                        .eq(CiamDeviceDo::getDeviceStatus, deviceStatus)
                        .eq(CiamDeviceDo::getRowValid, 1));
        return PageUtil.convert(entities, deviceMapper::toDomain);
    }

    @Override
    public List<Device> findByUserId(String userId) {
        List<CiamDeviceDo> entities = mapper.selectList(
                new LambdaQueryWrapper<CiamDeviceDo>()
                        .eq(CiamDeviceDo::getUserId, userId)
                        .eq(CiamDeviceDo::getRowValid, 1));
        return PageUtil.convert(entities, deviceMapper::toDomain);
    }

    @Override
    public List<Device> search(DeviceQuery query) {
        LambdaQueryWrapper<CiamDeviceDo> wrapper = new LambdaQueryWrapper<CiamDeviceDo>()
                .eq(CiamDeviceDo::getRowValid, 1)
                .eq(query.getDeviceId() != null, CiamDeviceDo::getDeviceId, query.getDeviceId())
                .eq(query.getUserId() != null, CiamDeviceDo::getUserId, query.getUserId())
                .eq(query.getClientType() != null, CiamDeviceDo::getClientType, query.getClientType())
                .eq(query.getClientId() != null, CiamDeviceDo::getClientId, query.getClientId())
                .like(query.getDeviceName() != null, CiamDeviceDo::getDeviceName, query.getDeviceName())
                .like(query.getDeviceOs() != null, CiamDeviceDo::getDeviceOs, query.getDeviceOs())
                .eq(query.getDeviceStatus() != null, CiamDeviceDo::getDeviceStatus, query.getDeviceStatus())
                .eq(query.getTrustedFlag() != null, CiamDeviceDo::getTrustedFlag, Boolean.TRUE.equals(query.getTrustedFlag()) ? 1 : 0)
                .eq(query.getLanguage() != null, CiamDeviceDo::getLanguage, query.getLanguage())
                .ge(query.getStartTime() != null, CiamDeviceDo::getCreateTime, query.getStartTime() != null ? query.getStartTime().toInstant() : null)
                .le(query.getEndTime() != null, CiamDeviceDo::getCreateTime, query.getEndTime() != null ? query.getEndTime().toInstant() : null);

        List<CiamDeviceDo> entities = mapper.selectList(wrapper);
        return PageUtil.convert(entities, deviceMapper::toDomain);
    }

    @Override
    public Optional<Device> findByDeviceFingerprint(String deviceFingerprint) {
        CiamDeviceDo entity = mapper.selectOne(
                new LambdaQueryWrapper<CiamDeviceDo>()
                        .eq(CiamDeviceDo::getDeviceFingerprint, deviceFingerprint)
                        .eq(CiamDeviceDo::getRowValid, 1));
        return Optional.ofNullable(deviceMapper.toDomain(entity));
    }

    @Override
    public int insert(Device device) {
        CiamDeviceDo entity = deviceMapper.toDo(device);
        return mapper.insert(entity);
    }

    @Override
    public int updateByDeviceId(Device device) {
        CiamDeviceDo entity = deviceMapper.toDo(device);
        return mapper.update(entity,
                new LambdaUpdateWrapper<CiamDeviceDo>()
                        .eq(CiamDeviceDo::getDeviceId, entity.getDeviceId()));
    }
}
