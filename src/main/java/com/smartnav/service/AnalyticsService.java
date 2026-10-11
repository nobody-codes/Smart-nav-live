package com.smartnav.service;

import com.smartnav.dto.AnalyticsDto;
import com.smartnav.entity.Report;
import com.smartnav.entity.User;
import com.smartnav.repository.ReportRepository;
import com.smartnav.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class AnalyticsService {

    private final UserRepository userRepository;
    private final ReportRepository reportRepository;

    public AnalyticsService(UserRepository userRepository, ReportRepository reportRepository) {
        this.userRepository = userRepository;
        this.reportRepository = reportRepository;
    }

    public AnalyticsDto getAdminAnalytics() {
        AnalyticsDto dto = new AnalyticsDto();

        dto.setTotalUsers(userRepository.count());
        dto.setActiveReports(reportRepository.countByStatus("APPROVED"));
        dto.setApprovedReports(reportRepository.countByStatus("APPROVED"));
        dto.setPendingReports(reportRepository.countByStatus("PENDING"));
        dto.setRejectedReports(reportRepository.countByStatus("REJECTED"));

        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        dto.setTodayReports(reportRepository.countByCreatedAtAfter(startOfToday));

        // Category breakdown
        Map<String, Long> catDist = new LinkedHashMap<>();
        for (Object[] row : reportRepository.countReportsByCategory()) {
            if (row != null && row.length >= 2) {
                String cat = (String) row[0];
                Long count = ((Number) row[1]).longValue();
                catDist.put(cat, count);
            }
        }
        // Fill in missing standard categories if empty
        String[] standardCats = {"ACCIDENT", "TRAFFIC_JAM", "ROAD_BLOCK", "CONSTRUCTION", "FLOOD", "POLICE_CHECKPOINT", "BROKEN_SIGNAL"};
        for (String c : standardCats) {
            catDist.putIfAbsent(c, 0L);
        }
        dto.setCategoryDistribution(catDist);

        // Daily traffic reports (last 7 days)
        Map<String, Long> dailyTrends = new LinkedHashMap<>();
        DateTimeFormatter dayFormatter = DateTimeFormatter.ofPattern("MMM dd");
        List<Report> allReports = reportRepository.findAll();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            String dayKey = date.format(dayFormatter);
            long count = allReports.stream()
                    .filter(r -> r.getCreatedAt() != null && r.getCreatedAt().toLocalDate().isEqual(date))
                    .count();
            dailyTrends.put(dayKey, count);
        }
        dto.setDailyTrafficReports(dailyTrends);

        // Monthly user growth (last 6 months)
        Map<String, Long> monthlyGrowth = new LinkedHashMap<>();
        DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("MMM yyyy");
        List<User> allUsers = userRepository.findAll();
        for (int i = 5; i >= 0; i--) {
            LocalDate monthDate = LocalDate.now().minusMonths(i);
            String monthKey = monthDate.format(monthFormatter);
            long count = allUsers.stream()
                    .filter(u -> u.getCreatedAt() != null &&
                            u.getCreatedAt().getYear() == monthDate.getYear() &&
                            u.getCreatedAt().getMonth() == monthDate.getMonth())
                    .count();
            monthlyGrowth.put(monthKey, count);
        }
        dto.setMonthlyUserGrowth(monthlyGrowth);

        return dto;
    }
}
