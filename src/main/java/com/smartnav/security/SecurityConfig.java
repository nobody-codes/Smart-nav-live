package com.smartnav.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomAuthenticationSuccessHandler successHandler;
    private final CustomAuthenticationFailureHandler failureHandler;
    private final CustomAccessDeniedHandler accessDeniedHandler;

    public SecurityConfig(CustomAuthenticationSuccessHandler successHandler,
                          CustomAuthenticationFailureHandler failureHandler,
                          CustomAccessDeniedHandler accessDeniedHandler) {
        this.successHandler = successHandler;
        this.failureHandler = failureHandler;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf
                // Disable CSRF for REST APIs, WebSockets, H2 Console, and Logout
                .ignoringRequestMatchers("/api/**", "/ws-traffic/**", "/h2-console/**", "/logout")
            )
            .headers(headers -> headers
                .frameOptions(frame -> frame.disable()) // Required for H2 web console & Leaflet iframes
            )
            .authorizeHttpRequests(auth -> auth
                // Static Assets, WebSockets & Error Pages
                .requestMatchers("/css/**", "/js/**", "/images/**", "/uploads/**", "/favicon.ico", "/ws-traffic/**", "/h2-console/**", "/error", "/error/**").permitAll()

                // Public Web Pages
                .requestMatchers("/", "/about", "/contact", "/login", "/register", "/forgot-password", "/reset-password", "/admin/login", "/unauthorized", "/map", "/routes", "/leaderboard").permitAll()

                // Public REST APIs
                .requestMatchers("/api/auth/**", "/api/routes/calculate", "/api/location/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/reports", "/api/reports/**", "/api/incidents", "/api/incidents/**", "/api/leaderboard").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/reports", "/api/reports/**", "/api/incidents", "/api/incidents/**").permitAll()

                // Strictly Protected Administrator Endpoints
                .requestMatchers("/admin/**", "/api/admin/**").hasRole("ADMIN")

                // Authenticated User Pages & Actions
                .requestMatchers("/dashboard", "/report", "/my-reports", "/notifications", "/profile").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/routes/save").authenticated()
                .requestMatchers(HttpMethod.PUT, "/api/reports/**", "/api/incidents/**", "/api/notifications/**").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/api/reports/**", "/api/incidents/**").authenticated()

                // Any other request must be authenticated
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .successHandler(successHandler)
                .failureHandler(failureHandler)
                .permitAll()
            )
            .rememberMe(remember -> remember
                .key("smartnav-secure-remember-me-key-2026")
                .tokenValiditySeconds(86400 * 14) // 14 days
                .rememberMeParameter("rememberMe")
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID", "remember-me")
                .permitAll()
            )
            .exceptionHandling(ex -> ex
                .accessDeniedHandler(accessDeniedHandler)
            );

        return http.build();
    }
}
