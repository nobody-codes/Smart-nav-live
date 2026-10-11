package com.smartnav.controller;

import com.smartnav.dto.ApiResponse;
import com.smartnav.dto.ReportRequest;
import com.smartnav.dto.ReportResponse;
import com.smartnav.entity.User;
import com.smartnav.security.SecurityUtils;
import com.smartnav.service.FileStorageService;
import com.smartnav.service.ReportService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping({"/api/reports", "/api/incidents"})
public class ReportRestController {

    private final ReportService reportService;
    private final FileStorageService fileStorageService;
    private final SecurityUtils securityUtils;
    private final com.smartnav.repository.UserRepository userRepository;

    public ReportRestController(ReportService reportService,
                                FileStorageService fileStorageService,
                                SecurityUtils securityUtils,
                                com.smartnav.repository.UserRepository userRepository) {
        this.reportService = reportService;
        this.fileStorageService = fileStorageService;
        this.securityUtils = securityUtils;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ReportResponse>>> getReports(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        List<ReportResponse> reports;
        if (category != null || status != null || userId != null || startDate != null || endDate != null) {
            reports = reportService.filterReports(category, status, userId, startDate, endDate);
        } else {
            reports = reportService.getAllReports();
        }
        return ResponseEntity.ok(ApiResponse.ok(reports));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ReportResponse>> getReportById(@PathVariable Long id) {
        try {
            ReportResponse report = reportService.getReportById(id);
            return ResponseEntity.ok(ApiResponse.ok(report));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<ReportResponse>> createReportJson(@Valid @RequestBody ReportRequest request) {
        User user = securityUtils.getCurrentUser().orElseGet(() ->
                userRepository.findByEmail("john.doe@smartnav.com").orElse(null)
        );
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Must be logged in or database must have seed users"));
        }

        ReportResponse created = reportService.createReport(user, request, null);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Report submitted successfully", created));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ReportResponse>> createReportMultipart(
            @RequestParam("category") String category,
            @RequestParam("description") String description,
            @RequestParam("latitude") Double latitude,
            @RequestParam("longitude") Double longitude,
            @RequestParam("address") String address,
            @RequestParam(value = "severity", defaultValue = "MEDIUM") String severity,
            @RequestParam(value = "photo", required = false) MultipartFile photo) {

        User user = securityUtils.getCurrentUser().orElseGet(() ->
                userRepository.findByEmail("john.doe@smartnav.com").orElse(null)
        );
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Must be logged in or database must have seed users"));
        }

        String imagePath = null;
        if (photo != null && !photo.isEmpty()) {
            imagePath = fileStorageService.storeFile(photo);
        }

        ReportRequest request = new ReportRequest();
        request.setCategory(category);
        request.setDescription(description);
        request.setLatitude(latitude);
        request.setLongitude(longitude);
        request.setAddress(address);
        request.setSeverity(severity);

        ReportResponse created = reportService.createReport(user, request, imagePath);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Report submitted successfully", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ReportResponse>> updateReport(
            @PathVariable Long id,
            @RequestBody ReportRequest request) {
        User user = securityUtils.getCurrentUser().orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Must be logged in to update reports"));
        }

        try {
            ReportResponse existing = reportService.getReportById(id);
            if (!securityUtils.isAdmin() && !existing.getUserId().equals(user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error("Not authorized to edit this report"));
            }

            ReportResponse updated = reportService.updateReport(id, request);
            return ResponseEntity.ok(ApiResponse.ok("Report updated successfully", updated));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteReport(@PathVariable Long id) {
        User user = securityUtils.getCurrentUser().orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Must be logged in to delete reports"));
        }

        try {
            ReportResponse report = reportService.getReportById(id);
            // Allow admin or report owner to delete
            if (!securityUtils.isAdmin() && !report.getUserId().equals(user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error("Not authorized to delete this report"));
            }

            reportService.deleteReport(id);
            return ResponseEntity.ok(ApiResponse.ok("Report deleted successfully", null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/nearby")
    public ResponseEntity<ApiResponse<List<ReportResponse>>> getNearby(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "5.0") double radius) {

        List<ReportResponse> nearby = reportService.getReportsInRadius(lat, lng, radius);
        return ResponseEntity.ok(ApiResponse.ok(nearby));
    }

    @GetMapping("/export/csv")
    public void exportReportsToCsv(HttpServletResponse response) throws IOException {
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=\"traffic-reports.csv\"");

        List<ReportResponse> reports = reportService.getAllReports();
        PrintWriter writer = response.getWriter();
        writer.println("ID,Category,Severity,Status,Address,Latitude,Longitude,ReportedBy,CreatedAt");

        for (ReportResponse r : reports) {
            writer.printf("%d,\"%s\",\"%s\",\"%s\",\"%s\",%f,%f,\"%s\",\"%s\"%n",
                    r.getId(),
                    r.getCategory(),
                    r.getSeverity(),
                    r.getStatus(),
                    r.getAddress().replace("\"", "\"\""),
                    r.getLatitude(),
                    r.getLongitude(),
                    r.getUserName() != null ? r.getUserName().replace("\"", "\"\"") : "Anonymous",
                    r.getCreatedAt());
        }
        writer.flush();
    }
}
