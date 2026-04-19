package net.hwyz.iov.cloud.sec.ciam.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.util.DateTimeUtil;
import net.hwyz.iov.cloud.sec.ciam.controller.mobile.dto.DeviceInfo;
import net.hwyz.iov.cloud.sec.ciam.domain.enums.DeviceStatus;
import net.hwyz.iov.cloud.sec.ciam.domain.repository.CiamDeviceRepository;
import net.hwyz.iov.cloud.sec.ciam.infrastructure.repository.dao.dataobject.CiamDeviceDo;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 设备领域服务 — 负责设备的注册、更新与识别逻辑。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceDomainService {

    private final CiamDeviceRepository deviceRepository;

    /**
     * 记录或更新设备信息。
     *
     * @param userId     用户业务唯一标识
     * @param deviceId   设备标识
     * @param deviceInfo 设备信息对象
     * @return 设备业务唯一标识
     */
    public String recordDevice(String userId, String deviceId, DeviceInfo deviceInfo) {
        if (deviceInfo == null || (deviceInfo.getDeviceId() == null && deviceInfo.getDeviceFingerprint() == null)) {
            log.warn("设备信息为空或缺少唯一标识，跳过记录: userId={}", userId);
            return null;
        }

        if (deviceId == null || deviceId.isBlank()) {
            // 如果没传 deviceId，根据指纹或其它信息生成
            deviceId = "DEV_" + Math.abs((deviceInfo.getDeviceFingerprint() != null ? 
                deviceInfo.getDeviceFingerprint() : deviceInfo.toString()).hashCode());
        }
        
        Optional<CiamDeviceDo> deviceOpt = deviceRepository.findByDeviceId(deviceId);
        if (deviceOpt.isPresent()) {
            CiamDeviceDo device = deviceOpt.get();
            // 更新最后登录时间及可能变化的信息
            device.setLastLoginTime(DateTimeUtil.getNowInstant());
            device.setModifyTime(DateTimeUtil.getNowInstant());
            if (deviceInfo.getAppVersion() != null) {
                device.setAppVersion(deviceInfo.getAppVersion());
            }
            if (deviceInfo.getLanguage() != null) {
                device.setLanguage(deviceInfo.getLanguage());
            }
            deviceRepository.updateByDeviceId(device);
            log.info("更新设备登录信息: deviceId={}, userId={}", deviceId, userId);
        } else {
            CiamDeviceDo device = new CiamDeviceDo();
            device.setDeviceId(deviceId);
            device.setUserId(userId);
            device.setClientType(deviceInfo.getClientType());
            device.setClientId(deviceInfo.getClientId());
            device.setDeviceName(deviceInfo.getDeviceName());
            device.setDeviceOs(deviceInfo.getDeviceOs());
            device.setAppVersion(deviceInfo.getAppVersion());
            device.setDeviceFingerprint(deviceInfo.getDeviceFingerprint());
            device.setDeviceStatus(DeviceStatus.ACTIVE.getCode());
            device.setFirstLoginTime(DateTimeUtil.getNowInstant());
            device.setLastLoginTime(DateTimeUtil.getNowInstant());
            device.setCreateTime(DateTimeUtil.getNowInstant());
            device.setModifyTime(DateTimeUtil.getNowInstant());
            device.setRowValid(1);
            device.setRowVersion(1);
            deviceRepository.insert(device);
            log.info("记录新设备: deviceId={}, userId={}", deviceId, userId);
        }

        return deviceId;
    }

    /**
     * 切换设备语言。
     *
     * @param userId   用户业务唯一标识
     * @param deviceId 设备标识
     * @param language 语言代码，如 zh-CN, en-US
     */
    public void changeLanguage(String userId, String deviceId, String language) {
        Optional<CiamDeviceDo> deviceOpt = deviceRepository.findByDeviceId(deviceId);
        if (deviceOpt.isPresent()) {
            CiamDeviceDo device = deviceOpt.get();
            device.setLanguage(language);
            device.setModifyTime(DateTimeUtil.getNowInstant());
            deviceRepository.updateByDeviceId(device);
            log.info("切换设备语言：deviceId={}, userId={}, language={}", deviceId, userId, language);
        } else {
            log.warn("设备不存在，无法切换语言：deviceId={}, userId={}", deviceId, userId);
        }
    }
}
