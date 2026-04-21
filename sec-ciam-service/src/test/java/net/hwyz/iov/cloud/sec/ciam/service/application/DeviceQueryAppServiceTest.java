package net.hwyz.iov.cloud.sec.ciam.service.application;

import net.hwyz.iov.cloud.framework.common.exception.BusinessException;
import net.hwyz.iov.cloud.sec.ciam.service.application.dto.DeviceInfoDTO;
import net.hwyz.iov.cloud.sec.ciam.service.domain.enums.DeviceStatus;
import net.hwyz.iov.cloud.sec.ciam.service.domain.model.Device;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.CiamDeviceRepository;
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

    private Device stubDevice(String deviceId) {
        Device device = new Device();
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
        return device;
    }

    // ---- queryDevice ----

    @Nested
    class QueryDeviceTests {

        @Test
        void returnsDeviceWhenExists() {
            when(deviceRepository.findByDeviceId(DEVICE_ID)).thenReturn(Optional.of(stubDevice(DEVICE_ID)));

            DeviceInfoDTO result = service.queryDevice(DEVICE_ID);

            assertNotNull(result);
            assertEquals(DEVICE_ID, result.getDeviceId());
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

            List<DeviceQueryAppService.DeviceSearchResult> result = service.queryDeviceList(
                    null, null, null, null, null, null, null, null, null, null, null);

            assertEquals(3, result.size());
        }

        @Test
        void filtersByDeviceId() {
            when(deviceRepository.findAll()).thenReturn(List.of(
                    stubDevice("DEV-001"),
                    stubDevice("DEV-002")));

            List<DeviceQueryAppService.DeviceSearchResult> result = service.queryDeviceList(
                    "DEV-001", null, null, null, null, null, null, null, null, null, null);

            assertEquals(1, result.size());
            assertEquals("DEV-001", result.get(0).deviceId());
        }

        @Test
        void filtersByUserId() {
            when(deviceRepository.findAll()).thenReturn(List.of(
                    stubDevice("DEV-001"),
                    stubDevice("DEV-002")));

            List<DeviceQueryAppService.DeviceSearchResult> result = service.queryDeviceList(
                    null, "user-device-001", null, null, null, null, null, null, null, null, null);

            assertEquals(2, result.size());
        }

        @Test
        void filtersByClientType() {
            Device device1 = stubDevice("DEV-001");
            device1.setClientType("MOBILE");
            Device device2 = stubDevice("DEV-002");
            device2.setClientType("WEB");
            when(deviceRepository.findAll()).thenReturn(List.of(device1, device2));

            List<DeviceQueryAppService.DeviceSearchResult> result = service.queryDeviceList(
                    null, null, "MOBILE", null, null, null, null, null, null, null, null);

            assertEquals(1, result.size());
            assertEquals("MOBILE", result.get(0).clientType());
        }

        @Test
        void filtersByDeviceStatus() {
            Device device1 = stubDevice("DEV-001");
            device1.setDeviceStatus(DeviceStatus.ACTIVE.getCode());
            Device device2 = stubDevice("DEV-002");
            device2.setDeviceStatus(DeviceStatus.INVALID.getCode());
            when(deviceRepository.findAll()).thenReturn(List.of(device1, device2));

            List<DeviceQueryAppService.DeviceSearchResult> result = service.queryDeviceList(
                    null, null, null, null, null, null, DeviceStatus.ACTIVE.getCode(), null, null, null, null);

            assertEquals(1, result.size());
            assertEquals(DeviceStatus.ACTIVE.getCode(), result.get(0).deviceStatus());
        }

        @Test
        void filtersByTrustedFlag() {
            Device device1 = stubDevice("DEV-001");
            device1.setTrustedFlag(1);
            Device device2 = stubDevice("DEV-002");
            device2.setTrustedFlag(0);
            when(deviceRepository.findAll()).thenReturn(List.of(device1, device2));

            List<DeviceQueryAppService.DeviceSearchResult> result = service.queryDeviceList(
                    null, null, null, null, null, null, null, true, null, null, null);

            assertEquals(1, result.size());
            assertEquals(1, result.get(0).trustedFlag());
        }

        @Test
        void filtersByDeviceNameFuzzy() {
            Device device1 = stubDevice("DEV-001");
            device1.setDeviceName("iPhone 15");
            Device device2 = stubDevice("DEV-002");
            device2.setDeviceName("Samsung Galaxy");
            when(deviceRepository.findAll()).thenReturn(List.of(device1, device2));

            List<DeviceQueryAppService.DeviceSearchResult> result = service.queryDeviceList(
                    null, null, null, null, "iPhone", null, null, null, null, null, null);

            assertEquals(1, result.size());
            assertTrue(result.get(0).deviceName().contains("iPhone"));
        }

        @Test
        void filtersByDeviceOsFuzzy() {
            Device device1 = stubDevice("DEV-001");
            device1.setDeviceOs("iOS 17.0");
            Device device2 = stubDevice("DEV-002");
            device2.setDeviceOs("Android 14");
            when(deviceRepository.findAll()).thenReturn(List.of(device1, device2));

            List<DeviceQueryAppService.DeviceSearchResult> result = service.queryDeviceList(
                    null, null, null, null, null, "iOS", null, null, null, null, null);

            assertEquals(1, result.size());
            assertTrue(result.get(0).deviceOs().contains("iOS"));
        }

        @Test
        void filtersByLanguage() {
            Device device1 = stubDevice("DEV-001");
            device1.setLanguage("zh-CN");
            Device device2 = stubDevice("DEV-002");
            device2.setLanguage("en-US");
            when(deviceRepository.findAll()).thenReturn(List.of(device1, device2));

            List<DeviceQueryAppService.DeviceSearchResult> result = service.queryDeviceList(
                    null, null, null, null, null, null, null, null, "zh-CN", null, null);

            assertEquals(1, result.size());
            assertEquals("zh-CN", result.get(0).language());
        }

        @Test
        void returnsEmptyWhenNoDevices() {
            when(deviceRepository.findAll()).thenReturn(Collections.emptyList());

            List<DeviceQueryAppService.DeviceSearchResult> result = service.queryDeviceList(
                    null, null, null, null, null, null, null, null, null, null, null);

            assertTrue(result.isEmpty());
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

            List<DeviceInfoDTO> result = service.queryUserDevices(USER_ID);

            assertEquals(2, result.size());
            assertEquals("client-001", result.get(0).getClientId());
        }

        @Test
        void returnsEmptyWhenUserHasNoDevices() {
            when(deviceRepository.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            List<DeviceInfoDTO> result = service.queryUserDevices(USER_ID);

            assertTrue(result.isEmpty());
        }
    }
}
