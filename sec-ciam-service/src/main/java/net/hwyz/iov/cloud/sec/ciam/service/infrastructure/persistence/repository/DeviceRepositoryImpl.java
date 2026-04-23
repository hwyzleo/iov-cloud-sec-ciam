package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.sec.ciam.service.application.assembler.DeviceAssembler;
import net.hwyz.iov.cloud.sec.ciam.service.domain.model.Device;
import net.hwyz.iov.cloud.sec.ciam.service.domain.model.DeviceSearchCriteria;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.DeviceRepository;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.mapper.CiamDeviceMapper;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.po.DevicePo;
import net.hwyz.iov.cloud.framework.web.util.PageUtil;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class DeviceRepositoryImpl implements DeviceRepository {

    private final CiamDeviceMapper mapper;
    private final DeviceAssembler deviceMapper = DeviceAssembler.INSTANCE;

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
    public List<Device> search(DeviceSearchCriteria criteria) {
        LambdaQueryWrapper<DevicePo> wrapper = new LambdaQueryWrapper<DevicePo>()
                .eq(DevicePo::getRowValid, 1)
                .eq(criteria.getDeviceId() != null, DevicePo::getDeviceId, criteria.getDeviceId())
                .eq(criteria.getUserId() != null, DevicePo::getUserId, criteria.getUserId())
                .eq(criteria.getClientType() != null, DevicePo::getClientType, criteria.getClientType())
                .eq(criteria.getClientId() != null, DevicePo::getClientId, criteria.getClientId())
                .like(criteria.getDeviceName() != null, DevicePo::getDeviceName, criteria.getDeviceName())
                .like(criteria.getDeviceOs() != null, DevicePo::getDeviceOs, criteria.getDeviceOs())
                .eq(criteria.getDeviceStatus() != null, DevicePo::getDeviceStatus, criteria.getDeviceStatus())
                .eq(criteria.getTrustedFlag() != null, DevicePo::getTrustedFlag, Boolean.TRUE.equals(criteria.getTrustedFlag()) ? 1 : 0)
                .eq(criteria.getLanguage() != null, DevicePo::getLanguage, criteria.getLanguage())
                .ge(criteria.getStartTime() != null, DevicePo::getCreateTime, criteria.getStartTime() != null ? criteria.getStartTime().toInstant() : null)
                .le(criteria.getEndTime() != null, DevicePo::getCreateTime, criteria.getEndTime() != null ? criteria.getEndTime().toInstant() : null);

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
