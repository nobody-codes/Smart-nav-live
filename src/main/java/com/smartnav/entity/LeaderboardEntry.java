package com.smartnav.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "leaderboard", indexes = {
    @Index(name = "idx_leaderboard_points", columnList = "points")
})
public class LeaderboardEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false)
    private Integer points = 0;

    @Column(nullable = false, length = 50)
    private String badge = "Bronze Scout";

    @Column(name = "rank_position")
    private Integer rankPosition = 0;

    @Column(name = "reports_submitted")
    private Integer reportsSubmitted = 0;

    @Column(name = "reports_approved")
    private Integer reportsApproved = 0;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    public LeaderboardEntry() {
    }

    public LeaderboardEntry(User user, Integer points, String badge, Integer rankPosition, Integer reportsSubmitted, Integer reportsApproved) {
        this.user = user;
        this.points = points != null ? points : 0;
        this.badge = badge != null ? badge : "Bronze Scout";
        this.rankPosition = rankPosition != null ? rankPosition : 0;
        this.reportsSubmitted = reportsSubmitted != null ? reportsSubmitted : 0;
        this.reportsApproved = reportsApproved != null ? reportsApproved : 0;
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
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

    public Integer getRankPosition() {
        return rankPosition;
    }

    public void setRankPosition(Integer rankPosition) {
        this.rankPosition = rankPosition;
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

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
