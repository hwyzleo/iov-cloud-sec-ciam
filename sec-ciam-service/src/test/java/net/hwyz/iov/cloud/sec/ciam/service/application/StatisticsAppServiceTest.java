package net.hwyz.iov.cloud.sec.ciam.service.application;
import net.hwyz.iov.cloud.sec.ciam.service.application.service.*;

import net.hwyz.iov.cloud.sec.ciam.service.application.dto.StatisticsResultDto2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

/**
 * StatisticsAppService 单元测试。
 * <p>
 * 当前为桩实现验证，确保各接口返回合法的空结果且不抛异常。
 */
class StatisticsAppServiceTest {

    private StatisticsAppService service;

    private static final LocalDateTime START = LocalDateTime.of(2026, 1, 1, 0, 0, 0);
    private static final LocalDateTime END = LocalDateTime.of(2026, 12, 31, 23, 59, 0);

    @BeforeEach
    void setUp() {
        service = new StatisticsAppService();
    }

    // ---- getRegistrationStats ----

    @Nested
    class GetRegistrationStatsTests {

        @Test
        void returnsEmptyResultWithTimeRangeAndChannel() {
            StatisticsResultDto2 result = service.getRegistrationStats(START, END, "app_store");

            assertEquals(0, result.getRegistrationCount());
            assertEquals(0.0, result.getConversionRate());
            assertNotNull(result.getChannelDistribution());
            assertTrue(result.getChannelDistribution().isEmpty());
        }

        @Test
        void supportsNullFilters() {
            StatisticsResultDto2 result = service.getRegistrationStats(null, null, null);

            assertEquals(0, result.getRegistrationCount());
            assertEquals(0.0, result.getConversionRate());
        }
    }

    // ---- getLoginStats ----

    @Nested
    class GetLoginStatsTests {

        @Test
        void returnsEmptyResultWithTimeRangeAndClientType() {
            StatisticsResultDto2 result = service.getLoginStats(START, END, "app");

            assertEquals(0, result.getLoginSuccessCount());
            assertEquals(0, result.getLoginFailureCount());
            assertEquals(0.0, result.getLoginSuccessRate());
        }

        @Test
        void supportsNullFilters() {
            StatisticsResultDto2 result = service.getLoginStats(null, null, null);

            assertEquals(0, result.getLoginSuccessCount());
            assertEquals(0, result.getLoginFailureCount());
            assertEquals(0.0, result.getLoginSuccessRate());
        }
    }

    // ---- getChannelDistribution ----

    @Nested
    class GetChannelDistributionTests {

        @Test
        void returnsEmptyDistribution() {
            StatisticsResultDto2 result = service.getChannelDistribution(START, END);

            assertNotNull(result.getChannelDistribution());
            assertTrue(result.getChannelDistribution().isEmpty());
        }

        @Test
        void supportsNullTimeRange() {
            StatisticsResultDto2 result = service.getChannelDistribution(null, null);

            assertNotNull(result.getChannelDistribution());
            assertEquals(Collections.emptyMap(), result.getChannelDistribution());
        }
    }

    // ---- getThirdPartyLoginDistribution ----

    @Nested
    class GetThirdPartyLoginDistributionTests {

        @Test
        void returnsEmptyDistribution() {
            StatisticsResultDto2 result = service.getThirdPartyLoginDistribution(START, END);

            assertNotNull(result.getThirdPartyDistribution());
            assertTrue(result.getThirdPartyDistribution().isEmpty());
        }

        @Test
        void supportsNullTimeRange() {
            StatisticsResultDto2 result = service.getThirdPartyLoginDistribution(null, null);

            assertNotNull(result.getThirdPartyDistribution());
            assertEquals(Collections.emptyMap(), result.getThirdPartyDistribution());
        }
    }

    // ---- StatisticsResult.empty() ----

    @Nested
    class StatisticsResultEmptyTests {

        @Test
        void emptyResultHasZeroValues() {
            StatisticsResultDto2 empty = StatisticsResultDto2.empty();

            assertEquals(0, empty.getRegistrationCount());
            assertEquals(0.0, empty.getConversionRate());
            assertEquals(0, empty.getLoginSuccessCount());
            assertEquals(0, empty.getLoginFailureCount());
            assertEquals(0.0, empty.getLoginSuccessRate());
            assertNotNull(empty.getChannelDistribution());
            assertTrue(empty.getChannelDistribution().isEmpty());
            assertNotNull(empty.getThirdPartyDistribution());
            assertTrue(empty.getThirdPartyDistribution().isEmpty());
        }
    }
}
