package net.hwyz.iov.cloud.sec.ciam.service.domain.service;

import net.hwyz.iov.cloud.sec.ciam.service.domain.enums.DecisionResult;
import net.hwyz.iov.cloud.sec.ciam.service.domain.enums.RiskLevel;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.CiamRiskEventRepository;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.dataobject.CiamRiskEventDo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RiskAssessmentServiceTest {

    private CiamRiskEventRepository riskEventRepository;
    private RiskAssessmentService service;

    @BeforeEach
    void setUp() {
        riskEventRepository = mock(CiamRiskEventRepository.class);
        when(riskEventRepository.insert(any())).thenReturn(1);
        service = new RiskAssessmentService(riskEventRepository);
    }

    @Test
    void normalLogin_lowRiskAllow() {
        RiskAssessmentResult result = service.assessLoginRisk(
                "user-001", "device-001", "1.2.3.4", "CN", "app", false, false);

        assertEquals(RiskLevel.LOW, result.getRiskLevel());
        assertEquals(DecisionResult.ALLOW, result.getDecisionResult());
        assertTrue(result.getHitRules().isEmpty());
        assertNotNull(result.getRiskEventId());
        verify(riskEventRepository).insert(any(CiamRiskEventDo.class));
    }

    @Test
    void newDevice_mediumRiskChallenge() {
        RiskAssessmentResult result = service.assessLoginRisk(
                "user-001", "device-new", "1.2.3.4", "CN", "app", true, false);

        assertEquals(RiskLevel.MEDIUM, result.getRiskLevel());
        assertEquals(DecisionResult.CHALLENGE, result.getDecisionResult());
        assertTrue(result.getHitRules().contains("new_device"));
    }

    @Test
    void geoChange_mediumRiskChallenge() {
        RiskAssessmentResult result = service.assessLoginRisk(
                "user-001", "device-001", "5.6.7.8", "US", "app", false, true);

        assertEquals(RiskLevel.MEDIUM, result.getRiskLevel());
        assertEquals(DecisionResult.CHALLENGE, result.getDecisionResult());
        assertTrue(result.getHitRules().contains("geo_change"));
    }

    @Test
    void newDeviceAndGeoChange_highRiskBlock() {
        RiskAssessmentResult result = service.assessLoginRisk(
                "user-001", "device-new", "5.6.7.8", "US", "app", true, true);

        assertEquals(RiskLevel.HIGH, result.getRiskLevel());
        assertEquals(DecisionResult.BLOCK, result.getDecisionResult());
        assertTrue(result.getHitRules().contains("new_device_and_geo_change"));
    }

    @Test
    void persistsRiskEvent() {
        service.assessLoginRisk("user-001", "device-001", "1.2.3.4", "CN", "web", true, false);

        ArgumentCaptor<CiamRiskEventDo> captor = ArgumentCaptor.forClass(CiamRiskEventDo.class);
        verify(riskEventRepository).insert(captor.capture());
        CiamRiskEventDo saved = captor.getValue();

        assertEquals("user-001", saved.getUserId());
        assertEquals("device-001", saved.getDeviceId());
        assertEquals("1.2.3.4", saved.getIpAddress());
        assertEquals("CN", saved.getRegionCode());
        assertEquals("web", saved.getClientType());
        assertEquals("login", saved.getRiskScene());
        assertEquals(RiskLevel.MEDIUM.getCode(), saved.getRiskLevel());
        assertEquals(DecisionResult.CHALLENGE.getCode(), saved.getDecisionResult());
        assertNotNull(saved.getEventTime());
        assertEquals(0, saved.getHandledFlag());
    }
}
