package com.smartnav.dto;

public class LeaderboardDto {

    private Long userId;
    private String userName;
    private Integer points;
    private String badge;
    private Integer rank;
    private Integer reportsSubmitted;
    private Integer reportsApproved;

    public LeaderboardDto() {
    }

    public LeaderboardDto(Long userId, String userName, Integer points, String badge, Integer rank, Integer reportsSubmitted, Integer reportsApproved) {
        this.userId = userId;
        this.userName = userName;
        this.points = points;
        this.badge = badge;
        this.rank = rank;
        this.reportsSubmitted = reportsSubmitted;
        this.reportsApproved = reportsApproved;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public Integer getPoints() {
        return points;
    }

    public void setPoints(Integer points) {
        this.points = points;
    }

    public String getBadge() {
        return badge;
    }

    public void setBadge(String badge) {
        this.badge = badge;
    }

    public Integer getRank() {
        return rank;
    }

    public void setRank(Integer rank) {
        this.rank = rank;
    }

    public Integer getReportsSubmitted() {
        return reportsSubmitted;
    }

    public void setReportsSubmitted(Integer reportsSubmitted) {
        this.reportsSubmitted = reportsSubmitted;
    }

    public Integer getReportsApproved() {
        return reportsApproved;
    }

    public void setReportsApproved(Integer reportsApproved) {
        this.reportsApproved = reportsApproved;
    }
}
