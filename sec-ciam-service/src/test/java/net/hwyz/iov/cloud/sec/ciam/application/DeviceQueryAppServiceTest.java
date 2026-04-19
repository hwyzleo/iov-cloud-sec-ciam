package net.hwyz.iov.cloud.sec.ciam.application;

import net.hwyz.iov.cloud.framework.common.exception.BusinessException;
import net.hwyz.iov.cloud.sec.ciam.domain.enums.DeviceStatus;
import net.hwyz.iov.cloud.sec.ciam.domain.repository.CiamDeviceRepository;
import net.hwyz.iov.cloud.sec.ciam.domain.search.SearchResult;
import net.hwyz.iov.cloud.sec.ciam.infrastructure.repository.dao.dataobject.CiamDeviceDo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * DeviceQueryAppService 单元测试。
 * <p>
 * 仅 mock 底层仓储服务，与项目现有测试风格一致。
 */
class DeviceQueryAppServiceTest {

    private CiamDeviceRepository deviceRepository;

    private DeviceQueryAppService service;

    private static final String DEVICE_ID = "DEV-001";
    private static final String USER_ID = "user-device-001";

    @BeforeEach
    void setUp() {
        deviceRepository = mock(CiamDeviceRepository.class);
        service = new DeviceQueryAppService(deviceRepository);
    }

    // ---- helpers ----

    private CiamDeviceDo stubDevice(String deviceId) {
        CiamDeviceDo device = new CiamDeviceDo();
        device.setDeviceId(deviceId);
        device.setUserId(USER_ID);
        device.setClientType("MOBILE");
        device.setClientId("client-001");
        device.setDeviceName("iPhone 15");
        device.setDeviceOs("iOS 17.0");
        device.setAppVersion("1.0.0");
        device.setDeviceFingerprint("fp-001");
        device.setTrustedFlag(1);
        device.setDeviceStatus(DeviceStatus.ACTIVE.getCode());
        device.setLanguage("zh-CN");
        device.setFirstLoginTime(Instant.now());
        device.setLastLoginTime(Instant.now());
        device.setCreateTime(Instant.now());
        device.setDescription("Test Device");
        device.setRowValid(1);
        return device;
    }

    // ---- queryDevice ----

    @Nested
    class QueryDeviceTests {

        @Test
        void returnsDeviceWhenExists() {
            when(deviceRepository.findByDeviceId(DEVICE_ID)).thenReturn(Optional.of(stubDevice(DEVICE_ID)));

            CiamDeviceDo result = service.queryDevice(DEVICE_ID);

            assertNotNull(result);
            assertEquals(DEVICE_ID, result.getDeviceId());
            assertEquals(USER_ID, result.getUserId());
        }

        @Test
        void throwsWhenDeviceNotFound() {
            when(deviceRepository.findByDeviceId("nonexistent")).thenReturn(Optional.empty());

            assertThrows(BusinessException.class, () -> service.queryDevice("nonexistent"));
        }
    }

    // ---- queryDeviceList ----

    @Nested
    class QueryDeviceListTests {

        @Test
        void returnsAllDevicesWhenNoFilters() {
            when(deviceRepository.findAll()).thenReturn(List.of(
                    stubDevice("DEV-001"),
                    stubDevice("DEV-002"),
                    stubDevice("DEV-003")));

            SearchResult<DeviceQueryAppService.DeviceSearchResult> result = service.queryDeviceList(
                    null, null, null, null, null, null, null, null, null, null, null, 0, 10);

            assertEquals(3, result.getTotal());
            assertEquals(3, result.getItems().size());
        }

        @Test
        void filtersByDeviceId() {
            when(deviceRepository.findAll()).thenReturn(List.of(
                    stubDevice("DEV-001"),
                    stubDevice("DEV-002")));

            SearchResult<DeviceQueryAppService.DeviceSearchResult> result = service.queryDeviceList(
                    "DEV-001", null, null, null, null, null, null, null, null, null, null, 0, 10);

            assertEquals(1, result.getTotal());
            assertEquals("DEV-001", result.getItems().get(0).deviceId());
        }

        @Test
        void filtersByUserId() {
            when(deviceRepository.findAll()).thenReturn(List.of(
                    stubDevice("DEV-001"),
                    stubDevice("DEV-002")));

            SearchResult<DeviceQueryAppService.DeviceSearchResult> result = service.queryDeviceList(
                    null, "user-device-001", null, null, null, null, null, null, null, null, null, 0, 10);

            assertEquals(2, result.getTotal());
        }

        @Test
        void filtersByClientType() {
            CiamDeviceDo device1 = stubDevice("DEV-001");
            device1.setClientType("MOBILE");
            CiamDeviceDo device2 = stubDevice("DEV-002");
            device2.setClientType("WEB");
            when(deviceRepository.findAll()).thenReturn(List.of(device1, device2));

            SearchResult<DeviceQueryAppService.DeviceSearchResult> result = service.queryDeviceList(
                    null, null, "MOBILE", null, null, null, null, null, null, null, null, 0, 10);

            assertEquals(1, result.getTotal());
            assertEquals("MOBILE", result.getItems().get(0).clientType());
        }

        @Test
        void filtersByDeviceStatus() {
            CiamDeviceDo device1 = stubDevice("DEV-001");
            device1.setDeviceStatus(DeviceStatus.ACTIVE.getCode());
            CiamDeviceDo device2 = stubDevice("DEV-002");
            device2.setDeviceStatus(DeviceStatus.INVALID.getCode());
            when(deviceRepository.findAll()).thenReturn(List.of(device1, device2));

            SearchResult<DeviceQueryAppService.DeviceSearchResult> result = service.queryDeviceList(
                    null, null, null, null, null, null, DeviceStatus.ACTIVE.getCode(), null, null, null, null, 0, 10);

            assertEquals(1, result.getTotal());
            assertEquals(DeviceStatus.ACTIVE.getCode(), result.getItems().get(0).deviceStatus());
        }

        @Test
        void filtersByTrustedFlag() {
            CiamDeviceDo device1 = stubDevice("DEV-001");
            device1.setTrustedFlag(1);
            CiamDeviceDo device2 = stubDevice("DEV-002");
            device2.setTrustedFlag(0);
            when(deviceRepository.findAll()).thenReturn(List.of(device1, device2));

            SearchResult<DeviceQueryAppService.DeviceSearchResult> result = service.queryDeviceList(
                    null, null, null, null, null, null, null, true, null, null, null, 0, 10);

            assertEquals(1, result.getTotal());
            assertEquals(1, result.getItems().get(0).trustedFlag());
        }

        @Test
        void filtersByDeviceNameFuzzy() {
            CiamDeviceDo device1 = stubDevice("DEV-001");
            device1.setDeviceName("iPhone 15");
            CiamDeviceDo device2 = stubDevice("DEV-002");
            device2.setDeviceName("Samsung Galaxy");
            when(deviceRepository.findAll()).thenReturn(List.of(device1, device2));

            SearchResult<DeviceQueryAppService.DeviceSearchResult> result = service.queryDeviceList(
                    null, null, null, null, "iPhone", null, null, null, null, null, null, 0, 10);

            assertEquals(1, result.getTotal());
            assertTrue(result.getItems().get(0).deviceName().contains("iPhone"));
        }

        @Test
        void filtersByDeviceOsFuzzy() {
            CiamDeviceDo device1 = stubDevice("DEV-001");
            device1.setDeviceOs("iOS 17.0");
            CiamDeviceDo device2 = stubDevice("DEV-002");
            device2.setDeviceOs("Android 14");
            when(deviceRepository.findAll()).thenReturn(List.of(device1, device2));

            SearchResult<DeviceQueryAppService.DeviceSearchResult> result = service.queryDeviceList(
                    null, null, null, null, null, "iOS", null, null, null, null, null, 0, 10);

            assertEquals(1, result.getTotal());
            assertTrue(result.getItems().get(0).deviceOs().contains("iOS"));
        }

        @Test
        void filtersByLanguage() {
            CiamDeviceDo device1 = stubDevice("DEV-001");
            device1.setLanguage("zh-CN");
            CiamDeviceDo device2 = stubDevice("DEV-002");
            device2.setLanguage("en-US");
            when(deviceRepository.findAll()).thenReturn(List.of(device1, device2));

            SearchResult<DeviceQueryAppService.DeviceSearchResult> result = service.queryDeviceList(
                    null, null, null, null, null, null, null, null, "zh-CN", null, null, 0, 10);

            assertEquals(1, result.getTotal());
            assertEquals("zh-CN", result.getItems().get(0).language());
        }

        @Test
        void appliesPagination() {
            when(deviceRepository.findAll()).thenReturn(List.of(
                    stubDevice("DEV-001"),
                    stubDevice("DEV-002"),
                    stubDevice("DEV-003"),
                    stubDevice("DEV-004"),
                    stubDevice("DEV-005")));

            SearchResult<DeviceQueryAppService.DeviceSearchResult> result = service.queryDeviceList(
                    null, null, null, null, null, null, null, null, null, null, null, 1, 2);

            assertEquals(5, result.getTotal());
            assertEquals(2, result.getItems().size());
            assertEquals(1, result.getPage());
            assertEquals(2, result.getSize());
        }

        @Test
        void returnsEmptyWhenNoDevices() {
            when(deviceRepository.findAll()).thenReturn(Collections.emptyList());

            SearchResult<DeviceQueryAppService.DeviceSearchResult> result = service.queryDeviceList(
                    null, null, null, null, null, null, null, null, null, null, null, 0, 10);

            assertEquals(0, result.getTotal());
            assertTrue(result.getItems().isEmpty());
        }
    }

    // ---- queryUserDevices ----

    @Nested
    class QueryUserDevicesTests {

        @Test
        void returnsDevicesForUser() {
            when(deviceRepository.findByUserId(USER_ID)).thenReturn(List.of(
                    stubDevice("DEV-001"),
                    stubDevice("DEV-002")));

            List<CiamDeviceDo> result = service.queryUserDevices(USER_ID);

            assertEquals(2, result.size());
            assertEquals(USER_ID, result.get(0).getUserId());
            assertEquals(USER_ID, result.get(1).getUserId());
        }

        @Test
        void returnsEmptyWhenUserHasNoDevices() {
            when(deviceRepository.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            List<CiamDeviceDo> result = service.queryUserDevices(USER_ID);

            assertTrue(result.isEmpty());
        }
    }
}
