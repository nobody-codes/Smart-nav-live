package com.smartnav.security;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginAttemptService {

    public static final int MAX_ATTEMPTS = 5;
    public static final int BLOCK_DURATION_MINUTES = 15;

    private static class Attempt {
        int count;
        LocalDateTime lastAttempt;

        Attempt(int count, LocalDateTime lastAttempt) {
            this.count = count;
            this.lastAttempt = lastAttempt;
        }
    }

    private final ConcurrentHashMap<String, Attempt> attemptsCache = new ConcurrentHashMap<>();

    public void loginSucceeded(String key) {
        if (key != null) {
            attemptsCache.remove(key.toLowerCase().trim());
        }
    }

    public void loginFailed(String key) {
        if (key == null) return;
        String normalizedKey = key.toLowerCase().trim();
        attemptsCache.compute(normalizedKey, (k, existing) -> {
            LocalDateTime now = LocalDateTime.now();
            if (existing == null || existing.lastAttempt.plusMinutes(BLOCK_DURATION_MINUTES).isBefore(now)) {
                return new Attempt(1, now);
            }
            existing.count++;
            existing.lastAttempt = now;
            return existing;
        });
    }

    public boolean isBlocked(String key) {
        if (key == null) return false;
        String normalizedKey = key.toLowerCase().trim();
        Attempt attempt = attemptsCache.get(normalizedKey);
        if (attempt == null) {
            return false;
        }
        if (attempt.lastAttempt.plusMinutes(BLOCK_DURATION_MINUTES).isBefore(LocalDateTime.now())) {
            attemptsCache.remove(normalizedKey);
            return false;
        }
        return attempt.count >= MAX_ATTEMPTS;
    }
}
