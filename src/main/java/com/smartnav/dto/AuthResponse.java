package com.smartnav.dto;

public class AuthResponse {

    private Long id;
    private String name;
    private String email;
    private String role;
    private Integer points;
    private String badge;
    private String redirectUrl;

    public AuthResponse() {
    }

    public AuthResponse(Long id, String name, String email, String role, Integer points, String badge, String redirectUrl) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.role = role;
        this.points = points;
        this.badge = badge;
        this.redirectUrl = redirectUrl;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
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

    public String getRedirectUrl() {
        return redirectUrl;
    }

    public void setRedirectUrl(String redirectUrl) {
        this.redirectUrl = redirectUrl;
    }
}
