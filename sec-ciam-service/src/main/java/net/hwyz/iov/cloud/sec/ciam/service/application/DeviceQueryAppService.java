package net.hwyz.iov.cloud.sec.ciam.service.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.exception.BusinessException;
import net.hwyz.iov.cloud.framework.common.util.DateTimeUtil;
import net.hwyz.iov.cloud.sec.ciam.service.application.dto.DeviceInfoDTO;
import net.hwyz.iov.cloud.sec.ciam.service.application.mapper.DeviceMapper;
import net.hwyz.iov.cloud.sec.ciam.service.common.exception.CiamErrorCode;
import net.hwyz.iov.cloud.sec.ciam.service.domain.model.Device;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.CiamDeviceRepository;
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

    private final CiamDeviceRepository deviceRepository;

    public List<DeviceSearchResult> queryDeviceList(String deviceId,
                                                     String userId,
                                                     String clientType,
                                                     String clientId,
                                                     String deviceName,
                                                     String deviceOs,
                                                     Integer deviceStatus,
                                                     Boolean trustedFlag,
                                                     String language,
                                                     OffsetDateTime startTime,
                                                     OffsetDateTime endTime) {
        List<Device> allDevices = deviceRepository.findAll();

        return allDevices.stream()
                .map(this::toDeviceSearchResult)
                .filter(doc -> {
                    if (deviceId != null && !deviceId.isEmpty() && !deviceId.equals(doc.deviceId())) return false;
                    if (userId != null && !userId.isEmpty() && !userId.equals(doc.userId())) return false;
                    if (clientType != null && !clientType.isEmpty() && !clientType.equals(doc.clientType())) return false;
                    if (clientId != null && !clientId.isEmpty() && !clientId.equals(doc.clientId())) return false;
                    if (deviceName != null && !deviceName.isEmpty() && (doc.deviceName() == null || !doc.deviceName().contains(deviceName))) return false;
                    if (deviceOs != null && !deviceOs.isEmpty() && (doc.deviceOs() == null || !doc.deviceOs().contains(deviceOs))) return false;
                    if (deviceStatus != null && !deviceStatus.equals(doc.deviceStatus())) return false;
                    if (trustedFlag != null && !trustedFlag.equals(doc.trustedFlag() != null && doc.trustedFlag() == 1)) return false;
                    if (language != null && !language.isEmpty() && !language.equals(doc.language())) return false;
                    if (startTime != null && doc.createTime() != null && doc.createTime().isBefore(startTime)) return false;
                    if (endTime != null && doc.createTime() != null && doc.createTime().isAfter(endTime)) return false;
                    return true;
                })
                .collect(Collectors.toList());
    }

    public DeviceInfoDTO queryDevice(String deviceId) {
        Device device = deviceRepository.findByDeviceId(deviceId)
                .orElseThrow(() -> new BusinessException(CiamErrorCode.DEVICE_NOT_FOUND));
        return DeviceMapper.INSTANCE.toDto(device);
    }

    public List<DeviceInfoDTO> queryUserDevices(String userId) {
        return deviceRepository.findByUserId(userId).stream()
                .map(DeviceMapper.INSTANCE::toDto)
                .collect(Collectors.toList());
    }

    private DeviceSearchResult toDeviceSearchResult(Device device) {
        return new DeviceSearchResult(
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
        );
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
