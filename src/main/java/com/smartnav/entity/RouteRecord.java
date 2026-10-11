package com.smartnav.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "routes")
public class RouteRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = true)
    private User user;

    @Column(name = "source_name", nullable = false, length = 255)
    private String sourceName;

    @Column(name = "source_lat", nullable = false)
    private Double sourceLat;

    @Column(name = "source_lng", nullable = false)
    private Double sourceLng;

    @Column(name = "dest_name", nullable = false, length = 255)
    private String destName;

    @Column(name = "dest_lat", nullable = false)
    private Double destLat;

    @Column(name = "dest_lng", nullable = false)
    private Double destLng;

    @Column(name = "distance_km", nullable = false)
    private Double distanceKm;

    @Column(name = "duration_mins", nullable = false)
    private Integer durationMins;

    @Column(name = "route_geometry_json", columnDefinition = "LONGTEXT")
    private String routeGeometryJson;

    @Column(name = "is_congested", nullable = false)
    private Boolean isCongested = false;

    @Column(name = "has_blocked_avoidance", nullable = false)
    private Boolean hasBlockedAvoidance = false;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public RouteRecord() {
    }

    public RouteRecord(User user, String sourceName, Double sourceLat, Double sourceLng,
                       String destName, Double destLat, Double destLng,
                       Double distanceKm, Integer durationMins, String routeGeometryJson,
                       Boolean isCongested, Boolean hasBlockedAvoidance) {
        this.user = user;
        this.sourceName = sourceName;
        this.sourceLat = sourceLat;
        this.sourceLng = sourceLng;
        this.destName = destName;
        this.destLat = destLat;
        this.destLng = destLng;
        this.distanceKm = distanceKm;
        this.durationMins = durationMins;
        this.routeGeometryJson = routeGeometryJson;
        this.isCongested = isCongested != null ? isCongested : false;
        this.hasBlockedAvoidance = hasBlockedAvoidance != null ? hasBlockedAvoidance : false;
        this.createdAt = LocalDateTime.now();
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

    public String getSourceName() {
        return sourceName;
    }

    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    public Double getSourceLat() {
        return sourceLat;
    }

    public void setSourceLat(Double sourceLat) {
        this.sourceLat = sourceLat;
    }

    public Double getSourceLng() {
        return sourceLng;
    }

    public void setSourceLng(Double sourceLng) {
        this.sourceLng = sourceLng;
    }

    public String getDestName() {
        return destName;
    }

    public void setDestName(String destName) {
        this.destName = destName;
    }

    public Double getDestLat() {
        return destLat;
    }

    public void setDestLat(Double destLat) {
        this.destLat = destLat;
    }

    public Double getDestLng() {
        return destLng;
    }

    public void setDestLng(Double destLng) {
        this.destLng = destLng;
    }

    public Double getDistanceKm() {
        return distanceKm;
    }

    public void setDistanceKm(Double distanceKm) {
        this.distanceKm = distanceKm;
    }

    public Integer getDurationMins() {
        return durationMins;
    }

    public void setDurationMins(Integer durationMins) {
        this.durationMins = durationMins;
    }

    public String getRouteGeometryJson() {
        return routeGeometryJson;
    }

    public void setRouteGeometryJson(String routeGeometryJson) {
        this.routeGeometryJson = routeGeometryJson;
    }

    public Boolean getIsCongested() {
        return isCongested;
    }

    public void setIsCongested(Boolean isCongested) {
        this.isCongested = isCongested;
    }

    public Boolean getHasBlockedAvoidance() {
        return hasBlockedAvoidance;
    }

    public void setHasBlockedAvoidance(Boolean hasBlockedAvoidance) {
        this.hasBlockedAvoidance = hasBlockedAvoidance;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
