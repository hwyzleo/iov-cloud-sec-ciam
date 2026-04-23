package net.hwyz.iov.cloud.sec.ciam.service.application;
import net.hwyz.iov.cloud.sec.ciam.service.application.service.*;
import net.hwyz.iov.cloud.sec.ciam.service.domain.adapter.*;

import net.hwyz.iov.cloud.framework.common.exception.BusinessException;
import net.hwyz.iov.cloud.sec.ciam.service.application.dto.DeviceInfoDto;
import net.hwyz.iov.cloud.sec.ciam.service.domain.enums.DeviceStatus;
import net.hwyz.iov.cloud.sec.ciam.service.domain.model.Device;
import net.hwyz.iov.cloud.sec.ciam.service.domain.query.DeviceQuery;
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

            DeviceInfoDto result = service.queryDevice(DEVICE_ID);

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
            when(deviceRepository.search(any(DeviceQuery.class))).thenReturn(List.of(
                    stubDevice("DEV-001"),
                    stubDevice("DEV-002"),
                    stubDevice("DEV-003")));

            List<DeviceQueryAppService.DeviceSearchResult> result = service.queryDeviceList(DeviceQuery.builder().build());

            assertEquals(3, result.size());
        }

        @Test
        void filtersByDeviceId() {
            when(deviceRepository.search(argThat(q -> "DEV-001".equals(q.getDeviceId())))).thenReturn(List.of(
                    stubDevice("DEV-001")));

            List<DeviceQueryAppService.DeviceSearchResult> result = service.queryDeviceList(DeviceQuery.builder().deviceId("DEV-001").build());

            assertEquals(1, result.size());
            assertEquals("DEV-001", result.get(0).deviceId());
        }

        @Test
        void filtersByUserId() {
            when(deviceRepository.search(argThat(q -> USER_ID.equals(q.getUserId())))).thenReturn(List.of(
                    stubDevice("DEV-001"),
                    stubDevice("DEV-002")));

            List<DeviceQueryAppService.DeviceSearchResult> result = service.queryDeviceList(DeviceQuery.builder().userId(USER_ID).build());

            assertEquals(2, result.size());
        }

        @Test
        void filtersByClientType() {
            when(deviceRepository.search(argThat(q -> "MOBILE".equals(q.getClientType())))).thenReturn(List.of(
                    stubDevice("DEV-001")));

            List<DeviceQueryAppService.DeviceSearchResult> result = service.queryDeviceList(DeviceQuery.builder().clientType("MOBILE").build());

            assertEquals(1, result.size());
            assertEquals("MOBILE", result.get(0).clientType());
        }

        @Test
        void filtersByDeviceStatus() {
            when(deviceRepository.search(argThat(q -> DeviceStatus.ACTIVE.getCode() == q.getDeviceStatus()))).thenReturn(List.of(
                    stubDevice("DEV-001")));

            List<DeviceQueryAppService.DeviceSearchResult> result = service.queryDeviceList(DeviceQuery.builder().deviceStatus(DeviceStatus.ACTIVE.getCode()).build());

            assertEquals(1, result.size());
            assertEquals(DeviceStatus.ACTIVE.getCode(), result.get(0).deviceStatus());
        }

        @Test
        void filtersByTrustedFlag() {
            when(deviceRepository.search(argThat(q -> q.getTrustedFlag() != null && q.getTrustedFlag()))).thenReturn(List.of(
                    stubDevice("DEV-001")));

            List<DeviceQueryAppService.DeviceSearchResult> result = service.queryDeviceList(DeviceQuery.builder().trustedFlag(true).build());

            assertEquals(1, result.size());
            assertEquals(1, result.get(0).trustedFlag());
        }

        @Test
        void filtersByDeviceNameFuzzy() {
            when(deviceRepository.search(argThat(q -> "iPhone".equals(q.getDeviceName())))).thenReturn(List.of(
                    stubDevice("DEV-001")));

            List<DeviceQueryAppService.DeviceSearchResult> result = service.queryDeviceList(DeviceQuery.builder().deviceName("iPhone").build());

            assertEquals(1, result.size());
            assertTrue(result.get(0).deviceName().contains("iPhone"));
        }

        @Test
        void filtersByDeviceOsFuzzy() {
            when(deviceRepository.search(argThat(q -> "iOS".equals(q.getDeviceOs())))).thenReturn(List.of(
                    stubDevice("DEV-001")));

            List<DeviceQueryAppService.DeviceSearchResult> result = service.queryDeviceList(DeviceQuery.builder().deviceOs("iOS").build());

            assertEquals(1, result.size());
            assertTrue(result.get(0).deviceOs().contains("iOS"));
        }

        @Test
        void filtersByLanguage() {
            when(deviceRepository.search(argThat(q -> "zh-CN".equals(q.getLanguage())))).thenReturn(List.of(
                    stubDevice("DEV-001")));

            List<DeviceQueryAppService.DeviceSearchResult> result = service.queryDeviceList(DeviceQuery.builder().language("zh-CN").build());

            assertEquals(1, result.size());
            assertEquals("zh-CN", result.get(0).language());
        }

        @Test
        void returnsEmptyWhenNoDevices() {
            when(deviceRepository.search(any(DeviceQuery.class))).thenReturn(Collections.emptyList());

            List<DeviceQueryAppService.DeviceSearchResult> result = service.queryDeviceList(DeviceQuery.builder().build());

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

            List<DeviceInfoDto> result = service.queryUserDevices(USER_ID);

            assertEquals(2, result.size());
            assertEquals("client-001", result.get(0).getClientId());
        }

        @Test
        void returnsEmptyWhenUserHasNoDevices() {
            when(deviceRepository.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            List<DeviceInfoDto> result = service.queryUserDevices(USER_ID);

            assertTrue(result.isEmpty());
        }
    }
}
