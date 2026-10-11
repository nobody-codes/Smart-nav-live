-- ========================================================
-- Real-Time Crowdsourced Mapping & Intelligent Navigation
-- Database Schema (MySQL Compatible)
-- ========================================================

-- Drop existing tables if needed (in reverse foreign key order)
DROP TABLE IF EXISTS leaderboard;
DROP TABLE IF EXISTS notifications;
DROP TABLE IF EXISTS routes;
DROP TABLE IF EXISTS reports;
DROP TABLE IF EXISTS system_settings;
DROP TABLE IF EXISTS users;

-- 1. USERS TABLE
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(30) NOT NULL DEFAULT 'USER', -- 'USER', 'ADMIN', 'GUEST'
    phone VARCHAR(30),
    points INT NOT NULL DEFAULT 0,
    badge VARCHAR(50) NOT NULL DEFAULT 'Bronze Scout',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- 'ACTIVE', 'SUSPENDED', 'DISABLED'
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_login TIMESTAMP NULL,
    reset_password_token VARCHAR(100) NULL,
    reset_password_expires_at TIMESTAMP NULL
);

-- 2. REPORTS TABLE (Crowdsourced Traffic Incidents)
CREATE TABLE reports (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    category VARCHAR(50) NOT NULL, -- 'ACCIDENT', 'TRAFFIC_JAM', 'ROAD_BLOCK', 'CONSTRUCTION', 'FLOOD', 'POLICE_CHECKPOINT', 'BROKEN_SIGNAL', 'POTHOLE', 'VEHICLE_BREAKDOWN', 'FIRE', 'OTHER'
    description TEXT NOT NULL,
    image VARCHAR(255),
    latitude DOUBLE NOT NULL,
    longitude DOUBLE NOT NULL,
    address VARCHAR(255) NOT NULL,
    city VARCHAR(100),
    state VARCHAR(100),
    country VARCHAR(100) DEFAULT 'India',
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING', -- 'PENDING', 'APPROVED', 'REJECTED'
    severity VARCHAR(20) NOT NULL DEFAULT 'MEDIUM', -- 'LOW', 'MEDIUM', 'HIGH', 'CRITICAL'
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_report_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 3. NOTIFICATIONS TABLE
CREATE TABLE notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT, -- null means broadcast to all users
    title VARCHAR(150) NOT NULL,
    message VARCHAR(500) NOT NULL,
    type VARCHAR(50) DEFAULT 'INFO', -- 'INCIDENT_ALERT', 'REPORT_STATUS', 'BADGE_EARNED', 'SYSTEM'
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notification_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 4. ROUTES TABLE (Saved / Calculated Intelligent Routes)
CREATE TABLE routes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    source_name VARCHAR(255) NOT NULL,
    source_lat DOUBLE NOT NULL,
    source_lng DOUBLE NOT NULL,
    dest_name VARCHAR(255) NOT NULL,
    dest_lat DOUBLE NOT NULL,
    dest_lng DOUBLE NOT NULL,
    distance_km DOUBLE NOT NULL,
    duration_mins INT NOT NULL,
    route_geometry_json LONGTEXT,
    is_congested BOOLEAN NOT NULL DEFAULT FALSE,
    has_blocked_avoidance BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_route_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);

-- 5. LEADERBOARD TABLE (User Contribution & Badges)
CREATE TABLE leaderboard (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    points INT NOT NULL DEFAULT 0,
    badge VARCHAR(50) NOT NULL DEFAULT 'Bronze Scout',
    rank_position INT DEFAULT 0,
    reports_submitted INT DEFAULT 0,
    reports_approved INT DEFAULT 0,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_leaderboard_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 6. SYSTEM SETTINGS TABLE (Configurable parameters)
CREATE TABLE system_settings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    setting_key VARCHAR(100) NOT NULL UNIQUE,
    setting_value VARCHAR(255) NOT NULL,
    description VARCHAR(255)
);

-- Indexes for performance
CREATE INDEX idx_reports_location ON reports (latitude, longitude);
CREATE INDEX idx_reports_status ON reports (status);
CREATE INDEX idx_reports_category ON reports (category);
CREATE INDEX idx_notifications_user ON notifications (user_id, is_read);
CREATE INDEX idx_leaderboard_points ON leaderboard (points DESC);
