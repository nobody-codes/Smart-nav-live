package com.smartnav.controller;

import com.smartnav.dto.ApiResponse;
import com.smartnav.entity.Notification;
import com.smartnav.entity.User;
import com.smartnav.security.SecurityUtils;
import com.smartnav.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationRestController {

    private final NotificationService notificationService;
    private final SecurityUtils securityUtils;

    public NotificationRestController(NotificationService notificationService, SecurityUtils securityUtils) {
        this.notificationService = notificationService;
        this.securityUtils = securityUtils;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Notification>>> getNotifications() {
        User user = securityUtils.getCurrentUser().orElse(null);
        Long userId = (user != null) ? user.getId() : null;
        List<Notification> list = notificationService.getNotificationsForUser(userId);
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok(ApiResponse.ok("Notification marked as read", null));
    }

    @PutMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead() {
        User user = securityUtils.getCurrentUser().orElse(null);
        if (user != null) {
            notificationService.markAllAsRead(user.getId());
        }
        return ResponseEntity.ok(ApiResponse.ok("All notifications marked as read", null));
    }
}
