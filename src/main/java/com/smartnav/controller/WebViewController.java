package com.smartnav.controller;

import com.smartnav.entity.User;
import com.smartnav.security.SecurityUtils;
import com.smartnav.service.AnalyticsService;
import com.smartnav.service.LeaderboardService;
import com.smartnav.service.ReportService;
import com.smartnav.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.Optional;

@Controller
public class WebViewController {

    private final SecurityUtils securityUtils;
    private final ReportService reportService;
    private final UserService userService;
    private final LeaderboardService leaderboardService;
    private final AnalyticsService analyticsService;

    public WebViewController(SecurityUtils securityUtils,
                             ReportService reportService,
                             UserService userService,
                             LeaderboardService leaderboardService,
                             AnalyticsService analyticsService) {
        this.securityUtils = securityUtils;
        this.reportService = reportService;
        this.userService = userService;
        this.leaderboardService = leaderboardService;
        this.analyticsService = analyticsService;
    }

    @ModelAttribute
    public void addGlobalAttributes(Model model) {
        Optional<User> currentUser = securityUtils.getCurrentUser();
        model.addAttribute("currentUser", currentUser.orElse(null));
        model.addAttribute("isAuthenticated", securityUtils.isAuthenticated());
        model.addAttribute("isAdmin", securityUtils.isAdmin());
    }

    // Public Pages
    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("stats", analyticsService.getAdminAnalytics());
        model.addAttribute("recentReports", reportService.getApprovedReports().stream().limit(6).toList());
        return "index";
    }

    @GetMapping("/about")
    public String about() {
        return "about";
    }

    @GetMapping("/contact")
    public String contact() {
        return "contact";
    }

    @GetMapping("/login")
    public String login() {
        if (securityUtils.isAuthenticated()) {
            return securityUtils.isAdmin() ? "redirect:/admin/dashboard" : "redirect:/dashboard";
        }
        return "login";
    }

    @GetMapping("/admin/login")
    public String adminLogin() {
        if (securityUtils.isAuthenticated() && securityUtils.isAdmin()) {
            return "redirect:/admin/dashboard";
        }
        return "admin-login";
    }

    @GetMapping("/register")
    public String register() {
        if (securityUtils.isAuthenticated()) {
            return "redirect:/dashboard";
        }
        return "register";
    }

    @GetMapping("/forgot-password")
    public String forgotPassword() {
        if (securityUtils.isAuthenticated()) {
            return "redirect:/dashboard";
        }
        return "forgot-password";
    }

    @GetMapping("/reset-password")
    public String resetPassword(@org.springframework.web.bind.annotation.RequestParam(required = false) String token, Model model) {
        if (token == null || token.trim().isEmpty() || userService.validateResetToken(token.trim()).isEmpty()) {
            model.addAttribute("tokenValid", false);
            model.addAttribute("errorMessage", "Password reset link is invalid or has expired. Please request a new link.");
        } else {
            model.addAttribute("tokenValid", true);
            model.addAttribute("token", token.trim());
        }
        return "reset-password";
    }

    @GetMapping("/logout")
    public String logout(jakarta.servlet.http.HttpServletRequest request) {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
        jakarta.servlet.http.HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return "redirect:/login?logout=true";
    }

    @GetMapping("/unauthorized")
    public String unauthorized(Model model) {
        model.addAttribute("statusCode", 403);
        model.addAttribute("errorTitle", "403 Forbidden - Access Denied");
        model.addAttribute("errorMessage", "You do not have administrative privileges to access this area.");
        return "error/403";
    }

    // User Pages
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        Optional<User> userOpt = securityUtils.getCurrentUser();
        if (userOpt.isEmpty()) return "redirect:/login";

        User user = userOpt.get();
        model.addAttribute("profile", userService.getUserProfileDto(user.getId()));
        model.addAttribute("userReports", reportService.getUserReports(user.getId()).stream().limit(5).toList());
        model.addAttribute("recentIncidents", reportService.getApprovedReports().stream().limit(5).toList());
        model.addAttribute("userRank", leaderboardService.getUserRank(user.getId()));
        return "dashboard";
    }

    @GetMapping("/map")
    public String liveMap(Model model) {
        model.addAttribute("approvedReports", reportService.getApprovedReports());
        return "map";
    }

    @GetMapping("/report")
    public String reportIncident() {
        return "report";
    }

    @GetMapping("/routes")
    public String routePlanner() {
        return "routes";
    }

    @GetMapping("/my-reports")
    public String myReports(Model model) {
        Optional<User> userOpt = securityUtils.getCurrentUser();
        if (userOpt.isEmpty()) return "redirect:/login";

        model.addAttribute("reports", reportService.getUserReports(userOpt.get().getId()));
        return "my-reports";
    }

    @GetMapping("/leaderboard")
    public String leaderboard(Model model) {
        model.addAttribute("leaderboard", leaderboardService.getLeaderboard());
        Optional<User> userOpt = securityUtils.getCurrentUser();
        userOpt.ifPresent(user -> model.addAttribute("myRank", leaderboardService.getUserRank(user.getId())));
        return "leaderboard";
    }

    @GetMapping("/notifications")
    public String notifications() {
        return "notifications";
    }

    @GetMapping("/profile")
    public String profile(Model model) {
        Optional<User> userOpt = securityUtils.getCurrentUser();
        if (userOpt.isEmpty()) return "redirect:/login";

        model.addAttribute("profile", userService.getUserProfileDto(userOpt.get().getId()));
        return "profile";
    }

    // Admin Pages
    @GetMapping("/admin/dashboard")
    public String adminDashboard(Model model) {
        model.addAttribute("analytics", analyticsService.getAdminAnalytics());
        model.addAttribute("recentReports", reportService.getAllReports().stream().limit(8).toList());
        return "admin-dashboard";
    }

    @GetMapping("/admin/reports")
    public String adminReports(Model model) {
        model.addAttribute("reports", reportService.getAllReports());
        return "admin-reports";
    }

    @GetMapping("/admin/map")
    public String adminMap(Model model) {
        model.addAttribute("reports", reportService.getAllReports());
        return "admin-map";
    }

    @GetMapping("/admin/users")
    public String adminUsers(Model model) {
        model.addAttribute("users", userService.getAllUserDtos());
        return "admin-users";
    }

    @GetMapping("/admin/analytics")
    public String adminAnalytics(Model model) {
        model.addAttribute("analytics", analyticsService.getAdminAnalytics());
        return "admin-analytics";
    }

    @GetMapping("/admin/settings")
    public String adminSettings() {
        return "admin-settings";
    }
}
