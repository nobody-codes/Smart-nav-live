package com.smartnav.websocket;

import com.smartnav.dto.ReportResponse;
import com.smartnav.entity.Notification;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class TrafficNotificationPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public TrafficNotificationPublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void publishNewReport(ReportResponse report) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("action", "NEW_REPORT");
        payload.put("report", report);
        payload.put("message", "New traffic incident reported: " + report.getCategory() + " at " + report.getAddress());
        messagingTemplate.convertAndSend("/topic/reports", payload);
    }

    public void publishReportStatusChange(ReportResponse report, String previousStatus) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("action", "STATUS_CHANGE");
        payload.put("report", report);
        payload.put("previousStatus", previousStatus);
        payload.put("message", "Incident report status changed to " + report.getStatus());
        messagingTemplate.convertAndSend("/topic/reports", payload);
    }

    public void publishReportDeleted(Long reportId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("action", "REPORT_DELETED");
        payload.put("reportId", reportId);
        messagingTemplate.convertAndSend("/topic/reports", payload);
    }

    public void publishNotification(Notification notification) {
        messagingTemplate.convertAndSend("/topic/notifications", notification);
    }
}
