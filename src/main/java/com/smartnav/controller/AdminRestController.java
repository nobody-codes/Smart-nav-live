package com.smartnav.controller;

import com.smartnav.dto.AnalyticsDto;
import com.smartnav.dto.ApiResponse;
import com.smartnav.dto.ReportResponse;
import com.smartnav.entity.SystemSetting;
import com.smartnav.entity.User;
import com.smartnav.repository.SystemSettingRepository;
import com.smartnav.service.AnalyticsService;
import com.smartnav.service.ReportService;
import com.smartnav.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminRestController {

    private final ReportService reportService;
    private final UserService userService;
    private final AnalyticsService analyticsService;
    private final SystemSettingRepository systemSettingRepository;

    public AdminRestController(ReportService reportService,
                               UserService userService,
                               AnalyticsService analyticsService,
                               SystemSettingRepository systemSettingRepository) {
        this.reportService = reportService;
        this.userService = userService;
        this.analyticsService = analyticsService;
        this.systemSettingRepository = systemSettingRepository;
    }

    @GetMapping("/reports")
    public ResponseEntity<ApiResponse<List<ReportResponse>>> getAllAdminReports(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status) {
        List<ReportResponse> reports = reportService.filterReports(category, status, null, null, null);
        return ResponseEntity.ok(ApiResponse.ok(reports));
    }

    @GetMapping("/incidents/pending")
    public ResponseEntity<ApiResponse<List<ReportResponse>>> getPendingIncidents() {
        List<ReportResponse> pending = reportService.filterReports(null, "PENDING", null, null, null);
        return ResponseEntity.ok(ApiResponse.ok(pending));
    }

    @PutMapping({"/approve/{id}", "/incidents/{id}/approve"})
    public ResponseEntity<ApiResponse<ReportResponse>> approveReport(@PathVariable Long id) {
        ReportResponse approved = reportService.approveReport(id);
        return ResponseEntity.ok(ApiResponse.ok("Report approved and published to live map", approved));
    }

    @PutMapping({"/reject/{id}", "/incidents/{id}/reject"})
    public ResponseEntity<ApiResponse<ReportResponse>> rejectReport(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        String reason = (body != null) ? body.get("reason") : "Does not meet verification standards";
        ReportResponse rejected = reportService.rejectReport(id, reason);
        return ResponseEntity.ok(ApiResponse.ok("Report rejected", rejected));
    }

    @DeleteMapping({"/reports/{id}", "/incidents/{id}"})
    public ResponseEntity<ApiResponse<Void>> deleteFakeReport(@PathVariable Long id) {
        reportService.deleteReport(id);
        return ResponseEntity.ok(ApiResponse.ok("Report deleted permanently", null));
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<com.smartnav.dto.UserDto>>> getAllUsers() {
        List<com.smartnav.dto.UserDto> users = userService.getAllUserDtos();
        return ResponseEntity.ok(ApiResponse.ok(users));
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<ApiResponse<com.smartnav.dto.UserDto>> getUserById(@PathVariable Long id) {
        User user = userService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + id));
        com.smartnav.dto.UserDto dto = com.smartnav.dto.UserDto.fromEntity(user);
        com.smartnav.dto.UserProfileDto profile = userService.getUserProfileDto(id);
        dto.setTotalReports(profile.getTotalReports());
        dto.setApprovedReports(profile.getApprovedReports());
        return ResponseEntity.ok(ApiResponse.ok(dto));
    }

    @PutMapping("/users/{id}/toggle-status")
    public ResponseEntity<ApiResponse<com.smartnav.dto.UserDto>> toggleUserStatus(@PathVariable Long id) {
        try {
            User updated = userService.toggleUserStatus(id);
            return ResponseEntity.ok(ApiResponse.ok("User status updated to " + updated.getStatus(), com.smartnav.dto.UserDto.fromEntity(updated)));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/analytics")
    public ResponseEntity<ApiResponse<AnalyticsDto>> getAnalytics() {
        AnalyticsDto analytics = analyticsService.getAdminAnalytics();
        return ResponseEntity.ok(ApiResponse.ok(analytics));
    }

    @GetMapping("/settings")
    public ResponseEntity<ApiResponse<List<SystemSetting>>> getSettings() {
        return ResponseEntity.ok(ApiResponse.ok(systemSettingRepository.findAll()));
    }

    @PostMapping("/settings")
    public ResponseEntity<ApiResponse<SystemSetting>> updateSetting(@RequestBody Map<String, String> payload) {
        String key = payload.get("settingKey");
        String value = payload.get("settingValue");
        SystemSetting setting = systemSettingRepository.findBySettingKey(key)
                .orElse(new SystemSetting(key, value, "System parameter"));
        setting.setSettingValue(value);
        SystemSetting saved = systemSettingRepository.save(setting);
        return ResponseEntity.ok(ApiResponse.ok("Setting updated", saved));
    }
}
