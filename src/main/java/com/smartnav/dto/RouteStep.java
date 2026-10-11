package com.smartnav.dto;

public class RouteStep {

    private String instruction;
    private Double distanceKm;
    private Integer durationMins;
    private String roadName;
    private Boolean isCongested = false;
    private String incidentWarning;

    public RouteStep() {
    }

    public RouteStep(String instruction, Double distanceKm, Integer durationMins, String roadName, Boolean isCongested, String incidentWarning) {
        this.instruction = instruction;
        this.distanceKm = distanceKm;
        this.durationMins = durationMins;
        this.roadName = roadName;
        this.isCongested = isCongested != null ? isCongested : false;
        this.incidentWarning = incidentWarning;
    }

    public String getInstruction() {
        return instruction;
    }

    public void setInstruction(String instruction) {
        this.instruction = instruction;
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

    public String getRoadName() {
        return roadName;
    }

    public void setRoadName(String roadName) {
        this.roadName = roadName;
    }

    public Boolean getIsCongested() {
        return isCongested;
    }

    public void setIsCongested(Boolean isCongested) {
        this.isCongested = isCongested;
    }

    public String getIncidentWarning() {
        return incidentWarning;
    }

    public void setIncidentWarning(String incidentWarning) {
        this.incidentWarning = incidentWarning;
    }
}
