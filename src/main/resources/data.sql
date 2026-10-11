-- ========================================================
-- Initial Seed Data for SmartNav System (Real-World Indian Geospatial Data)
-- ========================================================

-- Insert Initial Users (Passwords: admin123, user123)
-- BCrypt encoded hash for 'admin123': $2a$10$n5vDk91G7e8Lqf/07u.eTeSj3Nf1/d3mF4s4lQvT4Yd2C1P1m8pqq
-- BCrypt encoded hash for 'user123':  $2a$10$j8n2oG5Vp7cK1qX4W3m2lO8uJ9kP4m6vR2t1yQ0zW9xP8oI7n4m5y

INSERT INTO users (id, name, email, password, role, phone, points, badge, status, created_at)
VALUES 
(1, 'System Administrator', 'admin@smartnav.com', '$2a$10$n5vDk91G7e8Lqf/07u.eTeSj3Nf1/d3mF4s4lQvT4Yd2C1P1m8pqq', 'ADMIN', '+91-98220-01100', 350, 'Platinum Sentinel', 'ACTIVE', CURRENT_TIMESTAMP),
(2, 'Rahul Sharma', 'john.doe@smartnav.com', '$2a$10$j8n2oG5Vp7cK1qX4W3m2lO8uJ9kP4m6vR2t1yQ0zW9xP8oI7n4m5y', 'USER', '+91-98220-01101', 120, 'Platinum Sentinel', 'ACTIVE', CURRENT_TIMESTAMP),
(3, 'Priya Deshmukh', 'jane.smith@smartnav.com', '$2a$10$j8n2oG5Vp7cK1qX4W3m2lO8uJ9kP4m6vR2t1yQ0zW9xP8oI7n4m5y', 'USER', '+91-98220-01102', 75, 'Gold Pioneer', 'ACTIVE', CURRENT_TIMESTAMP),
(4, 'Amit Kulkarni', 'alex.rivera@smartnav.com', '$2a$10$j8n2oG5Vp7cK1qX4W3m2lO8uJ9kP4m6vR2t1yQ0zW9xP8oI7n4m5y', 'USER', '+91-98220-01103', 35, 'Silver Navigator', 'ACTIVE', CURRENT_TIMESTAMP),
(5, 'Guest Explorer', 'guest@smartnav.com', '$2a$10$j8n2oG5Vp7cK1qX4W3m2lO8uJ9kP4m6vR2t1yQ0zW9xP8oI7n4m5y', 'GUEST', '+91-98220-01104', 10, 'Bronze Scout', 'ACTIVE', CURRENT_TIMESTAMP);

-- Insert Realistic Traffic Incident Reports (Verified Real Coordinates in Pune, Mumbai, Bengaluru, Delhi, Hyderabad)
INSERT INTO reports (id, user_id, category, description, image, latitude, longitude, address, city, state, country, status, severity, created_at)
VALUES
(1, 2, 'ROAD_BLOCK', 'Emergency water main burst repair. Entire section barricaded for vehicular transit.', '/images/incident-roadblock.svg', 18.5920, 73.7580, 'Wakad Flyover & Hinjawadi Road Junction', 'Pune', 'Maharashtra', 'India', 'APPROVED', 'CRITICAL', CURRENT_TIMESTAMP),
(2, 3, 'TRAFFIC_JAM', 'Heavy gridlock during rush hour due to ongoing metro barricades on JM Road.', '/images/incident-traffic.svg', 18.5314, 73.8446, 'Shivajinagar Junction & JM Road', 'Pune', 'Maharashtra', 'India', 'APPROVED', 'HIGH', CURRENT_TIMESTAMP),
(3, 2, 'CONSTRUCTION', 'Station circle road resurfacing work. Single operational lane causing bottleneck.', '/images/incident-construction.svg', 18.5289, 73.8744, 'Pune Junction Railway Station, Agarkar Nagar', 'Pune', 'Maharashtra', 'India', 'APPROVED', 'MEDIUM', CURRENT_TIMESTAMP),
(4, 4, 'BROKEN_SIGNAL', 'Traffic light malfunction at Goodluck Chowk; manual traffic police directing flow.', '/images/incident-signal.svg', 18.5246, 73.8415, 'FC Road (Fergusson College Rd), Deccan Gymkhana', 'Pune', 'Maharashtra', 'India', 'APPROVED', 'HIGH', CURRENT_TIMESTAMP),
(5, 3, 'POTHOLE', 'Deep asphalt crater on left lane near ICC Tech Park causing sudden brake checks.', '/images/incident-construction.svg', 18.5308, 73.8289, 'Senapati Bapat Road near JW Marriott', 'Pune', 'Maharashtra', 'India', 'APPROVED', 'MEDIUM', CURRENT_TIMESTAMP),
(6, 2, 'ACCIDENT', 'Multi-vehicle collision on highway ramp. Tow truck on scene, right lane blocked.', '/images/incident-accident.svg', 18.4529, 73.8553, 'Katraj Tunnel Bypass, NH 48', 'Pune', 'Maharashtra', 'India', 'APPROVED', 'HIGH', CURRENT_TIMESTAMP),
(7, 4, 'FLOOD', 'Waterlogging in underpass following storm. Low-clearance hatchbacks advised to divert.', '/images/incident-flood.svg', 18.5074, 73.7744, 'Chandni Chowk Underpass, Kothrud', 'Pune', 'Maharashtra', 'India', 'APPROVED', 'HIGH', CURRENT_TIMESTAMP),
(8, 2, 'POLICE_CHECKPOINT', 'Transit checkpoint and commercial vehicle documentation verification.', '/images/incident-police.svg', 18.5385, 73.8340, 'Ganeshkhind Road near E-Square Chowk', 'Pune', 'Maharashtra', 'India', 'APPROVED', 'LOW', CURRENT_TIMESTAMP),
(9, 3, 'VEHICLE_BREAKDOWN', 'Stalled PMPML transit bus blocking roundabout exit.', '/images/incident-accident.svg', 18.5913, 73.7389, 'Shivaji Chowk, Hinjewadi Phase 1', 'Pune', 'Maharashtra', 'India', 'APPROVED', 'MEDIUM', CURRENT_TIMESTAMP),
(10, 4, 'FIRE', 'Small trash fire emitting heavy smoke on highway berm. Fire department en route.', '/images/incident-roadblock.svg', 18.5089, 73.9260, 'Magarpatta Bypass, Hadapsar', 'Pune', 'Maharashtra', 'India', 'PENDING', 'HIGH', CURRENT_TIMESTAMP),
(11, 2, 'ACCIDENT', 'Minor fender bender on curbside, slight slowdown.', '/images/incident-accident.svg', 18.5679, 73.9143, 'Viman Nagar Road near Phoenix Marketcity', 'Pune', 'Maharashtra', 'India', 'PENDING', 'LOW', CURRENT_TIMESTAMP),
(12, 3, 'TRAFFIC_JAM', 'Western Express Highway standstill heading southbound towards Bandra. Average 8 km/h.', '/images/incident-traffic.svg', 19.1136, 72.8697, 'Western Express Highway near Andheri Flyover', 'Mumbai', 'Maharashtra', 'India', 'APPROVED', 'HIGH', CURRENT_TIMESTAMP),
(13, 4, 'POLICE_CHECKPOINT', 'Routine security inspection and FASTag lane regulation.', '/images/incident-police.svg', 19.0330, 72.8160, 'Bandra Toll Plaza, Bandra-Worli Sea Link', 'Mumbai', 'Maharashtra', 'India', 'APPROVED', 'LOW', CURRENT_TIMESTAMP),
(14, 2, 'FLOOD', 'Severe water logging in circle underpass. Heavy vehicles only.', '/images/incident-flood.svg', 19.0434, 72.8634, 'Sion Circle Underpass, Sion East', 'Mumbai', 'Maharashtra', 'India', 'APPROVED', 'CRITICAL', CURRENT_TIMESTAMP),
(15, 3, 'TRAFFIC_JAM', 'Heavy gridlock connecting Outer Ring Road and Electronic City flyover.', '/images/incident-traffic.svg', 12.9172, 77.6228, 'Central Silk Board Junction, Hosur Road', 'Bengaluru', 'Karnataka', 'India', 'APPROVED', 'CRITICAL', CURRENT_TIMESTAMP),
(16, 2, 'ROAD_BLOCK', 'Inner Circle utility trenching; vehicular traffic diverted to Outer Circle.', '/images/incident-roadblock.svg', 28.6315, 77.2167, 'Connaught Place Inner Circle, Radial Rd 2', 'Delhi', 'Delhi', 'India', 'APPROVED', 'HIGH', CURRENT_TIMESTAMP),
(17, 4, 'TRAFFIC_JAM', 'Peak hour software corridor standstill towards Mindspace.', '/images/incident-traffic.svg', 17.4504, 78.3808, 'Cyber Towers Junction, Hitec City', 'Hyderabad', 'Telangana', 'India', 'APPROVED', 'HIGH', CURRENT_TIMESTAMP);

-- Insert Notifications
INSERT INTO notifications (id, user_id, title, message, type, is_read, created_at)
VALUES
(1, 2, 'Report Approved!', 'Your incident report for Baner Road, Pune was verified by Admin. +10 Points earned!', 'REPORT_STATUS', TRUE, CURRENT_TIMESTAMP),
(2, 2, 'Traffic Alert Near You', 'Critical road block reported on Baner Road near Balewadi High Street. Navigation will detour via Pashan Sus Road.', 'INCIDENT_ALERT', FALSE, CURRENT_TIMESTAMP),
(3, 3, 'Badge Unlocked: Gold Pioneer', 'Congratulations! You reached 75 contribution points and earned the Gold Pioneer badge.', 'BADGE_EARNED', FALSE, CURRENT_TIMESTAMP),
(4, NULL, 'System Advisory', 'Real-time WebSocket telemetry is active. Live OpenStreetMap and OSRM routing avoids verified road hazards.', 'SYSTEM', FALSE, CURRENT_TIMESTAMP);

-- Insert Leaderboard
INSERT INTO leaderboard (id, user_id, points, badge, rank_position, reports_submitted, reports_approved, updated_at)
VALUES
(1, 1, 350, 'Platinum Sentinel', 1, 35, 35, CURRENT_TIMESTAMP),
(2, 2, 120, 'Platinum Sentinel', 2, 15, 12, CURRENT_TIMESTAMP),
(3, 3, 75, 'Gold Pioneer', 3, 9, 8, CURRENT_TIMESTAMP),
(4, 4, 35, 'Silver Navigator', 4, 5, 4, CURRENT_TIMESTAMP),
(5, 5, 10, 'Bronze Scout', 5, 1, 1, CURRENT_TIMESTAMP);

-- Insert System Settings
INSERT INTO system_settings (id, setting_key, setting_value, description)
VALUES
(1, 'auto_approve_reports', 'false', 'Automatically approve user submitted incident reports'),
(2, 'congestion_penalty_factor', '1.8', 'Time multiplier for roads with traffic jams'),
(3, 'default_alert_radius_km', '5.0', 'Radius in km to trigger nearby incident alerts'),
(4, 'max_daily_reports_per_user', '20', 'Rate limit for crowdsourced submissions per user per day');
