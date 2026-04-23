package net.hwyz.iov.cloud.sec.ciam.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.exception.BusinessException;
import net.hwyz.iov.cloud.framework.common.util.DateTimeUtil;
import net.hwyz.iov.cloud.framework.web.util.PageUtil;
import net.hwyz.iov.cloud.sec.ciam.service.application.dto.DeviceInfoDto2;
import net.hwyz.iov.cloud.sec.ciam.service.application.assembler.DeviceAssembler;
import net.hwyz.iov.cloud.sec.ciam.service.application.dto.query.DeviceQuery;
import net.hwyz.iov.cloud.sec.ciam.service.common.exception.CiamErrorCode;
import net.hwyz.iov.cloud.sec.ciam.service.domain.model.Device;
import net.hwyz.iov.cloud.sec.ciam.service.domain.model.DeviceSearchCriteria;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.DeviceRepository;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 运营后台设备查询应用服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceQueryAppService {

    private final DeviceRepository deviceRepository;

    public List<DeviceSearchResult> queryDeviceList(DeviceQuery query) {
        DeviceSearchCriteria criteria = DeviceSearchCriteria.builder()
                .deviceId(query.getDeviceId())
                .userId(query.getUserId())
                .clientType(query.getClientType())
                .clientId(query.getClientId())
                .deviceName(query.getDeviceName())
                .deviceOs(query.getDeviceOs())
                .deviceStatus(query.getDeviceStatus())
                .trustedFlag(query.getTrustedFlag())
                .language(query.getLanguage())
                .startTime(query.getStartTime())
                .endTime(query.getEndTime())
                .build();
        List<Device> allDevices = deviceRepository.search(criteria);

        return PageUtil.convert(allDevices, device -> new DeviceSearchResult(
                device.getDeviceId(),
                device.getUserId(),
                device.getClientType(),
                device.getClientId(),
                device.getDeviceName(),
                device.getDeviceOs(),
                device.getAppVersion(),
                device.getDeviceFingerprint(),
                device.getTrustedFlag(),
                device.getDeviceStatus(),
                device.getLanguage(),
                DateTimeUtil.instantToOffsetDateTime(device.getFirstLoginTime()),
                DateTimeUtil.instantToOffsetDateTime(device.getLastLoginTime()),
                DateTimeUtil.instantToOffsetDateTime(device.getCreateTime()),
                device.getDescription()
        ));
    }

    public DeviceInfoDto2 queryDevice(String deviceId) {
        Device device = deviceRepository.findByDeviceId(deviceId)
                .orElseThrow(() -> new BusinessException(CiamErrorCode.DEVICE_NOT_FOUND));
        return DeviceAssembler.INSTANCE.toDto(device);
    }

    public List<DeviceInfoDto2> queryUserDevices(String userId) {
        return deviceRepository.findByUserId(userId).stream()
                .map(DeviceAssembler.INSTANCE::toDto)
                .collect(Collectors.toList());
    }

    public record DeviceSearchResult(
            String deviceId,
            String userId,
            String clientType,
            String clientId,
            String deviceName,
            String deviceOs,
            String appVersion,
            String deviceFingerprint,
            Integer trustedFlag,
            Integer deviceStatus,
            String language,
            OffsetDateTime firstLoginTime,
            OffsetDateTime lastLoginTime,
            OffsetDateTime createTime,
            String description
    ) {
    }
}
