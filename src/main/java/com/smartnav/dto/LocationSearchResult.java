package com.smartnav.dto;

public class LocationSearchResult {
    private String displayName;
    private String name;
    private Double lat;
    private Double lng;
    private String city;
    private String state;
    private String country;

    public LocationSearchResult() {
    }

    public LocationSearchResult(String displayName, String name, Double lat, Double lng, String city, String state, String country) {
        this.displayName = displayName;
        this.name = name;
        this.lat = lat;
        this.lng = lng;
        this.city = city;
        this.state = state;
        this.country = country;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Double getLat() {
        return lat;
    }

    public void setLat(Double lat) {
        this.lat = lat;
    }

    public Double getLatitude() {
        return lat;
    }

    public Double getLng() {
        return lng;
    }

    public void setLng(Double lng) {
        this.lng = lng;
    }

    public Double getLongitude() {
        return lng;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }
}
