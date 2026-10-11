package com.smartnav.dto;

import java.util.HashMap;
import java.util.Map;

public class AnalyticsDto {

    private long totalUsers;
    private long activeReports;
    private long approvedReports;
    private long pendingReports;
    private long rejectedReports;
    private long todayReports;

    private Map<String, Long> categoryDistribution = new HashMap<>();
    private Map<String, Long> dailyTrafficReports = new HashMap<>();
    private Map<String, Long> monthlyUserGrowth = new HashMap<>();

    public AnalyticsDto() {
    }

    public long getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(long totalUsers) {
        this.totalUsers = totalUsers;
    }

    public long getActiveReports() {
        return activeReports;
    }

    public void setActiveReports(long activeReports) {
        this.activeReports = activeReports;
    }

    public long getApprovedReports() {
        return approvedReports;
    }

    public void setApprovedReports(long approvedReports) {
        this.approvedReports = approvedReports;
    }

    public long getPendingReports() {
        return pendingReports;
    }

    public void setPendingReports(long pendingReports) {
        this.pendingReports = pendingReports;
    }

    public long getRejectedReports() {
        return rejectedReports;
    }

    public void setRejectedReports(long rejectedReports) {
        this.rejectedReports = rejectedReports;
    }

    public long getTodayReports() {
        return todayReports;
    }

    public void setTodayReports(long todayReports) {
        this.todayReports = todayReports;
    }

    public Map<String, Long> getCategoryDistribution() {
        return categoryDistribution;
    }

    public void setCategoryDistribution(Map<String, Long> categoryDistribution) {
        this.categoryDistribution = categoryDistribution;
    }

    public Map<String, Long> getDailyTrafficReports() {
        return dailyTrafficReports;
    }

    public void setDailyTrafficReports(Map<String, Long> dailyTrafficReports) {
        this.dailyTrafficReports = dailyTrafficReports;
    }

    public Map<String, Long> getMonthlyUserGrowth() {
        return monthlyUserGrowth;
    }

    public void setMonthlyUserGrowth(Map<String, Long> monthlyUserGrowth) {
        this.monthlyUserGrowth = monthlyUserGrowth;
    }
}
