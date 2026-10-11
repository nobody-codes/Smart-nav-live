package com.smartnav.service;

import com.smartnav.entity.*;
import com.smartnav.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ReportRepository reportRepository;
    private final NotificationRepository notificationRepository;
    private final RouteRecordRepository routeRecordRepository;
    private final LeaderboardRepository leaderboardRepository;
    private final SystemSettingRepository systemSettingRepository;
    private final PasswordEncoder passwordEncoder;
    private final LeaderboardService leaderboardService;

    public DataInitializer(UserRepository userRepository,
                           ReportRepository reportRepository,
                           NotificationRepository notificationRepository,
                           RouteRecordRepository routeRecordRepository,
                           LeaderboardRepository leaderboardRepository,
                           SystemSettingRepository systemSettingRepository,
                           PasswordEncoder passwordEncoder,
                           LeaderboardService leaderboardService) {
        this.userRepository = userRepository;
        this.reportRepository = reportRepository;
        this.notificationRepository = notificationRepository;
        this.routeRecordRepository = routeRecordRepository;
        this.leaderboardRepository = leaderboardRepository;
        this.systemSettingRepository = systemSettingRepository;
        this.passwordEncoder = passwordEncoder;
        this.leaderboardService = leaderboardService;
    }

    @Override
    public void run(String... args) {
        // Upsert standard demo accounts with properly encoded BCrypt passwords
        upsertUser("System Administrator", "admin@smartnav.com", "admin123", "ADMIN", "+91-98220-01100", 350, "Platinum Sentinel");
        upsertUser("Rahul Sharma", "john.doe@smartnav.com", "user123", "USER", "+91-98220-01101", 120, "Platinum Sentinel");
        upsertUser("Priya Deshmukh", "jane.smith@smartnav.com", "user123", "USER", "+91-98220-01102", 75, "Gold Pioneer");
        upsertUser("Amit Kulkarni", "alex.rivera@smartnav.com", "user123", "USER", "+91-98220-01103", 35, "Silver Navigator");
        upsertUser("Guest Explorer", "guest@smartnav.com", "guest123", "GUEST", "+91-98220-01104", 10, "Bronze Scout");

        if (reportRepository.count() == 0) {
            seedReportsAndData();
        }
    }

    private void upsertUser(String name, String email, String rawPass, String role, String phone, int points, String badge) {
        User user = userRepository.findByEmail(email).orElseGet(User::new);
        user.setName(name);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(rawPass));
        user.setRole(role);
        user.setPhone(phone);
        user.setPoints(points);
        user.setBadge(badge);
        user.setStatus("ACTIVE");
        User saved = userRepository.save(user);
        leaderboardService.syncUserToLeaderboard(saved, 0, 0);
    }

    private void seedReportsAndData() {
        User admin = userRepository.findByEmail("admin@smartnav.com").orElse(null);
        User rahul = userRepository.findByEmail("john.doe@smartnav.com").orElse(null);
        User priya = userRepository.findByEmail("jane.smith@smartnav.com").orElse(null);
        User amit = userRepository.findByEmail("alex.rivera@smartnav.com").orElse(null);
        User guest = userRepository.findByEmail("guest@smartnav.com").orElse(null);

        if (rahul == null || priya == null || amit == null) return;

        // 2. Seed Real-World Reports across major Indian cities
        List<Report> sampleReports = List.of(
                // --- PUNE ---
                createReport(rahul, "ROAD_BLOCK", "Emergency water main burst repair. Entire section barricaded for vehicular transit.",
                        "/images/incident-roadblock.svg", 18.5920, 73.7580, "Wakad Flyover & Hinjawadi Road Junction", "Pune", "Maharashtra", "India", "APPROVED", "CRITICAL"),

                createReport(priya, "TRAFFIC_JAM", "Heavy gridlock during rush hour due to ongoing metro barricades on JM Road.",
                        "/images/incident-traffic.svg", 18.5314, 73.8446, "Shivajinagar Junction & JM Road", "Pune", "Maharashtra", "India", "APPROVED", "HIGH"),

                createReport(rahul, "CONSTRUCTION", "Station circle road resurfacing work. Single operational lane causing bottleneck.",
                        "/images/incident-construction.svg", 18.5289, 73.8744, "Pune Junction Railway Station, Agarkar Nagar", "Pune", "Maharashtra", "India", "APPROVED", "MEDIUM"),

                createReport(amit, "BROKEN_SIGNAL", "Traffic light malfunction at Goodluck Chowk; manual traffic police directing flow.",
                        "/images/incident-signal.svg", 18.5246, 73.8415, "FC Road (Fergusson College Rd), Deccan Gymkhana", "Pune", "Maharashtra", "India", "APPROVED", "HIGH"),

                createReport(priya, "POTHOLE", "Deep asphalt crater on left lane near ICC Tech Park causing sudden brake checks.",
                        "/images/incident-construction.svg", 18.5308, 73.8289, "Senapati Bapat Road near JW Marriott", "Pune", "Maharashtra", "India", "APPROVED", "MEDIUM"),

                createReport(rahul, "ACCIDENT", "Multi-vehicle collision on highway ramp. Tow truck on scene, right lane blocked.",
                        "/images/incident-accident.svg", 18.4529, 73.8553, "Katraj Tunnel Bypass, NH 48", "Pune", "Maharashtra", "India", "APPROVED", "HIGH"),

                createReport(amit, "FLOOD", "Waterlogging in underpass following storm. Low-clearance hatchbacks advised to divert.",
                        "/images/incident-flood.svg", 18.5074, 73.7744, "Chandni Chowk Underpass, Kothrud", "Pune", "Maharashtra", "India", "APPROVED", "HIGH"),

                createReport(rahul, "POLICE_CHECKPOINT", "Transit checkpoint and commercial vehicle documentation verification.",
                        "/images/incident-police.svg", 18.5385, 73.8340, "Ganeshkhind Road near E-Square Chowk", "Pune", "Maharashtra", "India", "APPROVED", "LOW"),

                createReport(priya, "VEHICLE_BREAKDOWN", "Stalled PMPML transit bus blocking roundabout exit.",
                        "/images/incident-accident.svg", 18.5913, 73.7389, "Shivaji Chowk, Hinjewadi Phase 1", "Pune", "Maharashtra", "India", "APPROVED", "MEDIUM"),

                createReport(amit, "FIRE", "Small trash fire emitting heavy smoke on highway berm. Fire department en route.",
                        "/images/incident-roadblock.svg", 18.5089, 73.9260, "Magarpatta Bypass, Hadapsar", "Pune", "Maharashtra", "India", "PENDING", "HIGH"),

                createReport(rahul, "ACCIDENT", "Minor fender bender on curbside, slight slowdown.",
                        "/images/incident-accident.svg", 18.5679, 73.9143, "Viman Nagar Road near Phoenix Marketcity", "Pune", "Maharashtra", "India", "PENDING", "LOW"),

                // --- MUMBAI ---
                createReport(priya, "TRAFFIC_JAM", "Western Express Highway standstill heading southbound towards Bandra. Average 8 km/h.",
                        "/images/incident-traffic.svg", 19.1136, 72.8697, "Western Express Highway near Andheri Flyover", "Mumbai", "Maharashtra", "India", "APPROVED", "HIGH"),

                createReport(amit, "POLICE_CHECKPOINT", "Routine security inspection and FASTag lane regulation.",
                        "/images/incident-police.svg", 19.0330, 72.8160, "Bandra Toll Plaza, Bandra-Worli Sea Link", "Mumbai", "Maharashtra", "India", "APPROVED", "LOW"),

                createReport(rahul, "FLOOD", "Severe water logging in circle underpass. Heavy vehicles only.",
                        "/images/incident-flood.svg", 19.0434, 72.8634, "Sion Circle Underpass, Sion East", "Mumbai", "Maharashtra", "India", "APPROVED", "CRITICAL"),

                // --- BENGALURU ---
                createReport(priya, "TRAFFIC_JAM", "Heavy gridlock connecting Outer Ring Road and Electronic City flyover.",
                        "/images/incident-traffic.svg", 12.9172, 77.6228, "Central Silk Board Junction, Hosur Road", "Bengaluru", "Karnataka", "India", "APPROVED", "CRITICAL"),

                createReport(amit, "POTHOLE", "Multiple large potholes near Sony World Signal.",
                        "/images/incident-construction.svg", 12.9352, 77.6245, "80 Feet Road, 4th Block Koramangala", "Bengaluru", "Karnataka", "India", "APPROVED", "MEDIUM"),

                // --- DELHI ---
                createReport(rahul, "ROAD_BLOCK", "Inner Circle utility trenching; vehicular traffic diverted to Outer Circle.",
                        "/images/incident-roadblock.svg", 28.6315, 77.2167, "Connaught Place Inner Circle, Radial Rd 2", "Delhi", "Delhi", "India", "APPROVED", "HIGH"),

                createReport(priya, "TRAFFIC_JAM", "Heavy congestion near hospital gates on Ring Road.",
                        "/images/incident-traffic.svg", 28.5672, 77.2100, "AIIMS Flyover, Ring Road", "Delhi", "Delhi", "India", "APPROVED", "HIGH"),

                // --- HYDERABAD ---
                createReport(amit, "TRAFFIC_JAM", "Peak hour software corridor standstill towards Mindspace.",
                        "/images/incident-traffic.svg", 17.4504, 78.3808, "Cyber Towers Junction, Hitec City", "Hyderabad", "Telangana", "India", "APPROVED", "HIGH")
        );
        reportRepository.saveAll(sampleReports);

        // 3. Seed Leaderboard
        if (admin != null) leaderboardRepository.save(new LeaderboardEntry(admin, 350, "Platinum Sentinel", 1, 35, 35));
        leaderboardRepository.save(new LeaderboardEntry(rahul, 120, "Platinum Sentinel", 2, 15, 12));
        leaderboardRepository.save(new LeaderboardEntry(priya, 75, "Gold Pioneer", 3, 9, 8));
        leaderboardRepository.save(new LeaderboardEntry(amit, 35, "Silver Navigator", 4, 5, 4));
        if (guest != null) leaderboardRepository.save(new LeaderboardEntry(guest, 10, "Bronze Scout", 5, 1, 1));

        // 4. Seed Notifications
        notificationRepository.save(new Notification(rahul, "Report Approved!", "Your incident report for Baner Road, Pune was verified. +10 Points!", "REPORT_STATUS"));
        notificationRepository.save(new Notification(rahul, "Traffic Alert Near You", "Critical road block reported on Baner Road. Navigation will detour via Pashan-Sus Rd.", "INCIDENT_ALERT"));
        notificationRepository.save(new Notification(priya, "Badge Unlocked: Gold Pioneer", "You reached 75 contribution points and earned the Gold Pioneer badge.", "BADGE_EARNED"));
        notificationRepository.save(new Notification(null, "System Advisory", "Real-time WebSocket telemetry is active. Live OpenStreetMap routing avoids approved hazards.", "SYSTEM"));

        // 5. Seed Routes
        routeRecordRepository.save(new RouteRecord(
                rahul,
                "Pune Railway Station", 18.5289, 73.8744,
                "Hinjewadi Phase 1", 18.5913, 73.7389,
                18.3, 32, "[]", false, true
        ));
        routeRecordRepository.save(new RouteRecord(
                priya,
                "Mumbai Airport (T2)", 19.0974, 72.8744,
                "Bandra Kurla Complex (BKC)", 19.0660, 72.8687,
                8.5, 18, "[]", true, false
        ));

        // 6. Seed System Settings
        systemSettingRepository.save(new SystemSetting("auto_approve_reports", "false", "Automatically approve user submitted incident reports"));
        systemSettingRepository.save(new SystemSetting("congestion_penalty_factor", "1.8", "Time multiplier for roads with traffic jams"));
        systemSettingRepository.save(new SystemSetting("default_alert_radius_km", "5.0", "Radius in km to trigger nearby incident alerts"));
        systemSettingRepository.save(new SystemSetting("max_daily_reports_per_user", "20", "Rate limit for crowdsourced submissions per user per day"));
    }

    private Report createReport(User user, String category, String desc, String img, double lat, double lng,
                                String addr, String city, String state, String country, String status, String severity) {
        Report r = new Report(user, category, desc, img, lat, lng, addr, city, state, country, severity);
        r.setStatus(status);
        return r;
    }
}
