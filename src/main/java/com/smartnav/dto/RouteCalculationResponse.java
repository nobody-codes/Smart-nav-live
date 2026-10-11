package com.smartnav.dto;

import java.util.ArrayList;
import java.util.List;

public class RouteCalculationResponse {

    private Double totalDistanceKm;
    private Integer estimatedDurationMins;
    private Integer originalDurationMins;
    private Integer delayMins = 0;
    private Boolean isCongested = false;
    private Boolean hasBlockedAvoidance = false;
    private String summary;
    private List<List<Double>> coordinates = new ArrayList<>(); // [[lat, lng], ...]
    private List<List<Double>> congestedCoordinates = new ArrayList<>(); // sub-segments highlighted in RED
    private List<RouteStep> steps = new ArrayList<>();
    private List<ReportResponse> blockedIncidentsAvoided = new ArrayList<>();
    private List<ReportResponse> congestionIncidentsEncountered = new ArrayList<>();

    // Multi-route intelligent navigation fields
    private Boolean hasAlternative = false;
    private List<List<Double>> alternativeCoordinates = new ArrayList<>();
    private Double alternativeDistanceKm;
    private Integer alternativeDurationMins;
    private String alternativeSummary;
    private List<RouteStep> alternativeSteps = new ArrayList<>();
    private String warningMessage;
    private String routeStatus = "NORMAL"; // NORMAL, AFFECTED, DETOURED

    public RouteCalculationResponse() {
    }

    public Double getTotalDistanceKm() {
        return totalDistanceKm;
    }

    public void setTotalDistanceKm(Double totalDistanceKm) {
        this.totalDistanceKm = totalDistanceKm;
    }

    public Integer getEstimatedDurationMins() {
        return estimatedDurationMins;
    }

    public void setEstimatedDurationMins(Integer estimatedDurationMins) {
        this.estimatedDurationMins = estimatedDurationMins;
    }

    public Integer getOriginalDurationMins() {
        return originalDurationMins;
    }

    public void setOriginalDurationMins(Integer originalDurationMins) {
        this.originalDurationMins = originalDurationMins;
    }

    public Integer getDelayMins() {
        return delayMins;
    }

    public void setDelayMins(Integer delayMins) {
        this.delayMins = delayMins;
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

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public List<List<Double>> getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(List<List<Double>> coordinates) {
        this.coordinates = coordinates;
    }

    public List<List<Double>> getCongestedCoordinates() {
        return congestedCoordinates;
    }

    public void setCongestedCoordinates(List<List<Double>> congestedCoordinates) {
        this.congestedCoordinates = congestedCoordinates;
    }

    public List<RouteStep> getSteps() {
        return steps;
    }

    public void setSteps(List<RouteStep> steps) {
        this.steps = steps;
    }

    public List<ReportResponse> getBlockedIncidentsAvoided() {
        return blockedIncidentsAvoided;
    }

    public void setBlockedIncidentsAvoided(List<ReportResponse> blockedIncidentsAvoided) {
        this.blockedIncidentsAvoided = blockedIncidentsAvoided;
    }

    public List<ReportResponse> getCongestionIncidentsEncountered() {
        return congestionIncidentsEncountered;
    }

    public void setCongestionIncidentsEncountered(List<ReportResponse> congestionIncidentsEncountered) {
        this.congestionIncidentsEncountered = congestionIncidentsEncountered;
    }

    public Boolean getHasAlternative() {
        return hasAlternative;
    }

    public void setHasAlternative(Boolean hasAlternative) {
        this.hasAlternative = hasAlternative;
    }

    public List<List<Double>> getAlternativeCoordinates() {
        return alternativeCoordinates;
    }

    public void setAlternativeCoordinates(List<List<Double>> alternativeCoordinates) {
        this.alternativeCoordinates = alternativeCoordinates;
    }

    public Double getAlternativeDistanceKm() {
        return alternativeDistanceKm;
    }

    public void setAlternativeDistanceKm(Double alternativeDistanceKm) {
        this.alternativeDistanceKm = alternativeDistanceKm;
    }

    public Integer getAlternativeDurationMins() {
        return alternativeDurationMins;
    }

    public void setAlternativeDurationMins(Integer alternativeDurationMins) {
        this.alternativeDurationMins = alternativeDurationMins;
    }

    public String getAlternativeSummary() {
        return alternativeSummary;
    }

    public void setAlternativeSummary(String alternativeSummary) {
        this.alternativeSummary = alternativeSummary;
    }

    public List<RouteStep> getAlternativeSteps() {
        return alternativeSteps;
    }

    public void setAlternativeSteps(List<RouteStep> alternativeSteps) {
        this.alternativeSteps = alternativeSteps;
    }

    public String getWarningMessage() {
        return warningMessage;
    }

    public void setWarningMessage(String warningMessage) {
        this.warningMessage = warningMessage;
    }

    public String getRouteStatus() {
        return routeStatus;
    }

    public void setRouteStatus(String routeStatus) {
        this.routeStatus = routeStatus;
    }

    // Alias getters for frontend compatibility
    public List<List<Double>> getRouteCoordinates() {
        return coordinates;
    }

    public Double getDistanceKm() {
        return totalDistanceKm;
    }

    public Integer getDurationMinutes() {
        return estimatedDurationMins;
    }

    public List<RouteStep> getTurnByTurnSteps() {
        return steps;
    }
}
