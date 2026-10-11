package com.smartnav;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartnav.dto.RouteCalculationRequest;
import com.smartnav.dto.RouteCalculationResponse;
import com.smartnav.entity.Report;
import com.smartnav.entity.User;
import com.smartnav.repository.ReportRepository;
import com.smartnav.repository.RouteRecordRepository;
import com.smartnav.service.NavigationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class NavigationServiceTest {

    private ReportRepository reportRepository;
    private RouteRecordRepository routeRecordRepository;
    private NavigationService navigationService;

    @BeforeEach
    void setUp() {
        reportRepository = Mockito.mock(ReportRepository.class);
        routeRecordRepository = Mockito.mock(RouteRecordRepository.class);
        navigationService = new NavigationService(reportRepository, routeRecordRepository, new ObjectMapper());
    }

    @Test
    void testCalculateRouteWithoutObstacles() {
        when(reportRepository.findApprovedObstacles()).thenReturn(List.of());
        when(reportRepository.findApprovedCongestions()).thenReturn(List.of());

        RouteCalculationRequest req = new RouteCalculationRequest();
        req.setSourceLat(18.5289);
        req.setSourceLng(73.8744);
        req.setDestLat(18.5913);
        req.setDestLng(73.7389);
        req.setAvoidBlockedRoads(true);

        RouteCalculationResponse res = navigationService.calculateIntelligentRoute(req, null);

        assertNotNull(res);
        assertTrue(res.getTotalDistanceKm() > 0);
        assertTrue(res.getEstimatedDurationMins() > 0);
        assertFalse(res.getIsCongested());
        assertFalse(res.getHasBlockedAvoidance());
        assertFalse(res.getCoordinates().isEmpty());
    }

    @Test
    void testCalculateRouteWithActiveObstacleDetour() {
        User user = new User("Test User", "test@smartnav.com", "pass", "USER", null);
        Report obstacle = new Report(user, "ROAD_BLOCK", "Wakad Hinjewadi Block", null, 18.5920, 73.7580, "Wakad Flyover & Hinjawadi Rd", "CRITICAL");
        obstacle.setStatus("APPROVED");

        when(reportRepository.findApprovedObstacles()).thenReturn(List.of(obstacle));
        when(reportRepository.findApprovedCongestions()).thenReturn(List.of());

        RouteCalculationRequest req = new RouteCalculationRequest();
        req.setSourceLat(18.5289);
        req.setSourceLng(73.8744);
        req.setDestLat(18.5913);
        req.setDestLng(73.7389);
        req.setAvoidBlockedRoads(true);

        RouteCalculationResponse res = navigationService.calculateIntelligentRoute(req, null);

        assertNotNull(res);
        assertTrue(res.getHasBlockedAvoidance(), "Should detect and avoid obstacle");
        assertEquals(1, res.getBlockedIncidentsAvoided().size());
    }

    @Test
    void testCalculateRouteWithCongestionPenalty() {
        User user = new User("Test User", "test@smartnav.com", "pass", "USER", null);
        Report jam = new Report(user, "TRAFFIC_JAM", "Shivajinagar Bottleneck", null, 18.5308, 73.8475, "Shivajinagar Chowk", "HIGH");
        jam.setStatus("APPROVED");

        when(reportRepository.findApprovedObstacles()).thenReturn(List.of());
        when(reportRepository.findApprovedCongestions()).thenReturn(List.of(jam));

        RouteCalculationRequest req = new RouteCalculationRequest();
        req.setSourceLat(18.5289);
        req.setSourceLng(73.8744);
        req.setDestLat(18.5913);
        req.setDestLng(73.7389);

        RouteCalculationResponse res = navigationService.calculateIntelligentRoute(req, null);

        assertNotNull(res);
        assertTrue(res.getIsCongested(), "Should flag congestion");
        assertTrue(res.getEstimatedDurationMins() >= res.getOriginalDurationMins(), "Duration should increase with penalty");
        assertTrue(res.getDelayMins() > 0);
    }
}
