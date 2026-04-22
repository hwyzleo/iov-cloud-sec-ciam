package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.sec.ciam.service.application.mapper.DeviceMapper;
import net.hwyz.iov.cloud.sec.ciam.service.domain.model.Device;
import net.hwyz.iov.cloud.sec.ciam.service.domain.query.DeviceQuery;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.CiamDeviceRepository;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.CiamDeviceMapper;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.dataobject.DevicePo;
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
        DevicePo entity = mapper.selectOne(
                new LambdaQueryWrapper<DevicePo>()
                        .eq(DevicePo::getDeviceId, deviceId)
                        .eq(DevicePo::getRowValid, 1));
        return Optional.ofNullable(deviceMapper.toDomain(entity));
    }

    @Override
    public List<Device> findByUserIdAndStatus(String userId, int deviceStatus) {
        List<DevicePo> entities = mapper.selectList(
                new LambdaQueryWrapper<DevicePo>()
                        .eq(DevicePo::getUserId, userId)
                        .eq(DevicePo::getDeviceStatus, deviceStatus)
                        .eq(DevicePo::getRowValid, 1));
        return PageUtil.convert(entities, deviceMapper::toDomain);
    }

    @Override
    public List<Device> findByUserId(String userId) {
        List<DevicePo> entities = mapper.selectList(
                new LambdaQueryWrapper<DevicePo>()
                        .eq(DevicePo::getUserId, userId)
                        .eq(DevicePo::getRowValid, 1));
        return PageUtil.convert(entities, deviceMapper::toDomain);
    }

    @Override
    public List<Device> search(DeviceQuery query) {
        LambdaQueryWrapper<DevicePo> wrapper = new LambdaQueryWrapper<DevicePo>()
                .eq(DevicePo::getRowValid, 1)
                .eq(query.getDeviceId() != null, DevicePo::getDeviceId, query.getDeviceId())
                .eq(query.getUserId() != null, DevicePo::getUserId, query.getUserId())
                .eq(query.getClientType() != null, DevicePo::getClientType, query.getClientType())
                .eq(query.getClientId() != null, DevicePo::getClientId, query.getClientId())
                .like(query.getDeviceName() != null, DevicePo::getDeviceName, query.getDeviceName())
                .like(query.getDeviceOs() != null, DevicePo::getDeviceOs, query.getDeviceOs())
                .eq(query.getDeviceStatus() != null, DevicePo::getDeviceStatus, query.getDeviceStatus())
                .eq(query.getTrustedFlag() != null, DevicePo::getTrustedFlag, Boolean.TRUE.equals(query.getTrustedFlag()) ? 1 : 0)
                .eq(query.getLanguage() != null, DevicePo::getLanguage, query.getLanguage())
                .ge(query.getStartTime() != null, DevicePo::getCreateTime, query.getStartTime() != null ? query.getStartTime().toInstant() : null)
                .le(query.getEndTime() != null, DevicePo::getCreateTime, query.getEndTime() != null ? query.getEndTime().toInstant() : null);

        List<DevicePo> entities = mapper.selectList(wrapper);
        return PageUtil.convert(entities, deviceMapper::toDomain);
    }

    @Override
    public Optional<Device> findByDeviceFingerprint(String deviceFingerprint) {
        DevicePo entity = mapper.selectOne(
                new LambdaQueryWrapper<DevicePo>()
                        .eq(DevicePo::getDeviceFingerprint, deviceFingerprint)
                        .eq(DevicePo::getRowValid, 1));
        return Optional.ofNullable(deviceMapper.toDomain(entity));
    }

    @Override
    public int insert(Device device) {
        DevicePo entity = deviceMapper.toDo(device);
        return mapper.insert(entity);
    }

    @Override
    public int updateByDeviceId(Device device) {
        DevicePo entity = deviceMapper.toDo(device);
        return mapper.update(entity,
                new LambdaUpdateWrapper<DevicePo>()
                        .eq(DevicePo::getDeviceId, entity.getDeviceId()));
    }
}
