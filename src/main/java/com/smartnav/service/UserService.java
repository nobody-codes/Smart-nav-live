package com.smartnav.service;

import com.smartnav.dto.RegisterRequest;
import com.smartnav.dto.ResetPasswordRequest;
import com.smartnav.dto.UserDto;
import com.smartnav.dto.UserProfileDto;
import com.smartnav.entity.User;
import com.smartnav.repository.ReportRepository;
import com.smartnav.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final ReportRepository reportRepository;
    private final PasswordEncoder passwordEncoder;
    private final LeaderboardService leaderboardService;

    public UserService(UserRepository userRepository,
                       ReportRepository reportRepository,
                       @Lazy PasswordEncoder passwordEncoder,
                       LeaderboardService leaderboardService) {
        this.userRepository = userRepository;
        this.reportRepository = reportRepository;
        this.passwordEncoder = passwordEncoder;
        this.leaderboardService = leaderboardService;
    }

    public void validatePasswordStrength(String password) {
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("Password must contain at least 8 characters.");
        }
        boolean hasUpper = false;
        boolean hasLower = false;
        boolean hasDigit = false;
        boolean hasSpecial = false;
        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) hasUpper = true;
            else if (Character.isLowerCase(c)) hasLower = true;
            else if (Character.isDigit(c)) hasDigit = true;
            else hasSpecial = true;
        }
        if (!hasUpper || !hasLower || !hasDigit || !hasSpecial) {
            throw new IllegalArgumentException("Password must contain at least one uppercase letter, one lowercase letter, one number, and one special character.");
        }
    }

    @Transactional
    public User registerUser(RegisterRequest request) {
        if (request.getName() == null || request.getName().trim().length() < 2) {
            throw new IllegalArgumentException("Full name must be at least 2 characters.");
        }
        if (request.getEmail() == null || !request.getEmail().contains("@")) {
            throw new IllegalArgumentException("Please provide a valid email address.");
        }
        String normalizedEmail = request.getEmail().toLowerCase().trim();
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new IllegalArgumentException("Email already registered.");
        }

        validatePasswordStrength(request.getPassword());

        if (request.getConfirmPassword() != null && !request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match.");
        }

        // Always enforce USER role; NEVER allow admin registration through public registration flow
        User user = new User(
                request.getName().trim(),
                normalizedEmail,
                passwordEncoder.encode(request.getPassword()),
                "USER",
                request.getPhone() != null ? request.getPhone().trim() : null
        );
        user.setStatus("ACTIVE");
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        User saved = userRepository.save(user);
        leaderboardService.syncUserToLeaderboard(saved, 0, 0);
        log.info("New USER successfully registered: {}", saved.getEmail());
        return saved;
    }

    public Optional<User> findByEmail(String email) {
        if (email == null) return Optional.empty();
        return userRepository.findByEmail(email.toLowerCase().trim());
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public List<User> getAllUsers() {
        return userRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<UserDto> getAllUserDtos() {
        return userRepository.findAllByOrderByCreatedAtDesc().stream().map(u -> {
            UserDto dto = UserDto.fromEntity(u);
            long total = reportRepository.findByUserIdOrderByCreatedAtDesc(u.getId()).size();
            long approved = reportRepository.findByUserIdOrderByCreatedAtDesc(u.getId()).stream()
                    .filter(r -> "APPROVED".equals(r.getStatus()))
                    .count();
            dto.setTotalReports(total);
            dto.setApprovedReports(approved);
            return dto;
        }).toList();
    }

    @Transactional
    public User updateProfile(Long userId, String name, String phone, String currentPassword, String newPassword, String confirmNewPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));

        if (name != null && !name.trim().isEmpty()) {
            user.setName(name.trim());
        }
        if (phone != null) {
            user.setPhone(phone.trim());
        }

        if (newPassword != null && !newPassword.trim().isEmpty()) {
            if (currentPassword == null || !passwordEncoder.matches(currentPassword, user.getPassword())) {
                throw new IllegalArgumentException("Current password is incorrect.");
            }
            validatePasswordStrength(newPassword.trim());
            if (!newPassword.trim().equals(confirmNewPassword)) {
                throw new IllegalArgumentException("New passwords do not match.");
            }
            user.setPassword(passwordEncoder.encode(newPassword.trim()));
        }

        return userRepository.save(user);
    }

    @Transactional
    public User updateProfile(Long userId, String name, String phone, String newPassword) {
        return updateProfile(userId, name, phone, null, newPassword, newPassword);
    }

    @Transactional
    public String initiatePasswordReset(String email, String appBaseUrl) {
        if (email == null || email.trim().isEmpty()) {
            return null;
        }
        Optional<User> userOpt = userRepository.findByEmail(email.toLowerCase().trim());
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            String token = UUID.randomUUID().toString();
            user.setResetPasswordToken(token);
            user.setResetPasswordExpiresAt(LocalDateTime.now().plusMinutes(15));
            userRepository.save(user);

            String base = (appBaseUrl != null && !appBaseUrl.isEmpty()) ? appBaseUrl : "http://localhost:8080";
            String resetUrl = base + "/reset-password?token=" + token;
            log.info("============================================================");
            log.info(" [PASSWORD RESET DISPATCH]");
            log.info(" User: {}", user.getEmail());
            log.info(" Reset Token: {}", token);
            log.info(" Reset Link: {}", resetUrl);
            log.info(" Expiration: 15 minutes");
            log.info("============================================================");
            return token;
        }
        return null;
    }

    public Optional<User> validateResetToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return Optional.empty();
        }
        Optional<User> userOpt = userRepository.findByResetPasswordToken(token.trim());
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (user.getResetPasswordExpiresAt() != null && user.getResetPasswordExpiresAt().isAfter(LocalDateTime.now())) {
                return Optional.of(user);
            }
        }
        return Optional.empty();
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        if (request.getToken() == null || request.getToken().trim().isEmpty()) {
            throw new IllegalArgumentException("Password reset token is required.");
        }
        User user = validateResetToken(request.getToken())
                .orElseThrow(() -> new IllegalArgumentException("Password reset link is invalid or has expired. Please request a new one."));

        validatePasswordStrength(request.getNewPassword());
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match.");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setResetPasswordToken(null);
        user.setResetPasswordExpiresAt(null);
        userRepository.save(user);
        log.info("Password successfully updated via reset token for: {}", user.getEmail());
    }

    @Transactional
    public User toggleUserStatus(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));

        if ("ADMIN".equals(user.getRole()) || Long.valueOf(1).equals(user.getId())) {
            throw new IllegalStateException("Cannot suspend an administrator account.");
        }

        user.setStatus("ACTIVE".equalsIgnoreCase(user.getStatus()) ? "SUSPENDED" : "ACTIVE");
        return userRepository.save(user);
    }

    public UserProfileDto getUserProfileDto(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));

        UserProfileDto dto = new UserProfileDto();
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setPhone(user.getPhone());
        dto.setRole(user.getRole());
        dto.setPoints(user.getPoints());
        dto.setBadge(user.getBadge());
        dto.setStatus(user.getStatus());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setLastLogin(user.getLastLogin());

        long total = reportRepository.findByUserIdOrderByCreatedAtDesc(userId).size();
        long approved = reportRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .filter(r -> "APPROVED".equals(r.getStatus()))
                .count();

        dto.setTotalReports(total);
        dto.setApprovedReports(approved);

        return dto;
    }
}
