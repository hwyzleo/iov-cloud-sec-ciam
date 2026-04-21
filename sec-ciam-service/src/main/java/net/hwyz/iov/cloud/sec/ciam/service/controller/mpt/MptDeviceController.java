package net.hwyz.iov.cloud.sec.ciam.service.controller.mpt;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.framework.common.bean.ApiResponse;
import net.hwyz.iov.cloud.framework.common.bean.PageResult;
import net.hwyz.iov.cloud.framework.web.controller.BaseController;
import net.hwyz.iov.cloud.sec.ciam.service.controller.vo.DeviceVO;
import net.hwyz.iov.cloud.sec.ciam.service.application.DeviceQueryAppService;
import net.hwyz.iov.cloud.sec.ciam.service.application.mapper.DeviceMapper;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 运营后台管理控制器 — 设备查询。
 */
@RestController
@RequestMapping("/api/mpt/device/v1")
@RequiredArgsConstructor
public class MptDeviceController extends BaseController {

    private final DeviceQueryAppService deviceQueryAppService;
    
    private final DeviceMapper deviceMapper = DeviceMapper.INSTANCE;

    /**
     * 检索设备列表
     */
    @GetMapping("/devices")
    public ApiResponse<PageResult<DeviceQueryAppService.DeviceSearchResult>> searchDevices(
            @RequestParam(required = false) String deviceId,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String clientType,
            @RequestParam(required = false) String clientId,
            @RequestParam(required = false) String deviceName,
            @RequestParam(required = false) String deviceOs,
            @RequestParam(required = false) Integer deviceStatus,
            @RequestParam(required = false) Boolean trustedFlag,
            @RequestParam(required = false) String language,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endTime) {
        startPage();
        List<DeviceQueryAppService.DeviceSearchResult> list = deviceQueryAppService.queryDeviceList(
                deviceId, userId, clientType, clientId, deviceName, deviceOs,
                deviceStatus, trustedFlag, language, startTime, endTime);
        return ApiResponse.ok(getPageResult(list));
    }

    /**
     * 查询设备详情
     */
    @GetMapping("/devices/detail")
    public ApiResponse<DeviceVO> getDeviceDetail(@RequestParam String deviceId) {
        return ApiResponse.ok(deviceMapper.toVo(deviceQueryAppService.queryDevice(deviceId)));
    }

    /**
     * 查询用户的设备列表
     */
    @GetMapping("/devices/user")
    public ApiResponse<List<DeviceVO>> getUserDevices(@RequestParam String userId) {
        List<DeviceVO> voList = deviceQueryAppService.queryUserDevices(userId).stream()
            .map(deviceMapper::toVo)
            .collect(Collectors.toList());
        return ApiResponse.ok(voList);
    }
}
