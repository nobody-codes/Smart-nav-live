package com.smartnav.controller;

import com.smartnav.dto.ApiResponse;
import com.smartnav.dto.LocationSearchResult;
import com.smartnav.service.LocationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/location")
public class LocationRestController {

    private final LocationService locationService;

    public LocationRestController(LocationService locationService) {
        this.locationService = locationService;
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<LocationSearchResult>>> search(
            @RequestParam(value = "query", required = false) String queryParam,
            @RequestParam(value = "q", required = false) String qParam) {
        String query = queryParam != null && !queryParam.isBlank() ? queryParam : qParam;
        if (query == null || query.isBlank()) {
            return ResponseEntity.ok(ApiResponse.ok(List.of()));
        }
        List<LocationSearchResult> results = locationService.search(query);
        return ResponseEntity.ok(ApiResponse.ok(results));
    }

    @GetMapping("/reverse")
    public ResponseEntity<ApiResponse<LocationSearchResult>> reverseGeocode(
            @RequestParam("lat") Double lat,
            @RequestParam("lng") Double lng) {
        LocationSearchResult result = locationService.reverseGeocode(lat, lng);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
