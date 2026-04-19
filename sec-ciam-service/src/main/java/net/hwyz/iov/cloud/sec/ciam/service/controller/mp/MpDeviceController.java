package net.hwyz.iov.cloud.sec.ciam.service.controller.mp;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.framework.common.bean.ApiResponse;
import net.hwyz.iov.cloud.sec.ciam.service.application.DeviceQueryAppService;
import net.hwyz.iov.cloud.sec.ciam.service.domain.search.SearchResult;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.dataobject.CiamDeviceDo;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 运营后台管理控制器 — 设备查询。
 */
@RestController
@RequestMapping("/api/mp/device/v1")
@RequiredArgsConstructor
public class MpDeviceController {

    private final DeviceQueryAppService deviceQueryAppService;

    /**
     * 检索设备列表
     */
    @GetMapping("/devices")
    public ApiResponse<SearchResult<DeviceQueryAppService.DeviceSearchResult>> searchDevices(
            @RequestParam(required = false) String deviceId,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String clientType,
            @RequestParam(required = false) String clientId,
            @RequestParam(required = false) String deviceName,
            @RequestParam(required = false) String deviceOs,
            @RequestParam(required = false) Integer deviceStatus,
            @RequestParam(required = false) Boolean trustedFlag,
            @RequestParam(required = false) String language,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") OffsetDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") OffsetDateTime endTime,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {
        return ApiResponse.ok(deviceQueryAppService.queryDeviceList(
                deviceId, userId, clientType, clientId, deviceName, deviceOs,
                deviceStatus, trustedFlag, language, startTime, endTime, page, size));
    }

    /**
     * 查询设备详情
     */
    @GetMapping("/devices/detail")
    public ApiResponse<CiamDeviceDo> getDeviceDetail(@RequestParam String deviceId) {
        return ApiResponse.ok(deviceQueryAppService.queryDevice(deviceId));
    }

    /**
     * 查询用户的设备列表
     */
    @GetMapping("/devices/user")
    public ApiResponse<List<CiamDeviceDo>> getUserDevices(@RequestParam String userId) {
        return ApiResponse.ok(deviceQueryAppService.queryUserDevices(userId));
    }
}
