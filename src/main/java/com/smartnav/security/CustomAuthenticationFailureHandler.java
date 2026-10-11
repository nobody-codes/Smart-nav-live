package com.smartnav.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class CustomAuthenticationFailureHandler implements AuthenticationFailureHandler {

    private final LoginAttemptService loginAttemptService;

    public CustomAuthenticationFailureHandler(LoginAttemptService loginAttemptService) {
        this.loginAttemptService = loginAttemptService;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        String username = request.getParameter("username");
        if (username != null) {
            loginAttemptService.loginFailed(username);
        }

        boolean isAdminLogin = "true".equalsIgnoreCase(request.getParameter("adminLogin")) ||
                (request.getHeader("Referer") != null && request.getHeader("Referer").contains("/admin/login"));

        String basePath = isAdminLogin ? "/admin/login" : "/login";
        String errorCode = "bad_credentials";

        if (exception instanceof LockedException) {
            if (username != null && loginAttemptService.isBlocked(username)) {
                errorCode = "blocked";
            } else {
                errorCode = "suspended";
            }
        } else if (exception instanceof DisabledException) {
            errorCode = "disabled";
        }

        response.sendRedirect(basePath + "?error=" + URLEncoder.encode(errorCode, StandardCharsets.UTF_8));
    }
}
