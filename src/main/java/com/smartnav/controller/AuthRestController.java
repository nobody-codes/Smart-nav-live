package com.smartnav.controller;

import com.smartnav.dto.*;
import com.smartnav.entity.User;
import com.smartnav.security.LoginAttemptService;
import com.smartnav.security.SecurityUtils;
import com.smartnav.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthRestController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final SecurityUtils securityUtils;
    private final LoginAttemptService loginAttemptService;

    public AuthRestController(UserService userService,
                              AuthenticationManager authenticationManager,
                              SecurityUtils securityUtils,
                              LoginAttemptService loginAttemptService) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
        this.securityUtils = securityUtils;
        this.loginAttemptService = loginAttemptService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        try {
            User registered = userService.registerUser(request);
            AuthResponse response = new AuthResponse(
                    registered.getId(),
                    registered.getName(),
                    registered.getEmail(),
                    registered.getRole(),
                    registered.getPoints(),
                    registered.getBadge(),
                    "/login?registered=true"
            );
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.ok("Registration successful. Please log in.", response));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Registration failed. Please verify your details."));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody AuthRequest request,
                                                           HttpServletRequest httpRequest) {
        String email = request.getEmail() != null ? request.getEmail().toLowerCase().trim() : "";

        if (loginAttemptService.isBlocked(email)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(ApiResponse.error("Too many failed login attempts. Please try again after 15 minutes."));
        }

        Optional<User> existingUserOpt = userService.findByEmail(email);
        if (existingUserOpt.isPresent()) {
            User user = existingUserOpt.get();
            if ("SUSPENDED".equalsIgnoreCase(user.getStatus())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error("Your account is currently suspended. Please contact the administrator."));
            }
            if ("DISABLED".equalsIgnoreCase(user.getStatus())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error("Your account has been disabled. Please contact the administrator."));
            }
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.getPassword())
            );

            loginAttemptService.loginSucceeded(email);

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);

            HttpSession session = httpRequest.getSession(true);
            session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);

            Optional<User> userOpt = userService.findByEmail(email);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Invalid email or password."));
            }

            User user = userOpt.get();
            user.setLastLogin(LocalDateTime.now());
            userService.findById(user.getId()); // persist last login

            String redirectUrl = "ADMIN".equals(user.getRole()) ? "/admin/dashboard" : "/dashboard";

            AuthResponse response = new AuthResponse(
                    user.getId(),
                    user.getName(),
                    user.getEmail(),
                    user.getRole(),
                    user.getPoints(),
                    user.getBadge(),
                    redirectUrl
            );

            return ResponseEntity.ok(ApiResponse.ok("Login successful", response));
        } catch (LockedException e) {
            loginAttemptService.loginFailed(email);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Your account is currently suspended. Please contact the administrator."));
        } catch (DisabledException e) {
            loginAttemptService.loginFailed(email);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Your account has been disabled. Please contact the administrator."));
        } catch (BadCredentialsException e) {
            loginAttemptService.loginFailed(email);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Invalid email or password."));
        } catch (Exception e) {
            loginAttemptService.loginFailed(email);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Invalid email or password."));
        }
    }

    @PostMapping("/admin/login")
    public ResponseEntity<ApiResponse<AuthResponse>> adminLogin(@Valid @RequestBody AuthRequest request,
                                                                HttpServletRequest httpRequest) {
        String email = request.getEmail() != null ? request.getEmail().toLowerCase().trim() : "";

        if (loginAttemptService.isBlocked(email)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(ApiResponse.error("Too many failed attempts. Account temporarily locked for 15 minutes."));
        }

        Optional<User> existingUserOpt = userService.findByEmail(email);
        if (existingUserOpt.isPresent()) {
            User user = existingUserOpt.get();
            if ("SUSPENDED".equalsIgnoreCase(user.getStatus())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error("Your account is currently suspended. Please contact the administrator."));
            }
            if ("DISABLED".equalsIgnoreCase(user.getStatus())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error("Your account has been disabled. Please contact the administrator."));
            }
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.getPassword())
            );

            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

            if (!isAdmin) {
                loginAttemptService.loginFailed(email);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error("Admin access required."));
            }

            loginAttemptService.loginSucceeded(email);

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);

            HttpSession session = httpRequest.getSession(true);
            session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);

            User user = userService.findByEmail(email).orElseThrow();
            user.setLastLogin(LocalDateTime.now());

            AuthResponse response = new AuthResponse(
                    user.getId(),
                    user.getName(),
                    user.getEmail(),
                    user.getRole(),
                    user.getPoints(),
                    user.getBadge(),
                    "/admin/dashboard"
            );

            return ResponseEntity.ok(ApiResponse.ok("Administrator authentication successful", response));
        } catch (LockedException e) {
            loginAttemptService.loginFailed(email);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Your account is currently suspended. Please contact the administrator."));
        } catch (DisabledException e) {
            loginAttemptService.loginFailed(email);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Your account has been disabled. Please contact the administrator."));
        } catch (BadCredentialsException e) {
            loginAttemptService.loginFailed(email);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Invalid email or password."));
        } catch (Exception e) {
            loginAttemptService.loginFailed(email);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Invalid email or password."));
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Map<String, String>>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request,
                                                                           HttpServletRequest httpRequest) {
        String baseUrl = httpRequest.getScheme() + "://" + httpRequest.getServerName() + ":" + httpRequest.getServerPort();
        String token = userService.initiatePasswordReset(request.getEmail(), baseUrl);

        // Security requirement: Do not reveal whether an email exists
        String message = "If an account with that email is registered, password reset instructions have been dispatched.";
        Map<String, String> data = (token != null)
                ? Map.of("status", "DISPATCHED", "previewUrl", baseUrl + "/reset-password?token=" + token)
                : Map.of("status", "DISPATCHED");

        return ResponseEntity.ok(ApiResponse.ok(message, data));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        try {
            userService.resetPassword(request);
            return ResponseEntity.ok(ApiResponse.ok("Password updated successfully. You can now log in.", null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to reset password. Please request a new link."));
        }
    }

    @PostMapping("/profile")
    public ResponseEntity<ApiResponse<UserDto>> updateProfile(@RequestBody Map<String, String> body) {
        Optional<User> currentUserOpt = securityUtils.getCurrentUser();
        if (currentUserOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Not authenticated"));
        }
        User currentUser = currentUserOpt.get();

        String name = body.get("name");
        String phone = body.get("phone");
        String currentPassword = body.get("currentPassword");
        String newPassword = body.get("newPassword");
        String confirmNewPassword = body.get("confirmNewPassword");

        try {
            User updated = userService.updateProfile(currentUser.getId(), name, phone, currentPassword, newPassword, confirmNewPassword);
            return ResponseEntity.ok(ApiResponse.ok("Profile updated successfully", UserDto.fromEntity(updated)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/user")
    public ResponseEntity<ApiResponse<UserDto>> getCurrentUser() {
        Optional<User> userOpt = securityUtils.getCurrentUser();
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Not authenticated"));
        }
        return ResponseEntity.ok(ApiResponse.ok(UserDto.fromEntity(userOpt.get())));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> apiLogout(HttpServletRequest request) {
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return ResponseEntity.ok(ApiResponse.ok("Logged out successfully.", null));
    }
}
