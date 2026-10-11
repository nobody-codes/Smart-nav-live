package com.smartnav.service;

import com.smartnav.dto.ReportRequest;
import com.smartnav.dto.ReportResponse;
import com.smartnav.entity.Report;
import com.smartnav.entity.User;
import com.smartnav.repository.ReportRepository;
import com.smartnav.websocket.TrafficNotificationPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReportService {

    private final ReportRepository reportRepository;
    private final LeaderboardService leaderboardService;
    private final NotificationService notificationService;
    private final TrafficNotificationPublisher publisher;

    public ReportService(ReportRepository reportRepository,
                         LeaderboardService leaderboardService,
                         NotificationService notificationService,
                         TrafficNotificationPublisher publisher) {
        this.reportRepository = reportRepository;
        this.leaderboardService = leaderboardService;
        this.notificationService = notificationService;
        this.publisher = publisher;
    }

    @Transactional
    public ReportResponse createReport(User user, ReportRequest request, String imagePath) {
        String finalImage = (imagePath != null) ? imagePath : request.getImage();
        if (finalImage == null || finalImage.isEmpty()) {
            finalImage = getDefaultCategoryIcon(request.getCategory());
        }

        String city = request.getCity();
        String state = request.getState();
        String country = request.getCountry() != null ? request.getCountry() : "India";

        if (city == null || city.isBlank()) {
            String addr = request.getAddress().toLowerCase();
            if (addr.contains("pune")) { city = "Pune"; state = "Maharashtra"; }
            else if (addr.contains("mumbai")) { city = "Mumbai"; state = "Maharashtra"; }
            else if (addr.contains("bengaluru") || addr.contains("bangalore")) { city = "Bengaluru"; state = "Karnataka"; }
            else if (addr.contains("delhi")) { city = "Delhi"; state = "Delhi"; }
            else if (addr.contains("hyderabad")) { city = "Hyderabad"; state = "Telangana"; }
            else if (addr.contains("chennai")) { city = "Chennai"; state = "Tamil Nadu"; }
            else if (addr.contains("nashik")) { city = "Nashik"; state = "Maharashtra"; }
            else if (addr.contains("nagpur")) { city = "Nagpur"; state = "Maharashtra"; }
            else { city = "Pune"; state = "Maharashtra"; }
        }

        Report report = new Report(
                user,
                request.getCategory().toUpperCase().trim(),
                request.getDescription().trim(),
                finalImage,
                request.getLatitude(),
                request.getLongitude(),
                request.getAddress().trim(),
                city,
                state,
                country,
                request.getSeverity() != null ? request.getSeverity().toUpperCase().trim() : "MEDIUM"
        );

        // Auto-approve if user is an ADMIN
        if ("ADMIN".equals(user.getRole())) {
            report.setStatus("APPROVED");
        } else {
            report.setStatus("PENDING");
        }

        Report saved = reportRepository.save(report);

        // Award points for submission
        leaderboardService.recordReportSubmission(user);

        ReportResponse responseDto = ReportResponse.fromEntity(saved);

        // Broadcast to WebSocket
        publisher.publishNewReport(responseDto);

        // Notify user
        notificationService.createNotification(
                user,
                "Report Submitted",
                "Your " + formatCategoryName(saved.getCategory()) + " report was submitted and is pending verification.",
                "REPORT_STATUS"
        );

        return responseDto;
    }

    @Transactional
    public ReportResponse approveReport(Long reportId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Report not found with id: " + reportId));

        String oldStatus = report.getStatus();
        report.setStatus("APPROVED");
        Report updated = reportRepository.save(report);

        // Award approval points (+10)
        leaderboardService.recordReportApproval(report.getUser());

        ReportResponse responseDto = ReportResponse.fromEntity(updated);
        publisher.publishReportStatusChange(responseDto, oldStatus);

        // Notify reporting user
        notificationService.createNotification(
                report.getUser(),
                "Incident Report Approved! (+10 pts)",
                "Your incident report for \"" + report.getAddress() + "\" has been verified and published to the live map!",
                "REPORT_STATUS"
        );

        return responseDto;
    }

    @Transactional
    public ReportResponse rejectReport(Long reportId, String reason) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Report not found with id: " + reportId));

        String oldStatus = report.getStatus();
        report.setStatus("REJECTED");
        Report updated = reportRepository.save(report);

        ReportResponse responseDto = ReportResponse.fromEntity(updated);
        publisher.publishReportStatusChange(responseDto, oldStatus);

        notificationService.createNotification(
                report.getUser(),
                "Incident Report Not Approved",
                "Your incident report for \"" + report.getAddress() + "\" was rejected." + (reason != null ? " Reason: " + reason : ""),
                "REPORT_STATUS"
        );

        return responseDto;
    }

    @Transactional
    public void deleteReport(Long reportId) {
        if (!reportRepository.existsById(reportId)) {
            throw new IllegalArgumentException("Report not found with id: " + reportId);
        }
        reportRepository.deleteById(reportId);
        publisher.publishReportDeleted(reportId);
    }

    @Transactional
    public ReportResponse updateReport(Long reportId, ReportRequest request) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Report not found with id: " + reportId));

        if (request.getCategory() != null) report.setCategory(request.getCategory().toUpperCase().trim());
        if (request.getDescription() != null) report.setDescription(request.getDescription().trim());
        if (request.getAddress() != null) report.setAddress(request.getAddress().trim());
        if (request.getSeverity() != null) report.setSeverity(request.getSeverity().toUpperCase().trim());
        if (request.getLatitude() != null) report.setLatitude(request.getLatitude());
        if (request.getLongitude() != null) report.setLongitude(request.getLongitude());
        if (request.getCity() != null) report.setCity(request.getCity());
        if (request.getState() != null) report.setState(request.getState());

        Report updated = reportRepository.save(report);
        ReportResponse responseDto = ReportResponse.fromEntity(updated);
        publisher.publishReportStatusChange(responseDto, report.getStatus());
        return responseDto;
    }

    public List<ReportResponse> getAllReports() {
        return reportRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(ReportResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public List<ReportResponse> getApprovedReports() {
        return reportRepository.findByStatusOrderByCreatedAtDesc("APPROVED")
                .stream()
                .map(ReportResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public List<ReportResponse> getUserReports(Long userId) {
        return reportRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(ReportResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public ReportResponse getReportById(Long id) {
        return reportRepository.findById(id)
                .map(ReportResponse::fromEntity)
                .orElseThrow(() -> new IllegalArgumentException("Report not found with id: " + id));
    }

    public List<ReportResponse> filterReports(String category, String status, Long userId, LocalDateTime startDate, LocalDateTime endDate) {
        return reportRepository.filterReports(category, status, userId, startDate, endDate)
                .stream()
                .map(ReportResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public List<ReportResponse> getReportsInRadius(double centerLat, double centerLng, double radiusKm) {
        return reportRepository.findByStatusOrderByCreatedAtDesc("APPROVED")
                .stream()
                .filter(report -> haversineDistanceKm(centerLat, centerLng, report.getLatitude(), report.getLongitude()) <= radiusKm)
                .map(ReportResponse::fromEntity)
                .collect(Collectors.toList());
    }

    // Haversine Distance in Kilometers
    public double haversineDistanceKm(double lat1, double lon1, double lat2, double lon2) {
        final int EARTH_RADIUS_KM = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }

    private String getDefaultCategoryIcon(String category) {
        if (category == null) return "/images/incident-general.svg";
        return switch (category.toUpperCase()) {
            case "ACCIDENT" -> "/images/incident-accident.svg";
            case "TRAFFIC_JAM" -> "/images/incident-traffic.svg";
            case "ROAD_BLOCK" -> "/images/incident-roadblock.svg";
            case "CONSTRUCTION" -> "/images/incident-construction.svg";
            case "FLOOD" -> "/images/incident-flood.svg";
            case "POLICE_CHECKPOINT" -> "/images/incident-police.svg";
            case "BROKEN_SIGNAL" -> "/images/incident-signal.svg";
            case "POTHOLE" -> "/images/incident-construction.svg";
            case "VEHICLE_BREAKDOWN" -> "/images/incident-accident.svg";
            case "FIRE" -> "/images/incident-roadblock.svg";
            default -> "/images/incident-general.svg";
        };
    }

    private String formatCategoryName(String category) {
        if (category == null) return "Incident";
        return switch (category.toUpperCase()) {
            case "TRAFFIC_JAM" -> "Traffic Jam";
            case "ROAD_BLOCK" -> "Road Block";
            case "POLICE_CHECKPOINT" -> "Police Checkpoint";
            case "BROKEN_SIGNAL" -> "Broken Signal";
            case "VEHICLE_BREAKDOWN" -> "Vehicle Breakdown";
            case "POTHOLE" -> "Pothole";
            case "FLOOD" -> "Flooded Road";
            case "CONSTRUCTION" -> "Road Construction";
            case "FIRE" -> "Fire Incident";
            case "OTHER" -> "Other Hazard";
            default -> category.substring(0, 1).toUpperCase() + category.substring(1).toLowerCase();
        };
    }
}
