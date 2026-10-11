package com.smartnav.security;

import com.smartnav.entity.User;
import com.smartnav.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final LoginAttemptService loginAttemptService;

    public CustomAuthenticationSuccessHandler(UserRepository userRepository, LoginAttemptService loginAttemptService) {
        this.userRepository = userRepository;
        this.loginAttemptService = loginAttemptService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        String email = authentication.getName();
        loginAttemptService.loginSucceeded(email);

        userRepository.findByEmail(email).ifPresent(user -> {
            user.setLastLogin(LocalDateTime.now());
            userRepository.save(user);
        });

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        boolean isAdminLoginForm = "true".equalsIgnoreCase(request.getParameter("adminLogin")) ||
                (request.getHeader("Referer") != null && request.getHeader("Referer").contains("/admin/login"));

        // If the login request originated from the Administrator Login form, ensure user has ROLE_ADMIN
        if (isAdminLoginForm && !isAdmin) {
            SecurityContextHolder.clearContext();
            request.getSession().invalidate();
            response.sendRedirect("/admin/login?error=admin_required");
            return;
        }

        if (isAdmin) {
            response.sendRedirect("/admin/dashboard");
        } else {
            response.sendRedirect("/dashboard");
        }
    }
}
