package com.smartnav.controller;

import com.smartnav.dto.ApiResponse;
import com.smartnav.dto.RouteCalculationRequest;
import com.smartnav.dto.RouteCalculationResponse;
import com.smartnav.entity.RouteRecord;
import com.smartnav.entity.User;
import com.smartnav.security.SecurityUtils;
import com.smartnav.service.NavigationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/routes")
public class RouteRestController {

    private final NavigationService navigationService;
    private final SecurityUtils securityUtils;

    public RouteRestController(NavigationService navigationService, SecurityUtils securityUtils) {
        this.navigationService = navigationService;
        this.securityUtils = securityUtils;
    }

    @PostMapping("/calculate")
    public ResponseEntity<ApiResponse<RouteCalculationResponse>> calculateRoute(
            @Valid @RequestBody RouteCalculationRequest request) {
        User user = securityUtils.getCurrentUser().orElse(null);
        RouteCalculationResponse result = navigationService.calculateIntelligentRoute(request, user);
        return ResponseEntity.ok(ApiResponse.ok("Route calculated successfully", result));
    }

    @PostMapping("/save")
    public ResponseEntity<ApiResponse<RouteRecord>> saveRoute(
            @Valid @RequestBody RouteCalculationRequest request) {
        User user = securityUtils.getCurrentUser().orElse(null);
        RouteCalculationResponse calculated = navigationService.calculateIntelligentRoute(request, user);
        RouteRecord saved = navigationService.saveRoute(request, calculated, user);
        return ResponseEntity.ok(ApiResponse.ok("Route saved successfully", saved));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<RouteRecord>>> getUserRoutes() {
        User user = securityUtils.getCurrentUser().orElse(null);
        if (user == null) {
            return ResponseEntity.ok(ApiResponse.ok(List.of()));
        }
        List<RouteRecord> routes = navigationService.getUserRoutes(user.getId());
        return ResponseEntity.ok(ApiResponse.ok(routes));
    }
}
