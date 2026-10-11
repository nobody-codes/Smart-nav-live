package com.smartnav.service;

import com.smartnav.entity.Notification;
import com.smartnav.entity.User;
import com.smartnav.repository.NotificationRepository;
import com.smartnav.websocket.TrafficNotificationPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final TrafficNotificationPublisher publisher;

    public NotificationService(NotificationRepository notificationRepository, TrafficNotificationPublisher publisher) {
        this.notificationRepository = notificationRepository;
        this.publisher = publisher;
    }

    @Transactional
    public Notification createNotification(User user, String title, String message, String type) {
        Notification notification = new Notification(user, title, message, type);
        Notification saved = notificationRepository.save(notification);
        publisher.publishNotification(saved);
        return saved;
    }

    public List<Notification> getNotificationsForUser(Long userId) {
        return notificationRepository.findForUser(userId);
    }

    public long getUnreadCount(Long userId) {
        return notificationRepository.countUnreadForUser(userId);
    }

    @Transactional
    public void markAsRead(Long notificationId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            n.setIsRead(true);
            notificationRepository.save(n);
        });
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsReadForUser(userId);
    }
}
