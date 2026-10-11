# Real-Time Crowdsourced Mapping and Intelligent Navigation System

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.4-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-17%20%7C%2021%20%7C%2024-blue.svg)](https://www.oracle.com/java/)
[![Leaflet](https://img.shields.io/badge/Leaflet.js-1.9.4-green.svg)](https://leafletjs.com/)
[![WebSocket](https://img.shields.io/badge/WebSocket-STOMP%20%2F%20SockJS-orange.svg)](https://spring.io)
[![License](https://img.shields.io/badge/License-MIT-purple.svg)](LICENSE)

An enterprise-grade, full-stack final year engineering project delivering real-time crowdsourced traffic hazard alerts, situational mapping, and dynamic shortest-path navigation with intelligent obstacle avoidance and traffic congestion rerouting.

---

## 🌟 Key Highlights & Engineering Features

* **Crowdsourced Hazard Telemetry**: Registered drivers report live traffic incidents with categories (Accidents, Traffic Jams, Road Blocks, Construction, Floods, Police Checkpoints, Broken Signals), severity ratings, reverse-geocoded addresses, and photo uploads.
* **Intelligent Navigation & Hazard Avoidance (`NavigationService`)**:
  * Calculates optimal shortest driving paths.
  * Evaluates active hazards along road corridors.
  * Generates dynamic bypass waypoints around confirmed roadblocks and accidents.
  * Adjusts travel duration with congestion time multiplier penalties (1.8x).
  * High-visibility **red-line polyline** rendering for congested road segments.
* **Instant Real-Time WebSocket Telemetry**:
  * STOMP protocol over SockJS fallback (`/ws-traffic`).
  * Broadcasts new reports and moderation approvals directly to all active clients on `/topic/reports` without page reload.
  * Real-time toast alert popups and notification bell counter badges.
* **Three-Tier Role-Based Security**:
  * **Admin**: Traffic command dashboard, full report approval/rejection moderation, user suspension, Chart.js telemetry charts, and CSV/PDF data exports.
  * **User (Driver)**: Incident reporting with map picker, route planner, saved routes, personal report tracking, contribution points, and badges.
  * **Guest**: Read-only public live map and awareness feed.
* **Gamified Contribution & Badge System**:
  * +2 points for reporting incidents.
  * +10 points when an administrator verifies and approves a report.
  * Tiered Badges: **Bronze Scout** (0-19 pts), **Silver Navigator** (20-49 pts), **Gold Pioneer** (50-99 pts), **Platinum Sentinel** (100+ pts).
  * Global top contributors podium leaderboard.
* **Dual Database Architecture**:
  * **MySQL** production-ready profile (`application-mysql.properties`) with full schema DDL/DML.
  * **H2 In-Memory (MySQL-compatibility mode)** default dev profile for zero-configuration, instant plug-and-play evaluation.

---

## 🛠 Tech Stack

| Layer | Technologies |
| :--- | :--- |
| **Backend Framework** | Java 17+ (Java 24 verified), Spring Boot 3.3.4, Spring MVC |
| **Security & Auth** | Spring Security 6 (BCrypt password encoding, Role-based URL guards) |
| **Persistence / ORM** | Spring Data JPA, Hibernate ORM |
| **Database** | MySQL 8.x / H2 (MySQL Mode) |
| **Real-Time Messaging** | Spring WebSocket, STOMP protocol, SockJS client |
| **Geospatial & Maps** | Leaflet.js 1.9.4, OpenStreetMap tiles, Leaflet.markercluster 1.5.3, Nominatim Geocoding |
| **Frontend UI / UX** | HTML5, CSS3, JavaScript (ES6+), Bootstrap 5.3, FontAwesome 6, Google Fonts |
| **Data Analytics** | Chart.js 4.4.2 (Doughnut, Line, Bar charts) |
| **Build & Tooling** | Apache Maven 3.9.x |

---

## 🏛 Project Architecture & Package Structure

```
smart-nav-system/
├── pom.xml                               # Maven project dependencies & plugins
├── README.md                             # Complete project documentation
├── src/
│   ├── main/
│   │   ├── java/com/smartnav/
│   │   │   ├── SmartNavApplication.java  # Main application entry point
│   │   │   ├── config/                   # Configuration beans
│   │   │   ├── controller/               # MVC Views & REST Controllers
│   │   │   │   ├── WebViewController.java
│   │   │   │   ├── AuthRestController.java
│   │   │   │   ├── ReportRestController.java
│   │   │   │   ├── RouteRestController.java
│   │   │   │   ├── LeaderboardRestController.java
│   │   │   │   ├── AdminRestController.java
│   │   │   │   ├── NotificationRestController.java
│   │   │   │   └── CustomErrorController.java
│   │   │   ├── dto/                      # Data Transfer Objects
│   │   │   │   ├── ApiResponse.java
│   │   │   │   ├── AuthRequest.java & AuthResponse.java
│   │   │   │   ├── RegisterRequest.java
│   │   │   │   ├── ReportRequest.java & ReportResponse.java
│   │   │   │   ├── RouteCalculationRequest.java & Response.java
│   │   │   │   ├── RouteStep.java
│   │   │   │   ├── LeaderboardDto.java
│   │   │   │   ├── AnalyticsDto.java
│   │   │   │   └── UserProfileDto.java
│   │   │   ├── entity/                   # JPA Database Entities
│   │   │   │   ├── User.java
│   │   │   │   ├── Report.java
│   │   │   │   ├── Notification.java
│   │   │   │   ├── RouteRecord.java
│   │   │   │   ├── LeaderboardEntry.java
│   │   │   │   └── SystemSetting.java
│   │   │   ├── repository/               # Spring Data JPA Repositories
│   │   │   │   ├── UserRepository.java
│   │   │   │   ├── ReportRepository.java
│   │   │   │   ├── NotificationRepository.java
│   │   │   │   ├── RouteRecordRepository.java
│   │   │   │   ├── LeaderboardRepository.java
│   │   │   │   └── SystemSettingRepository.java
│   │   │   ├── security/                 # Spring Security Configuration
│   │   │   │   ├── SecurityConfig.java
│   │   │   │   ├── CustomUserDetailsService.java
│   │   │   │   └── SecurityUtils.java
│   │   │   ├── service/                  # Business & Algorithm Logic
│   │   │   │   ├── NavigationService.java    # Dynamic detour & obstacle avoidance
│   │   │   │   ├── ReportService.java        # Incident management & Haversine search
│   │   │   │   ├── UserService.java          # Authentication & profile management
│   │   │   │   ├── LeaderboardService.java    # Points, badge calculation & ranking
│   │   │   │   ├── NotificationService.java   # Notification alerts & dispatch
│   │   │   │   ├── AnalyticsService.java      # Chart.js telemetry aggregations
│   │   │   │   ├── FileStorageService.java    # Incident photo uploads
│   │   │   │   └── DataInitializer.java       # Automatic seed data loader
│   │   │   └── websocket/                # Real-Time WebSocket Broker
│   │   │       ├── WebSocketConfig.java
│   │   │       └── TrafficNotificationPublisher.java
│   │   └── resources/
│   │       ├── application.properties         # Main configuration
│   │       ├── application-dev.properties     # H2 MySQL-mode configuration
│   │       ├── application-mysql.properties   # Production MySQL configuration
│   │       ├── schema.sql                     # Normalized MySQL table schema
│   │       ├── data.sql                       # Seed data with dummy records
│   │       ├── static/
│   │       │   ├── css/styles.css             # Modern green & blue design system
│   │       │   ├── js/
│   │       │   │   ├── app.js                 # WebSocket STOMP client & toasts
│   │       │   │   ├── map.js                 # Leaflet interactive map & clustering
│   │       │   │   ├── routes.js              # Route planner & red congestion line
│   │       │   │   └── admin.js               # Chart.js dashboards & moderation
│   │       │   └── images/                    # Custom SVGs & brand icons
│   │       └── templates/                     # Thymeleaf HTML5 Views
│   │           ├── fragments/                 # Reusable navbar, footer, head
│   │           ├── index.html                 # Homepage with live preview
│   │           ├── about.html                 # Architecture & algorithm details
│   │           ├── contact.html               # Feedback & support
│   │           ├── login.html                 # Sign-in with one-click demo fill
│   │           ├── register.html              # Driver registration
│   │           ├── dashboard.html             # Driver personal dashboard
│   │           ├── map.html                   # Fullscreen interactive map
│   │           ├── report.html                # Incident submission form
│   │           ├── routes.html                # Intelligent route planner
│   │           ├── my-reports.html            # User submission history
│   │           ├── leaderboard.html           # Gamified contributor rankings
│   │           ├── notifications.html         # Live notification center
│   │           ├── profile.html               # Account profile & credentials
│   │           ├── admin-dashboard.html       # Admin widgets & Chart.js charts
│   │           ├── admin-reports.html         # Report moderation & CSV export
│   │           ├── admin-users.html           # Driver management & status toggle
│   │           ├── admin-analytics.html       # Telemetry trends & PDF export
│   │           ├── admin-settings.html        # System parameters
│   │           └── error/                     # 404 & 500 error pages
│   └── test/java/com/smartnav/
│       ├── SmartNavApplicationTests.java     # Context boot tests
│       └── NavigationServiceTest.java         # Obstacle avoidance & congestion tests
```

---

## ⚡ Quick Start & Run Instructions

### 1. Zero-Configuration Instant Evaluation Mode (Default: H2 in MySQL Mode)

The project includes an automatic in-memory MySQL-compatible database profile. No MySQL server installation is required to test or grade the project!

```bash
# Clone or navigate to the project directory:
cd /Users/hariomsharma/.gemini/antigravity-ide/scratch/smart-nav-system

# Run tests:
mvn test

# Run application:
mvn spring-boot:run
```

Once started, open your web browser at:
👉 **`http://localhost:8080`**

### 2. Running with External MySQL Database

To connect to a live MySQL 8.x instance:

1. Create the database in MySQL:
   ```sql
   CREATE DATABASE smart_nav_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```
2. Check or edit credentials in `src/main/resources/application-mysql.properties`:
   ```properties
   spring.datasource.url=jdbc:mysql://localhost:3306/smart_nav_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
   spring.datasource.username=root
   spring.datasource.password=root
   ```
3. Run with the MySQL profile activated:
   ```bash
   mvn spring-boot:run -Dspring-boot.run.profiles=mysql
   ```

---

## 🔑 Pre-Configured Demo Accounts

For grading and quick evaluation, the login page features **one-click demo login buttons**:

| Role | Email Address | Password | Permissions & Features |
| :--- | :--- | :--- | :--- |
| **Admin** | `admin@smartnav.com` | `admin123` | Full access to Admin Dashboard, Report Moderation, User Management, Analytics, Settings |
| **User (Driver)** | `john.doe@smartnav.com` | `user123` | Incident Reporting, Route Planner, Saved Routes, Personal Dashboard, Profile, Leaderboard |
| **User (Driver)** | `jane.smith@smartnav.com` | `user123` | Gold Pioneer Driver account with 75 contribution points |
| **Guest** | `guest@smartnav.com` | `guest123` | View-only access to Live Map and community statistics |

---

## 📡 REST API Reference

All REST endpoints return standardized JSON wrapped in `ApiResponse<T>`:

### Authentication
* `POST /api/auth/register` - Create new driver account
* `POST /api/auth/login` - Authenticate driver and create session
* `GET  /api/auth/user` - Retrieve currently authenticated user

### Traffic Reports
* `GET    /api/reports` - Retrieve reports (filter by `category`, `status`, `userId`, `startDate`, `endDate`)
* `GET    /api/reports/{id}` - Get report details
* `POST   /api/reports` - Submit report (JSON or `multipart/form-data` with photo)
* `DELETE /api/reports/{id}` - Delete report (admin or author only)
* `GET    /api/reports/nearby?lat={lat}&lng={lng}&radius={km}` - Find reports in radius using Haversine formula
* `GET    /api/reports/export/csv` - Download all reports as CSV file

### Intelligent Navigation
* `POST /api/routes/calculate` - Calculate shortest route, avoid approved roadblocks, compute congestion delays, return turn instructions and coordinates
* `POST /api/routes/save` - Save route to user profile
* `GET  /api/routes` - Retrieve user's saved routes

### Leaderboard & Contributor Gamification
* `GET /api/leaderboard` - Fetch ranked list of top contributors with badges and points
* `GET /api/leaderboard/my-rank` - Get authenticated user's current rank position

### Admin Operations
* `GET    /api/admin/reports` - Retrieve all reports with moderation filters
* `PUT    /api/admin/approve/{id}` - Approve report (publishes to map, awards +10 pts, broadcasts to WebSocket)
* `PUT    /api/admin/reject/{id}` - Reject report with reason
* `DELETE /api/admin/reports/{id}` - Delete fraudulent report permanently
* `GET    /api/admin/users` - List all users
* `PUT    /api/admin/users/{id}/toggle-status` - Toggle user active / suspended status
* `GET    /api/admin/analytics` - Aggregated metrics for Chart.js dashboards
* `GET    /api/admin/settings` & `POST /api/admin/settings` - System configuration parameters

### Real-Time WebSocket Topics
* Handshake endpoint: `/ws-traffic` (SockJS + STOMP)
* Topic `/topic/reports` - Broadcasts live new reports, approvals, and deletions
* Topic `/topic/notifications` - Broadcasts real-time traffic warnings to drivers

---

## 🧠 Intelligent Routing Engine (`NavigationService`)

The intelligent navigation logic works in four distinct phases:

1. **Hazard Corridor Scanning**:
   Using great-circle mathematics, it computes whether any approved roadblock (`ROAD_BLOCK`, `ACCIDENT`, `CONSTRUCTION`, `FLOOD`) lies within the travel corridor between origin $(lat_1, lon_1)$ and destination $(lat_2, lon_2)$.

2. **Obstacle Detour Waypoint Synthesis**:
   If an obstacle is detected and the `Avoid Blocked Roads` toggle is active, the algorithm synthesizes an orthogonal bypass vector offset ~700 meters perpendicular to the blockage:
   $$\vec{v}_{\text{bypass}} = \vec{p}_{\text{obstacle}} + \frac{[- \Delta lon, \Delta lat]}{\|\Delta\vec{v}\|} \cdot d_{\text{offset}}$$
   This bypass point is routed as an intermediate via-point, steering drivers around closed roads.

3. **Congestion Weighting**:
   If a `TRAFFIC_JAM` incident is present on the route, travel duration is multiplied by the configured congestion factor ($1.8\times$), and the delay is reported in minutes.

4. **Visual Congestion Red-Line**:
   The exact polyline coordinates within 600m of the traffic jam are extracted into `congestedCoordinates` and rendered as a vibrant **red dashed overlay** on top of the primary route.

---

## 📊 Database Schema (MySQL Compatible)

Normalized tables mapped via Spring Data JPA:
* **`users`**: id, name, email, password, role, phone, points, badge, status, created_at, updated_at
* **`reports`**: id, user_id, category, description, image, latitude, longitude, address, status, severity, created_at, updated_at
* **`notifications`**: id, user_id, title, message, type, is_read, created_at
* **`routes`**: id, user_id, source_name, source_lat, source_lng, dest_name, dest_lat, dest_lng, distance_km, duration_mins, route_geometry_json, is_congested, has_blocked_avoidance, created_at
* **`leaderboard`**: id, user_id, points, badge, rank_position, reports_submitted, reports_approved, updated_at
* **`system_settings`**: id, setting_key, setting_value, description

See `src/main/resources/schema.sql` and `src/main/resources/data.sql` for the complete SQL schema and initial seed data.

---

## 📄 License
This project is open-source and available under the [MIT License](LICENSE).
