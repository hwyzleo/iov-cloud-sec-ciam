package net.hwyz.iov.cloud.sec.ciam.service.adapter.web.controller.mpt;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.framework.common.bean.ApiResponse;
import net.hwyz.iov.cloud.framework.common.bean.PageResult;
import net.hwyz.iov.cloud.framework.web.controller.BaseController;
import net.hwyz.iov.cloud.sec.ciam.service.adapter.web.vo.DeviceVo;
import net.hwyz.iov.cloud.sec.ciam.service.application.service.DeviceQueryAppService;
import net.hwyz.iov.cloud.sec.ciam.service.application.assembler.DeviceMapper;
import net.hwyz.iov.cloud.sec.ciam.service.domain.query.DeviceQuery;
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

        DeviceQuery query = DeviceQuery.builder().deviceId(deviceId).userId(userId).clientType(clientType).clientId(clientId).deviceName(deviceName).deviceOs(deviceOs).deviceStatus(deviceStatus).trustedFlag(trustedFlag).language(language).startTime(startTime).endTime(endTime).build();
        startPage();
        List<DeviceQueryAppService.DeviceSearchResult> list = deviceQueryAppService.queryDeviceList(query);
        return ApiResponse.ok(getPageResult(list));
    }

    @GetMapping("/devices/detail")
    public ApiResponse<DeviceVo> getDeviceDetail(@RequestParam String deviceId) {
        return ApiResponse.ok(deviceMapper.toVo(deviceQueryAppService.queryDevice(deviceId)));
    }

    @GetMapping("/devices/user")
    public ApiResponse<List<DeviceVo>> getUserDevices(@RequestParam String userId) {
        List<DeviceVo> voList = deviceQueryAppService.queryUserDevices(userId).stream()
                .map(deviceMapper::toVo)
                .collect(Collectors.toList());
        return ApiResponse.ok(voList);
    }
}
