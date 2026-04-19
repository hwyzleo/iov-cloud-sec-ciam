package net.hwyz.iov.cloud.sec.ciam.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.exception.BusinessException;
import net.hwyz.iov.cloud.framework.common.util.DateTimeUtil;
import net.hwyz.iov.cloud.sec.ciam.common.exception.CiamErrorCode;
import net.hwyz.iov.cloud.sec.ciam.domain.repository.CiamDeviceRepository;
import net.hwyz.iov.cloud.sec.ciam.domain.search.SearchResult;
import net.hwyz.iov.cloud.sec.ciam.infrastructure.repository.dao.dataobject.CiamDeviceDo;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 运营后台设备查询应用服务 — 提供设备查询的检索能力。
 * <p>
 * 职责：
 * <ul>
 *   <li>设备列表检索</li>
 *   <li>设备详情查询</li>
 *   <li>用户设备列表查询</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceQueryAppService {

    private final CiamDeviceRepository deviceRepository;

    /**
     * 检索设备列表。
     *
     * @param deviceId     设备 ID（精确）
     * @param userId       用户 ID（精确）
     * @param clientType   客户端类型（精确）
     * @param clientId     客户端 ID（精确）
     * @param deviceName   设备名称（模糊）
     * @param deviceOs     设备系统（模糊）
     * @param deviceStatus 设备状态（精确）
     * @param trustedFlag  是否可信设备（精确）
     * @param language     语言（精确）
     * @param startTime    创建开始时间
     * @param endTime      创建结束时间
     * @param page         页码（从 0 开始）
     * @param size         每页大小
     * @return 设备检索结果
     */
    public SearchResult<DeviceSearchResult> queryDeviceList(String deviceId,
                                                            String userId,
                                                            String clientType,
                                                            String clientId,
                                                            String deviceName,
                                                            String deviceOs,
                                                            Integer deviceStatus,
                                                            Boolean trustedFlag,
                                                            String language,
                                                            OffsetDateTime startTime,
                                                            OffsetDateTime endTime,
                                                            int page,
                                                            int size) {
        log.info("检索设备列表：deviceId={}, userId={}, clientType={}, clientId={}, deviceName={}, deviceOs={}, deviceStatus={}, trustedFlag={}, language={}, startTime={}, endTime={}",
                deviceId, userId, clientType, clientId, deviceName, deviceOs, deviceStatus, trustedFlag, language, startTime, endTime);

        List<CiamDeviceDo> allDevices = deviceRepository.findAll();

        List<DeviceSearchResult> filteredList = allDevices.stream()
                .map(this::toDeviceSearchResult)
                .filter(doc -> {
                    if (deviceId != null && !deviceId.isEmpty() && !deviceId.equals(doc.deviceId())) {
                        return false;
                    }
                    if (userId != null && !userId.isEmpty() && !userId.equals(doc.userId())) {
                        return false;
                    }
                    if (clientType != null && !clientType.isEmpty() && !clientType.equals(doc.clientType())) {
                        return false;
                    }
                    if (clientId != null && !clientId.isEmpty() && !clientId.equals(doc.clientId())) {
                        return false;
                    }
                    if (deviceName != null && !deviceName.isEmpty()) {
                        String name = doc.deviceName();
                        if (name == null || !name.contains(deviceName)) {
                            return false;
                        }
                    }
                    if (deviceOs != null && !deviceOs.isEmpty()) {
                        String os = doc.deviceOs();
                        if (os == null || !os.contains(deviceOs)) {
                            return false;
                        }
                    }
                    if (deviceStatus != null && !deviceStatus.equals(doc.deviceStatus())) {
                        return false;
                    }
                    if (trustedFlag != null) {
                        boolean docTrustedFlag = doc.trustedFlag() != null && doc.trustedFlag() == 1;
                        if (!trustedFlag.equals(docTrustedFlag)) {
                            return false;
                        }
                    }
                    if (language != null && !language.isEmpty() && !language.equals(doc.language())) {
                        return false;
                    }
                    if (startTime != null || endTime != null) {
                        OffsetDateTime createTime = doc.createTime();
                        if (createTime == null) {
                            return false;
                        }
                        if (startTime != null && createTime.isBefore(startTime)) {
                            return false;
                        }
                        if (endTime != null && createTime.isAfter(endTime)) {
                            return false;
                        }
                    }
                    return true;
                })
                .collect(Collectors.toList());

        return paginate(filteredList, page, size);
    }

    /**
     * 查询设备详情。
     *
     * @param deviceId 设备业务唯一标识
     * @return 设备详情
     */
    public CiamDeviceDo queryDevice(String deviceId) {
        CiamDeviceDo device = deviceRepository.findByDeviceId(deviceId)
                .orElseThrow(() -> new BusinessException(CiamErrorCode.DEVICE_NOT_FOUND));
        log.info("查询设备详情：deviceId={}", deviceId);
        return device;
    }

    /**
     * 查询用户的设备列表。
     *
     * @param userId 用户业务唯一标识
     * @return 该用户的所有设备
     */
    public List<CiamDeviceDo> queryUserDevices(String userId) {
        log.info("查询用户设备列表：userId={}", userId);
        return deviceRepository.findByUserId(userId);
    }

    // ---- 内部方法 ----

    /**
     * 将 CiamDeviceDo 转换为 DeviceSearchResult。
     */
    private DeviceSearchResult toDeviceSearchResult(CiamDeviceDo device) {
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

    /**
     * 对内存列表做简单分页。
     */
    private <T> SearchResult<T> paginate(List<T> all, int page, int size) {
        int total = all.size();
        int fromIndex = Math.min(page * size, total);
        int toIndex = Math.min(fromIndex + size, total);
        List<T> pageItems = all.subList(fromIndex, toIndex);
        return SearchResult.<T>builder()
                .items(pageItems)
                .total(total)
                .page(page)
                .size(size)
                .build();
    }

    /**
     * 设备搜索结果记录。
     */
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
