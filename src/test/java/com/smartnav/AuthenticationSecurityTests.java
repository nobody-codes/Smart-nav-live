package com.smartnav;

import com.smartnav.dto.RegisterRequest;
import com.smartnav.dto.UserDto;
import com.smartnav.dto.UserProfileDto;
import com.smartnav.entity.User;
import com.smartnav.repository.UserRepository;
import com.smartnav.security.LoginAttemptService;
import com.smartnav.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class AuthenticationSecurityTests {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LoginAttemptService loginAttemptService;

    @Test
    @DisplayName("Verify BCrypt password encoding works for admin123 and user123")
    void testBcryptEncoding() {
        String adminHash = passwordEncoder.encode("admin123");
        String userHash = passwordEncoder.encode("user123");

        assertTrue(passwordEncoder.matches("admin123", adminHash));
        assertTrue(passwordEncoder.matches("user123", userHash));
        assertFalse(passwordEncoder.matches("wrongpass", adminHash));
    }

    @Test
    @DisplayName("Verify Seeded Admin Credentials Match in Database")
    void testSeededAdminCredentials() {
        User admin = userRepository.findByEmail("admin@smartnav.com").orElse(null);
        assertNotNull(admin, "Admin account must be seeded");
        assertEquals("ADMIN", admin.getRole());
        assertEquals("ACTIVE", admin.getStatus());
        assertTrue(passwordEncoder.matches("admin123", admin.getPassword()), "Admin password hash must match admin123");
    }

    @Test
    @DisplayName("Verify Seeded User Credentials Match in Database")
    void testSeededUserCredentials() {
        User user = userRepository.findByEmail("john.doe@smartnav.com").orElse(null);
        assertNotNull(user, "User account must be seeded");
        assertEquals("USER", user.getRole());
        assertEquals("ACTIVE", user.getStatus());
        assertTrue(passwordEncoder.matches("user123", user.getPassword()), "User password hash must match user123");
    }

    @Test
    @DisplayName("Verify Registration forces USER role and hashes password with BCrypt")
    void testRegistrationEnforcesUserRole() {
        RegisterRequest request = new RegisterRequest();
        request.setName("Security Tester");
        request.setEmail("security.test@example.com");
        request.setPhone("+91-99999-88888");
        request.setPassword("StrongP@ss123");
        request.setConfirmPassword("StrongP@ss123");
        request.setAcceptTerms(true);

        User registered = userService.registerUser(request);

        assertNotNull(registered.getId());
        assertEquals("USER", registered.getRole(), "Public registration must strictly assign USER role");
        assertEquals("ACTIVE", registered.getStatus());
        assertNotEquals("StrongP@ss123", registered.getPassword(), "Plain text password must NEVER be stored");
        assertTrue(passwordEncoder.matches("StrongP@ss123", registered.getPassword()), "Password must be BCrypt encoded");
    }

    @Test
    @DisplayName("Verify Duplicate Email Registration is Rejected")
    void testDuplicateEmailRejected() {
        RegisterRequest request = new RegisterRequest();
        request.setName("Duplicate Tester");
        request.setEmail("admin@smartnav.com"); // already registered
        request.setPassword("StrongP@ss123");
        request.setConfirmPassword("StrongP@ss123");
        request.setAcceptTerms(true);

        assertThrows(IllegalArgumentException.class, () -> userService.registerUser(request));
    }

    @Test
    @DisplayName("Verify Weak Password Registration is Rejected")
    void testWeakPasswordRejected() {
        RegisterRequest request = new RegisterRequest();
        request.setName("Weak Pass Tester");
        request.setEmail("weak.pass@example.com");
        request.setPassword("weak"); // Too short, no upper, no digit, no special
        request.setConfirmPassword("weak");
        request.setAcceptTerms(true);

        assertThrows(IllegalArgumentException.class, () -> userService.registerUser(request));
    }

    @Test
    @DisplayName("Verify Password Confirmation Mismatch is Rejected")
    void testPasswordMismatchRejected() {
        RegisterRequest request = new RegisterRequest();
        request.setName("Mismatch Tester");
        request.setEmail("mismatch@example.com");
        request.setPassword("StrongP@ss123");
        request.setConfirmPassword("DifferentP@ss456");
        request.setAcceptTerms(true);

        assertThrows(IllegalArgumentException.class, () -> userService.registerUser(request));
    }

    @Test
    @DisplayName("Verify Primary Admin Account Cannot be Suspended")
    void testAdminCannotBeSuspended() {
        User admin = userRepository.findByEmail("admin@smartnav.com").orElseThrow();
        assertThrows(IllegalStateException.class, () -> userService.toggleUserStatus(admin.getId()));
    }

    @Test
    @DisplayName("Verify Normal User Can be Suspended and Reactivated by Admin")
    void testUserSuspensionToggle() {
        User user = userRepository.findByEmail("john.doe@smartnav.com").orElseThrow();
        assertEquals("ACTIVE", user.getStatus());

        User suspended = userService.toggleUserStatus(user.getId());
        assertEquals("SUSPENDED", suspended.getStatus());

        User reactivated = userService.toggleUserStatus(user.getId());
        assertEquals("ACTIVE", reactivated.getStatus());
    }

    @Test
    @DisplayName("Verify Password Reset Token Generation and Expiration Logic")
    void testPasswordResetWorkflow() {
        String token = userService.initiatePasswordReset("john.doe@smartnav.com", "http://localhost:8080");
        assertNotNull(token);

        User user = userRepository.findByEmail("john.doe@smartnav.com").orElseThrow();
        assertEquals(token, user.getResetPasswordToken());
        assertNotNull(user.getResetPasswordExpiresAt());
        assertTrue(userService.validateResetToken(token).isPresent());

        // Invalid token test
        assertFalse(userService.validateResetToken("invalid-nonexistent-token").isPresent());

        // Test complete password reset
        com.smartnav.dto.ResetPasswordRequest resetReq = new com.smartnav.dto.ResetPasswordRequest();
        resetReq.setToken(token);
        resetReq.setNewPassword("NewSecur3P@ss");
        resetReq.setConfirmPassword("NewSecur3P@ss");
        userService.resetPassword(resetReq);

        User updatedUser = userRepository.findByEmail("john.doe@smartnav.com").orElseThrow();
        assertTrue(passwordEncoder.matches("NewSecur3P@ss", updatedUser.getPassword()));
        assertNull(updatedUser.getResetPasswordToken(), "Token must be invalidated upon use");
    }

    @Test
    @DisplayName("Verify UserDto Does Not Expose Password")
    void testUserDtoSecurity() {
        User admin = userRepository.findByEmail("admin@smartnav.com").orElseThrow();
        UserDto dto = UserDto.fromEntity(admin);

        assertEquals(admin.getId(), dto.getId());
        assertEquals(admin.getEmail(), dto.getEmail());
        assertEquals(admin.getRole(), dto.getRole());
        // UserDto has no password field at all
    }

    @Test
    @DisplayName("Verify Login Attempt Limiter Blocks After 5 Failures")
    void testLoginAttemptRateLimiter() {
        String testEmail = "bruteforce@smartnav.com";
        assertFalse(loginAttemptService.isBlocked(testEmail));

        for (int i = 0; i < 5; i++) {
            loginAttemptService.loginFailed(testEmail);
        }

        assertTrue(loginAttemptService.isBlocked(testEmail), "Account should be locked after 5 failed attempts");

        loginAttemptService.loginSucceeded(testEmail);
        assertFalse(loginAttemptService.isBlocked(testEmail), "Successful login should reset the attempt counter");
    }
}
