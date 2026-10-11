package com.smartnav.dto;

import jakarta.validation.constraints.NotNull;

public class RouteCalculationRequest {

    @NotNull(message = "Source latitude is required")
    private Double sourceLat;

    @NotNull(message = "Source longitude is required")
    private Double sourceLng;

    private String sourceName = "Starting Point";

    @NotNull(message = "Destination latitude is required")
    private Double destLat;

    @NotNull(message = "Destination longitude is required")
    private Double destLng;

    private String destName = "Destination";

    private Boolean avoidBlockedRoads = true;

    private Boolean preferLeastCongested = true;

    public RouteCalculationRequest() {
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

    public String getSourceName() {
        return sourceName;
    }

    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
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

    public String getDestName() {
        return destName;
    }

    public void setDestName(String destName) {
        this.destName = destName;
    }

    public Boolean getAvoidBlockedRoads() {
        return avoidBlockedRoads != null ? avoidBlockedRoads : true;
    }

    public void setAvoidBlockedRoads(Boolean avoidBlockedRoads) {
        this.avoidBlockedRoads = avoidBlockedRoads;
    }

    public Boolean getPreferLeastCongested() {
        return preferLeastCongested != null ? preferLeastCongested : true;
    }

    public void setPreferLeastCongested(Boolean preferLeastCongested) {
        this.preferLeastCongested = preferLeastCongested;
    }
}
